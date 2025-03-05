package org.odk.collect.android.formmanagement.download;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.odk.collect.android.utilities.FileUtils.read;
import static org.odk.collect.androidshared.utils.PathUtils.getAbsoluteFilePath;
import static org.odk.collect.formstest.FormUtils.buildForm;
import static org.odk.collect.formstest.FormUtils.createXFormBody;
import static java.util.Arrays.asList;

import com.google.common.io.Files;

import org.junit.Test;
import org.odk.collect.android.formmanagement.metadata.FormMetadataParser;
import org.odk.collect.android.formmanagement.ServerFormDetails;
import org.odk.collect.entities.server.EntitySource;
import org.odk.collect.entities.storage.EntitiesRepository;
import org.odk.collect.entities.storage.InMemEntitiesRepository;
import org.odk.collect.forms.Form;
import org.odk.collect.forms.FormListItem;
import org.odk.collect.forms.FormSource;
import org.odk.collect.forms.FormSourceException;
import org.odk.collect.forms.FormsRepository;
import org.odk.collect.forms.ManifestFile;
import org.odk.collect.forms.MediaFile;
import org.odk.collect.formstest.FormUtils;
import org.odk.collect.formstest.InMemFormsRepository;
import org.odk.collect.shared.strings.Md5;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

@SuppressWarnings("PMD.DoubleBraceInitialization")
public class ServerFormDownloaderTest {

    private final FormsRepository formsRepository = new InMemFormsRepository();
    private final File cacheDir = Files.createTempDir();
    private final File formsDir = Files.createTempDir();
    private final Supplier<Long> clock = () -> 123L;

    private final EntitiesRepository entitiesRepository = new InMemEntitiesRepository();
    private final EntitySource entitySource = mock();

    @Test
    public void downloadsAndSavesForm() throws Exception {
        String xform = createXFormBody("id", "version");
        ServerFormDetails serverFormDetails = new ServerFormDetails(
                "Form",
                "http://downloadUrl",
                "id",
                "version",
                Md5.getMd5Hash(new ByteArrayInputStream(xform.getBytes())),
                true,
                false,
                null);

        FormSource formSource = mock(FormSource.class);
        when(formSource.fetchForm("http://downloadUrl")).thenReturn(new ByteArrayInputStream(xform.getBytes()));

        ServerFormDownloader downloader = new ServerFormDownloader(formSource, formsRepository, cacheDir, formsDir.getAbsolutePath(), FormMetadataParser.INSTANCE, clock, entitiesRepository, entitySource);
        downloader.downloadForm(serverFormDetails, null, null);

        List<Form> allForms = formsRepository.getAll();
        assertThat(allForms.size(), is(1));
        Form form = allForms.get(0);
        assertThat(form.getFormId(), is("id"));

        File formFile = new File(getAbsoluteFilePath(formsDir.getAbsolutePath(), form.getFormFilePath()));
        assertThat(formFile.exists(), is(true));
        assertThat(new String(read(formFile)), is(xform));
    }

    @Test
    public void whenFormToDownloadIsUpdate_savesNewVersionAlongsideOldVersion() throws Exception {
        String xform = createXFormBody("id", "version");
        ServerFormDetails serverFormDetails = new ServerFormDetails(
                "Form",
                "http://downloadUrl",
                "id",
                "version",
                Md5.getMd5Hash(new ByteArrayInputStream(xform.getBytes())),
                true,
                false,
                null);

        FormSource formSource = mock(FormSource.class);
        when(formSource.fetchForm("http://downloadUrl")).thenReturn(new ByteArrayInputStream(xform.getBytes()));

        ServerFormDownloader downloader = new ServerFormDownloader(formSource, formsRepository, cacheDir, formsDir.getAbsolutePath(), FormMetadataParser.INSTANCE, clock, entitiesRepository, entitySource);
        downloader.downloadForm(serverFormDetails, null, null);

        String xformUpdate = createXFormBody("id", "updated");
        ServerFormDetails serverFormDetailsUpdated = new ServerFormDetails(
                "Form",
                "http://downloadUpdatedUrl",
                "id",
                "updated",
                Md5.getMd5Hash(new ByteArrayInputStream(xformUpdate.getBytes())),
                true,
                false,
                null);

        when(formSource.fetchForm("http://downloadUpdatedUrl")).thenReturn(new ByteArrayInputStream(xformUpdate.getBytes()));
        downloader.downloadForm(serverFormDetailsUpdated, null, null);

        List<Form> allForms = formsRepository.getAll();
        assertThat(allForms.size(), is(2));
        allForms.forEach(f -> {
            File formFile = new File(getAbsoluteFilePath(formsDir.getAbsolutePath(), f.getFormFilePath()));
            assertThat(formFile.exists(), is(true));
        });
    }

    @Test
    public void whenFormToDownloadIsUpdate_withSameFormIdAndVersion_replacePreExistingForm() throws Exception {
        String xform = createXFormBody("id", "version");
        ServerFormDetails serverFormDetails = new ServerFormDetails(
                "Form",
                "http://downloadUrl",
                "id",
                "version",
                Md5.getMd5Hash(new ByteArrayInputStream(xform.getBytes())),
                true,
                false,
                new ManifestFile("", Collections.singletonList(
                        new MediaFile("file1", "hash-1", "http://file1")
                )));

        FormSource formSource = mock(FormSource.class);
        when(formSource.fetchMediaFile("http://file1")).thenReturn(new ByteArrayInputStream("contents1".getBytes()));
        when(formSource.fetchForm("http://downloadUrl")).thenReturn(new ByteArrayInputStream(xform.getBytes()));

        ServerFormDownloader downloader = new ServerFormDownloader(formSource, formsRepository, cacheDir, formsDir.getAbsolutePath(), FormMetadataParser.INSTANCE, clock, entitiesRepository, entitySource);
        downloader.downloadForm(serverFormDetails, null, null);

        List<Form> formsBeforeUpdate = formsRepository.getAllByFormIdAndVersion("id", "version");

        String xformUpdate = FormUtils.createXFormBody("id", "version", "A different title");
        ServerFormDetails serverFormDetailsUpdated = new ServerFormDetails(
                "Form",
                "http://downloadUrl",
                "id",
                "version",
                Md5.getMd5Hash(new ByteArrayInputStream(xformUpdate.getBytes())),
                true,
                false,
                new ManifestFile("", Collections.singletonList(
                        new MediaFile("file1", "hash-1", "http://file1")
                )));

        when(formSource.fetchForm("http://downloadUrl")).thenReturn(new ByteArrayInputStream(xformUpdate.getBytes()));
        downloader.downloadForm(serverFormDetailsUpdated, null, null);

        List<Form> formsAfterUpdate = formsRepository.getAllByFormIdAndVersion("id", "version");
        assertThat(formsAfterUpdate.size(), is(1));

        // Pre-existing forms should be deleted along with its files
        File formFile = new File(getAbsoluteFilePath(formsDir.getAbsolutePath(), formsBeforeUpdate.get(0).getFormFilePath()));
        assertThat(formFile.exists(), is(false));
        File mediaFile = new File(getAbsoluteFilePath(formsDir.getAbsolutePath(), formsBeforeUpdate.get(0).getFormMediaPath()) + File.separator + "file1");
        assertThat(mediaFile.exists(), is(false));

        // New forms should be added
        formFile = new File(getAbsoluteFilePath(formsDir.getAbsolutePath(), formsAfterUpdate.get(0).getFormFilePath()));
        assertThat(formFile.exists(), is(true));
        mediaFile = new File(getAbsoluteFilePath(formsDir.getAbsolutePath(), formsAfterUpdate.get(0).getFormMediaPath()) + File.separator + "file1");
        assertThat(mediaFile.exists(), is(true));
    }

    @Test
    public void whenFormListMissingHash_throwsError() throws Exception {
        String xform = createXFormBody("id", "version");
        ServerFormDetails serverFormDetails = new ServerFormDetails(
                "Form",
                "http://downloadUrl",
                "id",
                "version",
                null,
                true,
                false,
                null);

        FormSource formSource = mock(FormSource.class);
        when(formSource.fetchForm("http://downloadUrl")).thenReturn(new ByteArrayInputStream(xform.getBytes()));

        ServerFormDownloader downloader = new ServerFormDownloader(formSource, formsRepository, cacheDir, formsDir.getAbsolutePath(), FormMetadataParser.INSTANCE, clock, entitiesRepository, entitySource);
        try {
            downloader.downloadForm(serverFormDetails, null, null);
            fail("Expected exception because of missing form hash");
        } catch (FormDownloadException.FormWithNoHash e) {
            // pass
        }
    }

    @Test
    public void whenFormHasMediaFiles_downloadsAndSavesFormAndMediaFiles() throws Exception {
        String xform = createXFormBody("id", "version");
        ServerFormDetails serverFormDetails = new ServerFormDetails(
                "Form",
                "http://downloadUrl",
                "id",
                "version",
                Md5.getMd5Hash(new ByteArrayInputStream(xform.getBytes())),
                true,
                false,
                new ManifestFile("", asList(
                        new MediaFile("file1", "hash-1", "http://file1"),
                        new MediaFile("file2", "hash-2", "http://file2")
                )));

        FormSource formSource = mock(FormSource.class);
        when(formSource.fetchForm("http://downloadUrl")).thenReturn(new ByteArrayInputStream(xform.getBytes()));
        when(formSource.fetchMediaFile("http://file1")).thenReturn(new ByteArrayInputStream("contents1".getBytes()));
        when(formSource.fetchMediaFile("http://file2")).thenReturn(new ByteArrayInputStream("contents2".getBytes()));

        ServerFormDownloader downloader = new ServerFormDownloader(formSource, formsRepository, cacheDir, formsDir.getAbsolutePath(), FormMetadataParser.INSTANCE, clock, entitiesRepository, entitySource);
        downloader.downloadForm(serverFormDetails, null, null);

        List<Form> allForms = formsRepository.getAll();
        assertThat(allForms.size(), is(1));
        Form form = allForms.get(0);
        assertThat(form.getFormId(), is("id"));

        File formFile = new File(getAbsoluteFilePath(formsDir.getAbsolutePath(), form.getFormFilePath()));
        assertThat(formFile.exists(), is(true));
        assertThat(new String(read(formFile)), is(xform));

        File mediaFile1 = new File(form.getFormMediaPath() + "/file1");
        assertThat(mediaFile1.exists(), is(true));
        assertThat(new String(read(mediaFile1)), is("contents1"));

        File mediaFile2 = new File(form.getFormMediaPath() + "/file2");
        assertThat(mediaFile2.exists(), is(true));
        assertThat(new String(read(mediaFile2)), is("contents2"));
    }

    @Test
    public void whenFormHasMediaFiles_andIsFormToDownloadIsUpdate_doesNotRedownloadMediaFiles() throws Exception {
        String xform = createXFormBody("id", "version");
        ServerFormDetails serverFormDetails = new ServerFormDetails(
                "Form",
                "http://downloadUrl",
                "id",
                "version",
                Md5.getMd5Hash(new ByteArrayInputStream(xform.getBytes())),
                true,
                false,
                new ManifestFile("", asList(
                        new MediaFile("file1", Md5.getMd5Hash("contents1"), "http://file1"),
                        new MediaFile("file2", Md5.getMd5Hash("contents2"), "http://file2")
                )));

        FormSource formSource = mock(FormSource.class);
        when(formSource.fetchForm("http://downloadUrl")).thenReturn(new ByteArrayInputStream(xform.getBytes()));
        when(formSource.fetchMediaFile("http://file1")).thenReturn(new ByteArrayInputStream("contents1".getBytes()));
        when(formSource.fetchMediaFile("http://file2")).thenReturn(new ByteArrayInputStream("contents2".getBytes()));

        ServerFormDownloader downloader = new ServerFormDownloader(formSource, formsRepository, cacheDir, formsDir.getAbsolutePath(), FormMetadataParser.INSTANCE, clock, entitiesRepository, entitySource);
        downloader.downloadForm(serverFormDetails, null, null);

        String xformUpdate = createXFormBody("id", "updated");
        ServerFormDetails serverFormDetailsUpdated = new ServerFormDetails(
                "Form",
                "http://downloadUpdatedUrl",
                "id",
                "updated",
                Md5.getMd5Hash(new ByteArrayInputStream(xformUpdate.getBytes())),
                false,
                true,
                new ManifestFile("", asList(
                        new MediaFile("file1", Md5.getMd5Hash("contents1"), "http://file1"),
                        new MediaFile("file2", Md5.getMd5Hash("contents2"), "http://file2")
                )));

        when(formSource.fetchForm("http://downloadUpdatedUrl")).thenReturn(new ByteArrayInputStream(xformUpdate.getBytes()));
        downloader.downloadForm(serverFormDetailsUpdated, null, null);

        verify(formSource, times(1)).fetchMediaFile("http://file1");
        verify(formSource, times(1)).fetchMediaFile("http://file2");
    }

    @Test
    public void whenFormHasMediaFiles_andIsFormToDownloadIsUpdate_downloadsFilesWithChangedHash() throws Exception {
        String xform = createXFormBody("id", "version");
        ServerFormDetails serverFormDetails = new ServerFormDetails(
                "Form",
                "http://downloadUrl",
                "id",
                "version",
                Md5.getMd5Hash(new ByteArrayInputStream(xform.getBytes())),
                true,
                false,
                new ManifestFile("", asList(
                        new MediaFile("file1", Md5.getMd5Hash("contents1"), "http://file1"),
                        new MediaFile("file2", Md5.getMd5Hash("contents2"), "http://file2")
                )));

        FormSource formSource = mock(FormSource.class);
        when(formSource.fetchForm("http://downloadUrl")).thenReturn(new ByteArrayInputStream(xform.getBytes()));
        when(formSource.fetchMediaFile("http://file1")).thenReturn(new ByteArrayInputStream("contents1".getBytes()));
        when(formSource.fetchMediaFile("http://file2")).thenReturn(new ByteArrayInputStream("contents2".getBytes()));

        ServerFormDownloader downloader = new ServerFormDownloader(formSource, formsRepository, cacheDir, formsDir.getAbsolutePath(), FormMetadataParser.INSTANCE, clock, entitiesRepository, entitySource);
        downloader.downloadForm(serverFormDetails, null, null);

        String xformUpdate = createXFormBody("id", "updated");
        ServerFormDetails serverFormDetailsUpdated = new ServerFormDetails(
                "Form",
                "http://downloadUpdatedUrl",
                "id",
                "updated",
                Md5.getMd5Hash(new ByteArrayInputStream(xformUpdate.getBytes())),
                false,
                true,
                new ManifestFile("", asList(
                        new MediaFile("file1", Md5.getMd5Hash("contents1"), "http://file1"),
                        new MediaFile("file2", Md5.getMd5Hash("contents3"), "http://file2")
                )));

        when(formSource.fetchMediaFile("http://file2")).thenReturn(new ByteArrayInputStream("contents3".getBytes()));
        when(formSource.fetchForm("http://downloadUpdatedUrl")).thenReturn(new ByteArrayInputStream(xformUpdate.getBytes()));
        downloader.downloadForm(serverFormDetailsUpdated, null, null);

        Form form = formsRepository.getAllByFormIdAndVersion("id", "updated").get(0);
        File mediaFile2 = new File(form.getFormMediaPath() + "/file2");
        assertThat(mediaFile2.exists(), is(true));
        assertThat(new String(read(mediaFile2)), is("contents3"));
    }

    @Test
    public void whenFormHasMediaFiles_andFetchingMediaFileFails_throwsFetchErrorAndDoesNotSaveAnything() throws Exception {
        String xform = createXFormBody("id", "version");
        ServerFormDetails serverFormDetails = new ServerFormDetails(
                "Form",
                "http://downloadUrl",
                "id",
                "version",
                Md5.getMd5Hash(new ByteArrayInputStream(xform.getBytes())),
                true,
                false,
                new ManifestFile("", List.of(
                        new MediaFile("file1", "hash-1", "http://file1")
                )));

        FormSource formSource = mock(FormSource.class);
        when(formSource.fetchForm("http://downloadUrl")).thenReturn(new ByteArrayInputStream(xform.getBytes()));
        when(formSource.fetchMediaFile("http://file1")).thenThrow(new FormSourceException.FetchError());

        ServerFormDownloader downloader = new ServerFormDownloader(formSource, formsRepository, cacheDir, formsDir.getAbsolutePath(), FormMetadataParser.INSTANCE, clock, entitiesRepository, entitySource);

        try {
            downloader.downloadForm(serverFormDetails, null, null);
            fail("Expected exception");
        } catch (FormDownloadException.FormSourceError e) {
            assertThat(formsRepository.getAll(), is(empty()));
            assertThat(asList(new File(getCacheFilesPath()).listFiles()), is(empty()));
            assertThat(asList(new File(getFormFilesPath()).listFiles()), is(empty()));
        }
    }

    @Test
    public void whenFormHasMediaFiles_andFileExistsInMediaDirPath_throwsDiskExceptionAndDoesNotSaveAnything() throws Exception {
        String xform = createXFormBody("id", "version");
        ServerFormDetails serverFormDetails = new ServerFormDetails(
                "Form",
                "http://downloadUrl",
                "id",
                "version",
                Md5.getMd5Hash(new ByteArrayInputStream(xform.getBytes())),
                true,
                false,
                new ManifestFile("", List.of(
                        new MediaFile("file1", "hash-1", "http://file1")
                )));

        FormSource formSource = mock(FormSource.class);
        when(formSource.fetchForm("http://downloadUrl")).thenReturn(new ByteArrayInputStream(xform.getBytes()));
        when(formSource.fetchMediaFile("http://file1")).thenReturn(new ByteArrayInputStream("contents1".getBytes()));

        // Create file where media dir would go
        assertThat(new File(formsDir, "Form-media").createNewFile(), is(true));

        ServerFormDownloader downloader = new ServerFormDownloader(formSource, formsRepository, cacheDir, formsDir.getAbsolutePath(), FormMetadataParser.INSTANCE, clock, entitiesRepository, entitySource);

        try {
            downloader.downloadForm(serverFormDetails, null, null);
            fail("Expected exception");
        } catch (FormDownloadException.DiskError e) {
            assertThat(formsRepository.getAll(), is(empty()));
            assertThat(asList(new File(getCacheFilesPath()).listFiles()), is(empty()));
            assertThat(asList(new File(getFormFilesPath()).listFiles()), is(empty()));
        }
    }

    @Test
    public void beforeDownloadingEachMediaFile_reportsProgress() throws Exception {
        String xform = createXFormBody("id", "version");
        ServerFormDetails serverFormDetails = new ServerFormDetails(
                "Form",
                "http://downloadUrl",
                "id",
                "version",
                Md5.getMd5Hash(new ByteArrayInputStream(xform.getBytes())),
                true,
                false,
                new ManifestFile("", asList(
                        new MediaFile("file1", "hash-1", "http://file1"),
                        new MediaFile("file2", "hash-2", "http://file2")
                )));

        FormSource formSource = mock(FormSource.class);
        when(formSource.fetchForm("http://downloadUrl")).thenReturn(new ByteArrayInputStream(xform.getBytes()));
        when(formSource.fetchMediaFile("http://file1")).thenReturn(new ByteArrayInputStream("contents".getBytes()));
        when(formSource.fetchMediaFile("http://file2")).thenReturn(new ByteArrayInputStream("contents".getBytes()));

        ServerFormDownloader downloader = new ServerFormDownloader(formSource, formsRepository, cacheDir, formsDir.getAbsolutePath(), FormMetadataParser.INSTANCE, clock, entitiesRepository, entitySource);
        RecordingProgressReporter progressReporter = new RecordingProgressReporter();
        downloader.downloadForm(serverFormDetails, progressReporter, null);

        assertThat(progressReporter.reports, contains(1, 2));
    }

    //region Undelete on re-download
    @Test
    public void whenFormIsSoftDeleted_unDeletesForm() throws Exception {
        String xform = createXFormBody("deleted-form", "version");
        Form form = buildForm("deleted-form", "version", getFormFilesPath(), xform)
                .deleted(true)
                .build();
        formsRepository.save(form);

        ServerFormDetails serverFormDetails = new ServerFormDetails(
                form.getDisplayName(),
                "http://downloadUrl",
                form.getFormId(),
                form.getVersion(),
                Md5.getMd5Hash(new ByteArrayInputStream(xform.getBytes())),
                true,
                false,
                null);

        FormSource formSource = mock(FormSource.class);
        when(formSource.fetchForm("http://downloadUrl")).thenReturn(new ByteArrayInputStream(xform.getBytes()));

        ServerFormDownloader downloader = new ServerFormDownloader(formSource, formsRepository, cacheDir, formsDir.getAbsolutePath(), FormMetadataParser.INSTANCE, clock, entitiesRepository, entitySource);
        downloader.downloadForm(serverFormDetails, null, null);
        assertThat(formsRepository.get(1L).isDeleted(), is(false));
    }

    @Test
    public void whenMultipleFormsWithSameFormIdVersionDeleted_reDownloadUnDeletesFormWithSameHash() throws Exception {
        String xform = FormUtils.createXFormBody("deleted-form", "version", "A title");
        Form form = buildForm("deleted-form", "version", getFormFilesPath(), xform)
                .deleted(true)
                .build();
        formsRepository.save(form);

        String xform2 = FormUtils.createXFormBody("deleted-form", "version", "A different title");
        Form form2 = buildForm("deleted-form", "version", getFormFilesPath(), xform2)
                .deleted(true)
                .build();
        formsRepository.save(form2);

        ServerFormDetails serverFormDetails = new ServerFormDetails(
                form2.getDisplayName(),
                "http://downloadUrl",
                form2.getFormId(),
                form2.getVersion(),
                Md5.getMd5Hash(new ByteArrayInputStream(xform2.getBytes())),
                true,
                false,
                null);

        FormSource formSource = mock(FormSource.class);
        when(formSource.fetchForm("http://downloadUrl")).thenReturn(new ByteArrayInputStream(xform2.getBytes()));

        ServerFormDownloader downloader = new ServerFormDownloader(formSource, formsRepository, cacheDir, formsDir.getAbsolutePath(), FormMetadataParser.INSTANCE, clock, entitiesRepository, entitySource);
        downloader.downloadForm(serverFormDetails, null, null);
        assertThat(formsRepository.get(1L).isDeleted(), is(true));
        assertThat(formsRepository.get(2L).isDeleted(), is(false));
    }
    //endregion

    @Test
    public void whenFormAlreadyDownloaded_formRemainsOnDevice() throws Exception {
        String xform = createXFormBody("id", "version");
        ServerFormDetails serverFormDetails = new ServerFormDetails(
                "Form",
                "http://downloadUrl",
                "id",
                "version",
                Md5.getMd5Hash(new ByteArrayInputStream(xform.getBytes())),
                true,
                false,
                null);

        FormSource formSource = mock(FormSource.class);
        when(formSource.fetchForm("http://downloadUrl")).thenReturn(new ByteArrayInputStream(xform.getBytes()));

        ServerFormDownloader downloader = new ServerFormDownloader(formSource, formsRepository, cacheDir, formsDir.getAbsolutePath(), FormMetadataParser.INSTANCE, clock, entitiesRepository, entitySource);

        // Initial download
        downloader.downloadForm(serverFormDetails, null, null);

        ServerFormDetails serverFormDetailsAlreadyOnDevice = new ServerFormDetails(
                "Form",
                "http://downloadUrl",
                "id",
                "version",
                Md5.getMd5Hash(new ByteArrayInputStream(xform.getBytes())),
                false,
                false,
                null);

        when(formSource.fetchForm("http://downloadUrl")).thenReturn(new ByteArrayInputStream(xform.getBytes()));
        downloader.downloadForm(serverFormDetailsAlreadyOnDevice, null, null);

        List<Form> allForms = formsRepository.getAll();
        assertThat(allForms.size(), is(1));

        Form form = allForms.get(0);
        File formFile = new File(getAbsoluteFilePath(formsDir.getAbsolutePath(), form.getFormFilePath()));
        assertThat(new String(read(formFile)), is(xform));
    }

    @Test
    public void whenFormAlreadyDownloaded_andFormHasNewMediaFiles_updatesMediaFilesAndLastDetectedAttachmentsUpdateDateInForm() throws Exception {
        String xform = createXFormBody("id", "version");
        ServerFormDetails serverFormDetails = new ServerFormDetails(
                "Form",
                "http://downloadUrl",
                "id",
                "version",
                Md5.getMd5Hash(new ByteArrayInputStream(xform.getBytes())),
                true,
                false,
                new ManifestFile("", List.of(
                        new MediaFile("file1", Md5.getMd5Hash(new ByteArrayInputStream("contents".getBytes())), "http://file1")
                )));

        FormSource formSource = mock(FormSource.class);
        when(formSource.fetchForm("http://downloadUrl")).thenReturn(new ByteArrayInputStream(xform.getBytes()));
        when(formSource.fetchMediaFile("http://file1")).thenReturn(new ByteArrayInputStream("contents".getBytes()));

        ServerFormDownloader downloader = new ServerFormDownloader(formSource, formsRepository, cacheDir, formsDir.getAbsolutePath(), FormMetadataParser.INSTANCE, clock, entitiesRepository, entitySource);

        // Initial download
        downloader.downloadForm(serverFormDetails, null, null);

        List<Form> allForms = formsRepository.getAll();
        assertThat(allForms.size(), is(1));
        Form form = allForms.get(0);
        assertThat(form.getLastDetectedAttachmentsUpdateDate(), is(nullValue()));

        ServerFormDetails serverFormDetailsUpdatedMediaFile = new ServerFormDetails(
                "Form",
                "http://downloadUrl",
                "id",
                "version",
                Md5.getMd5Hash(new ByteArrayInputStream(xform.getBytes())),
                false,
                false,
                new ManifestFile("", List.of(
                        new MediaFile("file1", Md5.getMd5Hash(new ByteArrayInputStream("contents-updated".getBytes())), "http://file1")
                )));

        when(formSource.fetchForm("http://downloadUrl")).thenReturn(new ByteArrayInputStream(xform.getBytes()));
        when(formSource.fetchMediaFile("http://file1")).thenReturn(new ByteArrayInputStream("contents-updated".getBytes()));

        // Second download
        downloader.downloadForm(serverFormDetailsUpdatedMediaFile, null, null);

        allForms = formsRepository.getAll();
        assertThat(allForms.size(), is(1));
        form = allForms.get(0);
        assertThat(form.getFormId(), is("id"));

        File formFile = new File(getAbsoluteFilePath(formsDir.getAbsolutePath(), form.getFormFilePath()));
        assertThat(formFile.exists(), is(true));
        assertThat(new String(read(formFile)), is(xform));

        File mediaFile1 = new File(form.getFormMediaPath() + "/file1");
        assertThat(mediaFile1.exists(), is(true));
        assertThat(new String(read(mediaFile1)), is("contents-updated"));

        assertThat(form.getLastDetectedAttachmentsUpdateDate(), is(123L));
    }

    @Test
    public void whenFormAlreadyDownloaded_andFormHasNewMediaFiles_andMediaFetchFails_throwsFetchError() throws Exception {
        String xform = createXFormBody("id", "version");
        ServerFormDetails serverFormDetails = new ServerFormDetails(
                "Form",
                "http://downloadUrl",
                "id",
                "version",
                Md5.getMd5Hash(new ByteArrayInputStream(xform.getBytes())),
                true,
                false,
                new ManifestFile("", List.of(
                        new MediaFile("file1", Md5.getMd5Hash(new ByteArrayInputStream("contents".getBytes())), "http://file1")
                )));

        FormSource formSource = mock(FormSource.class);
        when(formSource.fetchForm("http://downloadUrl")).thenReturn(new ByteArrayInputStream(xform.getBytes()));
        when(formSource.fetchMediaFile("http://file1")).thenReturn(new ByteArrayInputStream("contents".getBytes()));

        ServerFormDownloader downloader = new ServerFormDownloader(formSource, formsRepository, cacheDir, formsDir.getAbsolutePath(), FormMetadataParser.INSTANCE, clock, entitiesRepository, entitySource);

        // Initial download
        downloader.downloadForm(serverFormDetails, null, null);

        try {
            ServerFormDetails serverFormDetailsUpdatedMediaFile = new ServerFormDetails(
                    "Form",
                    "http://downloadUrl",
                    "id",
                    "version",
                    Md5.getMd5Hash(new ByteArrayInputStream(xform.getBytes())),
                    false,
                    false,
                    new ManifestFile("", List.of(
                            new MediaFile("file1", Md5.getMd5Hash(new ByteArrayInputStream("contents-updated".getBytes())), "http://file1")
                    )));

            when(formSource.fetchForm("http://downloadUrl")).thenReturn(new ByteArrayInputStream(xform.getBytes()));
            when(formSource.fetchMediaFile("http://file1")).thenThrow(new FormSourceException.FetchError());
            downloader.downloadForm(serverFormDetailsUpdatedMediaFile, null, null);
            fail("Expected exception");
        } catch (FormDownloadException.FormSourceError e) {
            // Check form is still intact
            List<Form> allForms = formsRepository.getAll();
            assertThat(allForms.size(), is(1));
            Form form = allForms.get(0);
            assertThat(form.getFormId(), is("id"));

            File formFile = new File(getAbsoluteFilePath(formsDir.getAbsolutePath(), form.getFormFilePath()));
            assertThat(formFile.exists(), is(true));
            assertThat(new String(read(formFile)), is(xform));
        }
    }

    @Test
    public void afterDownloadingXForm_cancelling_throwsDownloadingInterruptedExceptionAndDoesNotSaveAnything() throws Exception {
        String xform = createXFormBody("id", "version");
        ServerFormDetails serverFormDetails = new ServerFormDetails(
                "Form",
                "http://downloadUrl",
                "id",
                "version",
                Md5.getMd5Hash(new ByteArrayInputStream(xform.getBytes())),
                true,
                false,
                null);

        CancelAfterFormDownloadFormSource formListApi = new CancelAfterFormDownloadFormSource(xform);
        ServerFormDownloader downloader = new ServerFormDownloader(formListApi, formsRepository, cacheDir, formsDir.getAbsolutePath(), FormMetadataParser.INSTANCE, clock, entitiesRepository, entitySource);

        try {
            downloader.downloadForm(serverFormDetails, null, formListApi);
            fail("Expected exception");
        } catch (FormDownloadException.DownloadingInterrupted e) {
            assertThat(formsRepository.getAll(), is(empty()));
            assertThat(asList(new File(getCacheFilesPath()).listFiles()), is(empty()));
            assertThat(asList(new File(getFormFilesPath()).listFiles()), is(empty()));
        }
    }

    @Test
    public void afterDownloadingMediaFile_cancelling_throwsDownloadingInterruptedExceptionAndDoesNotSaveAnything() throws Exception {
        String xform = createXFormBody("id", "version");
        ServerFormDetails serverFormDetails = new ServerFormDetails(
                "Form",
                "http://downloadUrl",
                "id",
                "version",
                Md5.getMd5Hash(new ByteArrayInputStream(xform.getBytes())),
                true,
                false,
                new ManifestFile("", asList(
                        new MediaFile("file1", "hash-1", "http://file1"),
                        new MediaFile("file2", "hash-2", "http://file2")
                )));

        CancelAfterMediaFileDownloadFormSource formListApi = new CancelAfterMediaFileDownloadFormSource(xform);
        ServerFormDownloader downloader = new ServerFormDownloader(formListApi, formsRepository, cacheDir, formsDir.getAbsolutePath(), FormMetadataParser.INSTANCE, clock, entitiesRepository, entitySource);

        try {
            downloader.downloadForm(serverFormDetails, null, formListApi);
            fail("Excepted exception");
        } catch (FormDownloadException.DownloadingInterrupted e) {
            assertThat(formsRepository.getAll(), is(empty()));
            assertThat(asList(new File(getCacheFilesPath()).listFiles()), is(empty()));
            assertThat(asList(new File(getFormFilesPath()).listFiles()), is(empty()));
        }
    }

    @Test
    public void whenFormHasEntityAttachmentsButNoEntityBlock_downloadsAndSavesFormAsAFormUsingEntities() throws Exception {
        String xform = createXFormBody("id", "version");
        ServerFormDetails serverFormDetails = new ServerFormDetails(
                "Form",
                "http://downloadUrl",
                "id",
                "version",
                Md5.getMd5Hash(new ByteArrayInputStream(xform.getBytes())),
                true,
                false,
                new ManifestFile("", List.of(
                        new MediaFile("file1", "hash-1", "http://file1", true)
                )));

        FormSource formSource = mock(FormSource.class);
        when(formSource.fetchForm("http://downloadUrl")).thenReturn(new ByteArrayInputStream(xform.getBytes()));
        when(formSource.fetchMediaFile("http://file1")).thenReturn(new ByteArrayInputStream("contents1".getBytes()));

        ServerFormDownloader downloader = new ServerFormDownloader(formSource, formsRepository, cacheDir, formsDir.getAbsolutePath(), FormMetadataParser.INSTANCE, clock, entitiesRepository, entitySource);
        downloader.downloadForm(serverFormDetails, null, null);

        List<Form> allForms = formsRepository.getAll();
        assertThat(allForms.size(), is(1));
        Form form = allForms.get(0);
        assertThat(form.usesEntities(), is(true));
    }

    @Test
    public void whenFormHasEntityAttachmentsAndEntityBlock_downloadsAndSavesFormAsAFormUsingEntities() throws Exception {
        String xform = createXFormBody("id", "version", "Test Form", "2024.1.0");
        ServerFormDetails serverFormDetails = new ServerFormDetails(
                "Form",
                "http://downloadUrl",
                "id",
                "version",
                Md5.getMd5Hash(new ByteArrayInputStream(xform.getBytes())),
                true,
                false,
                new ManifestFile("", List.of(
                        new MediaFile("file1", "hash-1", "http://file1", true)
                )));

        FormSource formSource = mock(FormSource.class);
        when(formSource.fetchForm("http://downloadUrl")).thenReturn(new ByteArrayInputStream(xform.getBytes()));
        when(formSource.fetchMediaFile("http://file1")).thenReturn(new ByteArrayInputStream("contents1".getBytes()));

        ServerFormDownloader downloader = new ServerFormDownloader(formSource, formsRepository, cacheDir, formsDir.getAbsolutePath(), FormMetadataParser.INSTANCE, clock, entitiesRepository, entitySource);
        downloader.downloadForm(serverFormDetails, null, null);

        List<Form> allForms = formsRepository.getAll();
        assertThat(allForms.size(), is(1));
        Form form = allForms.get(0);
        assertThat(form.usesEntities(), is(true));
    }

    @Test
    public void whenFormLacksEntityAttachmentsButHasEntityBlock_downloadsAndSavesFormAsAFormUsingEntities() throws Exception {
        String xform = createXFormBody("id", "version", "Test Form", "2024.1.0");
        ServerFormDetails serverFormDetails = new ServerFormDetails(
                "Form",
                "http://downloadUrl",
                "id",
                "version",
                Md5.getMd5Hash(new ByteArrayInputStream(xform.getBytes())),
                true,
                false,
                new ManifestFile("", List.of(
                        new MediaFile("file1", "hash-1", "http://file1")
                )));

        FormSource formSource = mock(FormSource.class);
        when(formSource.fetchForm("http://downloadUrl")).thenReturn(new ByteArrayInputStream(xform.getBytes()));
        when(formSource.fetchMediaFile("http://file1")).thenReturn(new ByteArrayInputStream("contents1".getBytes()));

        ServerFormDownloader downloader = new ServerFormDownloader(formSource, formsRepository, cacheDir, formsDir.getAbsolutePath(), FormMetadataParser.INSTANCE, clock, entitiesRepository, entitySource);
        downloader.downloadForm(serverFormDetails, null, null);

        List<Form> allForms = formsRepository.getAll();
        assertThat(allForms.size(), is(1));
        Form form = allForms.get(0);
        assertThat(form.usesEntities(), is(true));
    }

    @Test
    public void whenFormLacksEntityAttachmentsAndEntityBlock_downloadsAndSavesFormAsAFormNotUsingEntities() throws Exception {
        String xform = createXFormBody("id", "version");
        ServerFormDetails serverFormDetails = new ServerFormDetails(
                "Form",
                "http://downloadUrl",
                "id",
                "version",
                Md5.getMd5Hash(new ByteArrayInputStream(xform.getBytes())),
                true,
                false,
                new ManifestFile("", List.of(
                        new MediaFile("file1", "hash-1", "http://file1")
                )));

        FormSource formSource = mock(FormSource.class);
        when(formSource.fetchForm("http://downloadUrl")).thenReturn(new ByteArrayInputStream(xform.getBytes()));
        when(formSource.fetchMediaFile("http://file1")).thenReturn(new ByteArrayInputStream("contents1".getBytes()));

        ServerFormDownloader downloader = new ServerFormDownloader(formSource, formsRepository, cacheDir, formsDir.getAbsolutePath(), FormMetadataParser.INSTANCE, clock, entitiesRepository, entitySource);
        downloader.downloadForm(serverFormDetails, null, null);

        List<Form> allForms = formsRepository.getAll();
        assertThat(allForms.size(), is(1));
        Form form = allForms.get(0);
        assertThat(form.usesEntities(), is(false));
    }

    @Test
    public void whenFormAlreadyDownloadedDoesNotHaveEntities_andOneOfTheAttachmentsBecomesMarkedAsAnEntityList_downloadsAndMarkTheFormAsAFormThatUsesEntities() throws Exception {
        String xform = createXFormBody("id", "version");
        ServerFormDetails serverFormDetails = new ServerFormDetails(
                "Form",
                "http://downloadUrl",
                "id",
                "version",
                Md5.getMd5Hash(new ByteArrayInputStream(xform.getBytes())),
                true,
                false,
                new ManifestFile("", List.of(
                        new MediaFile("file1", Md5.getMd5Hash(new ByteArrayInputStream("contents".getBytes())), "http://file1")
                )));

        FormSource formSource = mock(FormSource.class);
        when(formSource.fetchForm("http://downloadUrl")).thenReturn(new ByteArrayInputStream(xform.getBytes()));
        when(formSource.fetchMediaFile("http://file1")).thenReturn(new ByteArrayInputStream("contents".getBytes()));

        ServerFormDownloader downloader = new ServerFormDownloader(formSource, formsRepository, cacheDir, formsDir.getAbsolutePath(), FormMetadataParser.INSTANCE, clock, entitiesRepository, entitySource);

        // Initial download
        downloader.downloadForm(serverFormDetails, null, null);

        List<Form> allForms = formsRepository.getAll();
        assertThat(allForms.size(), is(1));
        Form form = allForms.get(0);
        assertThat(form.usesEntities(), is(false));

        ServerFormDetails serverFormDetailsUpdatedMediaFile = new ServerFormDetails(
                "Form",
                "http://downloadUrl",
                "id",
                "version",
                Md5.getMd5Hash(new ByteArrayInputStream(xform.getBytes())),
                false,
                false,
                new ManifestFile("", List.of(
                        new MediaFile("file1", Md5.getMd5Hash(new ByteArrayInputStream("contents".getBytes())), "http://file1", true)
                )));

        when(formSource.fetchForm("http://downloadUrl")).thenReturn(new ByteArrayInputStream(xform.getBytes()));
        when(formSource.fetchMediaFile("http://file1")).thenReturn(new ByteArrayInputStream("contents".getBytes()));

        // Second download
        downloader.downloadForm(serverFormDetailsUpdatedMediaFile, null, null);

        allForms = formsRepository.getAll();
        assertThat(allForms.size(), is(1));
        form = allForms.get(0);
        assertThat(form.usesEntities(), is(true));
    }

    private String getFormFilesPath() {
        return formsDir.getAbsolutePath();
    }

    private String getCacheFilesPath() {
        return cacheDir.getAbsolutePath();
    }

    public static class RecordingProgressReporter implements FormDownloader.ProgressReporter {

        List<Integer> reports = new ArrayList<>();

        @Override
        public void onDownloadingMediaFile(int count) {
            reports.add(count);
        }
    }

    public static class CancelAfterFormDownloadFormSource implements FormSource, Supplier<Boolean> {

        private final String xform;
        private boolean isCancelled;

        public CancelAfterFormDownloadFormSource(String xform) {
            this.xform = xform;
        }

        @Override
        public InputStream fetchForm(String formURL) {
            isCancelled = true;
            return new ByteArrayInputStream(xform.getBytes());
        }

        @Override
        public List<FormListItem> fetchFormList() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ManifestFile fetchManifest(String manifestURL) {
            throw new UnsupportedOperationException();
        }

        @Override
        public InputStream fetchMediaFile(String mediaFileURL) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Boolean get() {
            return isCancelled;
        }
    }

    public static class CancelAfterMediaFileDownloadFormSource implements FormSource, Supplier<Boolean> {

        private final String xform;
        private boolean isCancelled;

        public CancelAfterMediaFileDownloadFormSource(String xform) {
            this.xform = xform;
        }

        @Override
        public InputStream fetchForm(String formURL) {
            return new ByteArrayInputStream(xform.getBytes());
        }

        @Override
        public InputStream fetchMediaFile(String mediaFileURL) {
            isCancelled = true;
            return new ByteArrayInputStream("contents".getBytes());
        }

        @Override
        public List<FormListItem> fetchFormList() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ManifestFile fetchManifest(String manifestURL) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Boolean get() {
            return isCancelled;
        }
    }
}
