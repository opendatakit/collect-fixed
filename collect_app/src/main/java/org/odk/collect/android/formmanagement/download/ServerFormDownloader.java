package org.odk.collect.android.formmanagement.download;

import static org.odk.collect.android.utilities.FileUtils.interuptablyWriteFile;

import org.jetbrains.annotations.NotNull;
import org.odk.collect.android.formmanagement.MediaFilesDownloadResult;
import org.odk.collect.android.formmanagement.metadata.FormMetadata;
import org.odk.collect.android.formmanagement.metadata.FormMetadataParser;
import org.odk.collect.android.formmanagement.ServerFormDetails;
import org.odk.collect.android.formmanagement.ServerFormUseCases;
import org.odk.collect.android.utilities.FileUtils;
import org.odk.collect.android.utilities.FormNameUtils;
import org.odk.collect.androidshared.utils.Validator;
import org.odk.collect.async.OngoingWorkListener;
import org.odk.collect.entities.server.EntitySource;
import org.odk.collect.entities.storage.EntitiesRepository;
import org.odk.collect.forms.Form;
import org.odk.collect.forms.FormSource;
import org.odk.collect.forms.FormSourceException;
import org.odk.collect.forms.FormsRepository;
import org.odk.collect.shared.files.FileExt;
import org.odk.collect.shared.strings.Md5;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import timber.log.Timber;

public class ServerFormDownloader implements FormDownloader {

    private final FormsRepository formsRepository;
    private final FormSource formSource;
    private final File cacheDir;
    private final String formsDirPath;
    private final FormMetadataParser formMetadataParser;
    private final Supplier<Long> clock;
    private final EntitiesRepository entitiesRepository;

    public ServerFormDownloader(FormSource formSource, FormsRepository formsRepository, File cacheDir, String formsDirPath, FormMetadataParser formMetadataParser, Supplier<Long> clock, EntitiesRepository entitiesRepository) {
        this.formSource = formSource;
        this.cacheDir = cacheDir;
        this.formsDirPath = formsDirPath;
        this.formsRepository = formsRepository;
        this.formMetadataParser = formMetadataParser;
        this.clock = clock;
        this.entitiesRepository = entitiesRepository;
    }

    @Override
    public void downloadForm(ServerFormDetails form, @Nullable ProgressReporter progressReporter, @Nullable Supplier<Boolean> isCancelled) throws FormDownloadException {
        Form formOnDevice;
        List<Form> preExistingFormsWithSameIdAndVersion = new ArrayList<>();
        try {
            formOnDevice = formsRepository.getOneByMd5Hash(validateHash(form.getHash()));
        } catch (IllegalArgumentException e) {
            throw new FormDownloadException.FormWithNoHash();
        }

        if (formOnDevice != null) {
            if (formOnDevice.isDeleted()) {
                formsRepository.restore(formOnDevice.getDbId());
            }
        } else {
            preExistingFormsWithSameIdAndVersion = formsRepository.getAllByFormIdAndVersion(form.getFormId(), form.getFormVersion());
        }

        File tempDir = new File(cacheDir, "download-" + UUID.randomUUID().toString());
        tempDir.mkdirs();

        try {
            OngoingWorkListener stateListener = new ProgressReporterAndSupplierStateListener(progressReporter, isCancelled);
            processOneForm(form, stateListener, tempDir, formsDirPath, formMetadataParser);
        } catch (FormSourceException e) {
            throw new FormDownloadException.FormSourceError(e);
        } finally {
            FileExt.deleteDirectory(tempDir);
            for (Form formToDelete : preExistingFormsWithSameIdAndVersion) {
                formsRepository.delete(formToDelete.getDbId());
            }
        }
    }

    private void processOneForm(ServerFormDetails fd, OngoingWorkListener stateListener, File tempDir, String formsDirPath, FormMetadataParser formMetadataParser) throws FormDownloadException, FormSourceException {
        // use a temporary media path until everything is ok.
        String tempMediaPath = new File(tempDir, "media").getAbsolutePath();
        FileResult fileResult = null;
        MediaFilesDownloadResult mediaFilesDownloadResult;

        try {
            // get the xml file
            // if we've downloaded a duplicate, this gives us the file
            fileResult = downloadXform(fd.getFormName(), fd.getDownloadUrl(), stateListener, tempDir, formsDirPath);

            // download media files if there are any
            if (fd.getManifest() != null && !fd.getManifest().getMediaFiles().isEmpty()) {
                mediaFilesDownloadResult = ServerFormUseCases.downloadMediaFiles(fd, formSource, formsRepository, tempMediaPath, tempDir, entitiesRepository, (EntitySource) formSource, stateListener);
            } else {
                mediaFilesDownloadResult = new MediaFilesDownloadResult(false, false);
            }

            ServerFormUseCases.copySavedFileFromPreviousFormVersionIfExists(formsRepository, fd.getFormId(), tempMediaPath);
        } catch (FormDownloadException.DownloadingInterrupted | InterruptedException e) {
            Timber.i(e);
            cleanUp(fileResult, tempMediaPath);
            throw new FormDownloadException.DownloadingInterrupted();
        } catch (IOException e) {
            throw new FormDownloadException.DiskError();
        }

        if (stateListener != null && stateListener.isCancelled()) {
            cleanUp(fileResult, tempMediaPath);
            throw new FormDownloadException.DownloadingInterrupted();
        }

        FormMetadata formMetadata = null;
        if (fileResult.isNew) {
            try {
                final long start = System.currentTimeMillis();
                Timber.i("Parsing document %s", fileResult.file.getAbsolutePath());

                formMetadata = formMetadataParser.readMetadata(fileResult.file);

                Timber.i("Parse finished in %.3f seconds.", (System.currentTimeMillis() - start) / 1000F);
            } catch (RuntimeException e) {
                throw new FormDownloadException.FormParsingError();
            }
        }

        if (stateListener != null && stateListener.isCancelled()) {
            throw new FormDownloadException.DownloadingInterrupted();
        }

        if (fileResult.isNew && !isSubmissionOk(formMetadata)) {
            throw new FormDownloadException.InvalidSubmission();
        }

        try {
            installEverything(tempMediaPath, fileResult, formMetadata, formsDirPath, mediaFilesDownloadResult);
        } catch (FormDownloadException.DiskError e) {
            cleanUp(fileResult, tempMediaPath);
            throw e;
        }
    }

    private boolean isSubmissionOk(FormMetadata formMetadata) {
        String submission = formMetadata.getSubmissionUri();
        return submission == null || Validator.isUrlValid(submission);
    }

    private void installEverything(String tempMediaPath, FileResult fileResult, FormMetadata formMetadata, String formsDirPath, MediaFilesDownloadResult mediaFilesDownloadResult) throws FormDownloadException.DiskError {
        FormResult formResult;

        File formFile;

        if (fileResult.isNew()) {
            // Copy form to forms dir
            formFile = new File(formsDirPath, fileResult.file.getName());
            FileUtils.copyFile(fileResult.file, formFile);
        } else {
            formFile = fileResult.file;

            if (mediaFilesDownloadResult.getNewAttachmentsDownloaded()) {
                Form existingForm = formsRepository.getOneByPath(formFile.getAbsolutePath());
                if (existingForm != null) {
                    formsRepository.save(new Form.Builder(existingForm)
                            .lastDetectedAttachmentsUpdateDate(clock.get())
                            .build()
                    );
                }
            }

            if (mediaFilesDownloadResult.getEntitiesDownloaded()) {
                Form existingForm = formsRepository.getOneByPath(formFile.getAbsolutePath());
                if (existingForm != null) {
                    formsRepository.save(new Form.Builder(existingForm)
                            .usesEntities(true)
                            .build()
                    );
                }
            }
        }

        // Save form in database
        formResult = findOrCreateForm(formFile, formMetadata, mediaFilesDownloadResult);

        // move the media files in the media folder
        if (tempMediaPath != null) {
            File formMediaDir = new File(formResult.form.getFormMediaPath());

            try {
                moveMediaFiles(tempMediaPath, formMediaDir);
            } catch (IOException e) {
                Timber.e(e);

                if (formResult.isNew() && fileResult.isNew()) {
                    // this means we should delete the entire form together with the metadata
                    formsRepository.delete(formResult.form.getDbId());
                }

                throw new FormDownloadException.DiskError();
            }
        }
    }

    private void cleanUp(FileResult fileResult, String tempMediaPath) {
        if (fileResult == null) {
            Timber.d("The user cancelled (or an exception happened) the download of a form at the very beginning.");
        } else {
            String md5Hash = Md5.getMd5Hash(fileResult.file);
            if (md5Hash != null) {
                formsRepository.deleteByMd5Hash(md5Hash);
            }
            FileUtils.deleteAndReport(fileResult.getFile());
        }

        if (tempMediaPath != null) {
            FileUtils.purgeMediaPath(tempMediaPath);
        }
    }

    private FormResult findOrCreateForm(File formFile, FormMetadata formMetadata, MediaFilesDownloadResult mediaFilesDownloadResult) {
        final String formFilePath = formFile.getAbsolutePath();
        String mediaPath = FileUtils.constructMediaPath(formFilePath);

        Form existingForm = formsRepository.getOneByPath(formFile.getAbsolutePath());

        if (existingForm == null) {
            Form newForm = saveNewForm(formMetadata, formFile, mediaPath, mediaFilesDownloadResult.getEntitiesDownloaded());
            return new FormResult(newForm, true);
        } else {
            return new FormResult(existingForm, false);
        }
    }

    private Form saveNewForm(FormMetadata formMetadata, File formFile, String mediaPath, boolean entityAttachmentsDetected) {
        Form form = new Form.Builder()
                .formFilePath(formFile.getAbsolutePath())
                .formMediaPath(mediaPath)
                .displayName(formMetadata.getTitle())
                .version(formMetadata.getVersion())
                .formId(formMetadata.getId())
                .submissionUri(formMetadata.getSubmissionUri())
                .base64RSAPublicKey(formMetadata.getBase64RsaPublicKey())
                .autoDelete(formMetadata.getAutoDelete())
                .autoSend(formMetadata.getAutoSend())
                .geometryXpath(formMetadata.getGeometryXPath())
                .usesEntities(formMetadata.isEntityForm() || entityAttachmentsDetected)
                .build();

        return formsRepository.save(form);
    }

    /**
     * Takes the formName and the URL and attempts to download the specified file. Returns a file
     * object representing the downloaded file.
     */
    private FileResult downloadXform(String formName, String url, OngoingWorkListener stateListener, File tempDir, String formsDirPath) throws FormSourceException, IOException, FormDownloadException.DownloadingInterrupted, InterruptedException {
        InputStream xform = formSource.fetchForm(url);

        String fileName = getFormFileName(formName, formsDirPath);
        File tempFormFile = new File(tempDir + File.separator + fileName);
        interuptablyWriteFile(xform, tempFormFile, tempDir, stateListener);

        // we've downloaded the file, and we may have renamed it
        // make sure it's not the same as a file we already have
        Form form = formsRepository.getOneByMd5Hash(Md5.getMd5Hash(tempFormFile));
        if (form != null) {
            // delete the file we just downloaded, because it's a duplicate
            FileUtils.deleteAndReport(tempFormFile);

            // set the file returned to the file we already had
            return new FileResult(new File(form.getFormFilePath()), false);
        } else {
            return new FileResult(tempFormFile, true);
        }
    }

    @NotNull
    private static String getFormFileName(String formName, String formsDirPath) {
        String formattedFormName = FormNameUtils.formatFilenameFromFormName(formName);
        String fileName = formattedFormName + ".xml";
        int i = 2;
        while (new File(formsDirPath + File.separator + fileName).exists()) {
            fileName = formattedFormName + "_" + i + ".xml";
            i++;
        }
        return fileName;
    }

    private static String validateHash(String hash) {
        return hash == null || hash.isEmpty() ? null : hash;
    }

    private static void moveMediaFiles(String tempMediaPath, File formMediaPath) throws IOException {
        File tempMediaFolder = new File(tempMediaPath);
        File[] mediaFiles = tempMediaFolder.listFiles();

        if (mediaFiles != null && mediaFiles.length != 0) {
            for (File mediaFile : mediaFiles) {
                try {
                    org.apache.commons.io.FileUtils.copyFileToDirectory(mediaFile, formMediaPath);
                } catch (IllegalArgumentException e) {
                    // This can happen if copyFileToDirectory is pointed at a file instead of a dir
                    throw new IOException(e);
                }

            }
        }
    }

    private static class FormResult {

        private final Form form;
        private final boolean isNew;

        private FormResult(Form form, boolean isNew) {
            this.form = form;
            this.isNew = isNew;
        }

        private boolean isNew() {
            return isNew;
        }

        public Form getForm() {
            return form;
        }
    }

    private static class FileResult {

        private final File file;
        private final boolean isNew;

        FileResult(File file, boolean isNew) {
            this.file = file;
            this.isNew = isNew;
        }

        private File getFile() {
            return file;
        }

        private boolean isNew() {
            return isNew;
        }
    }

    private static class ProgressReporterAndSupplierStateListener implements OngoingWorkListener {
        private final ProgressReporter progressReporter;
        private final Supplier<Boolean> isCancelled;

        ProgressReporterAndSupplierStateListener(ProgressReporter progressReporter, Supplier<Boolean> isCancelled) {
            this.progressReporter = progressReporter;
            this.isCancelled = isCancelled;
        }

        @Override
        public void progressUpdate(int progress) {
            if (progressReporter != null) {
                progressReporter.onDownloadingMediaFile(progress);
            }
        }

        @Override
        public boolean isCancelled() {
            if (isCancelled != null) {
                return isCancelled.get();
            } else {
                return false;
            }
        }
    }
}
