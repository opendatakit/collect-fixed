/*
 * Copyright (C) 2017 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.odk.collect.android.utilities;

import static org.odk.collect.strings.localization.LocalizedApplicationKt.getLocalizedString;
import static java.util.Arrays.asList;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.CharMatcher;

import org.apache.commons.io.IOUtils;
import org.odk.collect.android.application.Collect;
import org.odk.collect.async.OngoingWorkListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * Static methods used for common file operations.
 *
 * @author Carl Hartung (carlhartung@gmail.com)
 */
public final class FileUtils {
    /** Suffix for the form media directory. */
    public static final String MEDIA_SUFFIX = "-media";

    /** Filename of the last-saved instance data. */
    public static final String LAST_SAVED_FILENAME = "last-saved.xml";

    /** Valid XML stub that can be parsed without error. */
    public static final String STUB_XML = "<?xml version='1.0' ?><stub />";

    private FileUtils() {
    }

    public static void saveAnswerFileFromUri(Uri uri, File destFile, Context context) {
        try (InputStream fileInputStream = context.getContentResolver().openInputStream(uri);
             OutputStream fileOutputStream = new FileOutputStream(destFile)) {
            IOUtils.copy(fileInputStream, fileOutputStream);
        } catch (IOException e) {
            Timber.e(e);
        }
    }

    public static File createDestinationMediaFile(String fileLocation, String fileExtension) {
        return new File(fileLocation
                + File.separator
                + System.currentTimeMillis()
                + "."
                + fileExtension);
    }

    public static boolean createFolder(String path) {
        File dir = new File(path);
        return dir.exists() || dir.mkdirs();
    }

    public static String copyFile(File sourceFile, File destFile) {
        if (sourceFile.exists()) {
            String errorMessage = actualCopy(sourceFile, destFile);
            if (errorMessage != null) {
                try {
                    Thread.sleep(500);
                    Timber.e(new Error("Retrying to copy the file after 500ms: " + sourceFile.getAbsolutePath()));
                    errorMessage = actualCopy(sourceFile, destFile);
                } catch (InterruptedException e) {
                    Timber.i(e);
                }
            }
            return errorMessage;
        } else {
            String msg = "Source file does not exist: " + sourceFile.getAbsolutePath();
            Timber.e(new Error(msg));
            return msg;
        }
    }

    private static String actualCopy(File sourceFile, File destFile) {
        FileInputStream fileInputStream = null;
        FileOutputStream fileOutputStream = null;
        FileChannel src = null;
        FileChannel dst = null;
        try {
            fileInputStream = new FileInputStream(sourceFile);
            src = fileInputStream.getChannel();
            fileOutputStream = new FileOutputStream(destFile);
            dst = fileOutputStream.getChannel();
            dst.transferFrom(src, 0, src.size());
            dst.force(true);
            return null;
        } catch (Exception e) {
            if (e instanceof FileNotFoundException) {
                Timber.e(e, "FileNotFoundException while copying file");
            } else if (e instanceof IOException) {
                Timber.e(e, "IOException while copying file");
            } else {
                Timber.e(e, "Exception while copying file");
            }
            return e.getMessage();
        } finally {
            IOUtils.closeQuietly(fileInputStream);
            IOUtils.closeQuietly(fileOutputStream);
            IOUtils.closeQuietly(src);
            IOUtils.closeQuietly(dst);
        }
    }

    public static void deleteAndReport(File file) {
        if (file != null && file.exists()) {
            // remove garbage
            if (!file.delete()) {
                Timber.d("%s will be deleted upon exit.", file.getAbsolutePath());
                file.deleteOnExit();
            } else {
                Timber.d("%s has been deleted.", file.getAbsolutePath());
            }
        }
    }

    public static String getFormBasename(File formXml) {
        return getFormBasename(formXml.getName());
    }

    public static String getFormBasename(String formFilePath) {
        return formFilePath.substring(0, formFilePath.lastIndexOf('.'));
    }

    public static String constructMediaPath(String formFilePath) {
        return getFormBasename(formFilePath) + MEDIA_SUFFIX;
    }

    public static File getFormMediaDir(File formXml) {
        final String formFileName = getFormBasename(formXml);
        return new File(formXml.getParent(), formFileName + MEDIA_SUFFIX);
    }

    public static String getFormBasenameFromMediaFolder(File mediaFolder) {
        /*
         * TODO (from commit 37e3467): Apparently the form name is neither
         * in the formController nor the formDef. In fact, it doesn't seem to
         * be saved into any object in JavaRosa. However, the mediaFolder
         * has the substring of the file name in it, so we extract the file name
         * from here. Awkward...
         */
        return mediaFolder.getName().split(MEDIA_SUFFIX)[0];
    }

    public static File getLastSavedFile(File formXml) {
        return new File(getFormMediaDir(formXml), LAST_SAVED_FILENAME);
    }

    public static String getLastSavedPath(File mediaFolder) {
        return mediaFolder.getAbsolutePath() + File.separator + LAST_SAVED_FILENAME;
    }

    /**
     * Returns the path to the last-saved file for this form,
     * creating a valid stub if it doesn't yet exist.
     */
    public static String getOrCreateLastSavedSrc(File formXml) {
        File lastSavedFile = getLastSavedFile(formXml);

        if (!lastSavedFile.exists()) {
            write(lastSavedFile, STUB_XML.getBytes(StandardCharsets.UTF_8));
        }

        return "jr://file/" + LAST_SAVED_FILENAME;
    }

    /**
     * @param mediaDir the media folder
     */
    public static void checkMediaPath(File mediaDir) {
        if (mediaDir.exists() && mediaDir.isFile()) {
            Timber.e(new Error("The media folder is already there and it is a FILE!! We will need to delete it and create a folder instead"));
            boolean deleted = mediaDir.delete();
            if (!deleted) {
                throw new RuntimeException(
                        getLocalizedString(Collect.getInstance(), org.odk.collect.strings.R.string.fs_delete_media_path_if_file_error,
                                mediaDir.getAbsolutePath()));
            }
        }

        // the directory case
        boolean createdOrExisted = createFolder(mediaDir.getAbsolutePath());
        if (!createdOrExisted) {
            throw new RuntimeException(
                    getLocalizedString(Collect.getInstance(), org.odk.collect.strings.R.string.fs_create_media_folder_error,
                            mediaDir.getAbsolutePath()));
        }
    }

    public static void purgeMediaPath(String mediaPath) {
        File tempMediaFolder = new File(mediaPath);
        File[] tempMediaFiles = tempMediaFolder.listFiles();

        if (tempMediaFiles != null) {
            for (File tempMediaFile : tempMediaFiles) {
                deleteAndReport(tempMediaFile);
            }
        }

        deleteAndReport(tempMediaFolder);
    }

    public static byte[] read(File file) {
        byte[] bytes = new byte[(int) file.length()];
        try (InputStream is = new FileInputStream(file)) {
            is.read(bytes);
        } catch (IOException e) {
            Timber.e(e);
        }
        return bytes;
    }

    public static void write(File file, byte[] data) {
        // Make sure the directory path to this file exists.
        file.getParentFile().mkdirs();

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        } catch (IOException e) {
            Timber.e(e);
        }
    }

    public static void grantFilePermissions(Intent intent, Uri uri, Context context) {
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }

    @SuppressWarnings("PMD.DoNotHardCodeSDCard")
    public static String expandAndroidStoragePath(String path) {
        if (path != null && path.startsWith("/storage/emulated/0/")) {
            return "/sdcard/" + path.substring("/storage/emulated/0/".length());
        }

        return path;
    }

    public static String getMimeType(File file) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(file.getAbsolutePath());
        String mimeType = extension != null ? MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) : null;

        if (mimeType == null || mimeType.isEmpty()) {
            FileNameMap fileNameMap = URLConnection.getFileNameMap();
            mimeType = fileNameMap.getContentTypeFor(file.getAbsolutePath());
        }

        if (mimeType == null || mimeType.isEmpty()) {
            mimeType = URLConnection.guessContentTypeFromName(file.getName());
        }

        return mimeType;
    }

    public static List<File> listFiles(File file) {
        if (file != null && file.exists()) {
            return asList(file.listFiles());
        } else {
            return new ArrayList<>();
        }
    }

    public static String getFilenameError(String filename) {
        String possiblyRestricted = "?:\"*|/\\<>\u0000";
        boolean containsAt = filename.contains("@");
        boolean containsNonAscii = CharMatcher.ascii().matchesAllOf(filename);
        boolean containsPossiblyRestricted = CharMatcher.anyOf(possiblyRestricted).matchesAnyOf(possiblyRestricted);
        return "Problem with project name file. Contains @: " + containsAt + ", Contains non-ascii: " + containsNonAscii + ", Contains restricted: " + containsPossiblyRestricted;
    }

    /**
     * Common routine to take a downloaded document save the contents in the file
     * 'file'.
     * <p>
     * The file is saved into a temp folder and is moved to the final place if everything
     * is okay, so that garbage is not left over on cancel.
     */
    public static void interuptablyWriteFile(InputStream inputStream, File destinationFile, File tempDir, OngoingWorkListener listener)
            throws IOException, InterruptedException {

        File tempFile = File.createTempFile(
                destinationFile.getName(),
                ".tempDownload",
                tempDir
        );

        // WiFi network connections can be renegotiated during a large download sequence.
        // This will cause intermittent download failures.  Silently retry once after each
        // failure.  Only if there are two consecutive failures do we abort.
        boolean success = false;
        int attemptCount = 0;
        final int maxAttemptCount = 2;
        while (!success && ++attemptCount <= maxAttemptCount) {
            // write connection to file
            InputStream is = null;
            OutputStream os = null;

            try {
                is = inputStream;
                os = new FileOutputStream(tempFile);

                byte[] buf = new byte[4096];
                int len;
                while ((len = is.read(buf)) > 0 && (listener == null || !listener.isCancelled())) {
                    os.write(buf, 0, len);
                }
                os.flush();
                success = true;

            } catch (Exception e) {
                Timber.e(e);
                // silently retry unless this is the last attempt,
                // in which case we rethrow the exception.

                FileUtils.deleteAndReport(tempFile);

                if (attemptCount == maxAttemptCount) {
                    throw e;
                }
            } finally {
                if (os != null) {
                    try {
                        os.close();
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                }
                if (is != null) {
                    try {
                        // ensure stream is consumed...
                        final long count = 1024L;
                        while (is.skip(count) == count) {
                            // skipping to the end of the http entity
                        }
                    } catch (Exception e) {
                        // no-op
                    }
                    try {
                        is.close();
                    } catch (Exception e) {
                        Timber.w(e);
                    }
                }
            }

            if (listener != null && listener.isCancelled()) {
                FileUtils.deleteAndReport(tempFile);
                throw new InterruptedException();
            }
        }

        Timber.d("Completed downloading of %s. It will be moved to the proper path...", tempFile.getAbsolutePath());

        FileUtils.deleteAndReport(destinationFile);

        String errorMessage = FileUtils.copyFile(tempFile, destinationFile);

        if (destinationFile.exists()) {
            Timber.d("Copied %s over %s", tempFile.getAbsolutePath(), destinationFile.getAbsolutePath());
            FileUtils.deleteAndReport(tempFile);
        } else {
            String msg = Collect.getInstance().getString(org.odk.collect.strings.R.string.fs_file_copy_error,
                    tempFile.getAbsolutePath(), destinationFile.getAbsolutePath(), errorMessage);
            throw new RuntimeException(msg);
        }
    }

    public static void copyFileFromAssets(Context context, String fileDestPath, String fileSourcePath) throws IOException {
        copyStreamToPath(getAssetAsStream(context, fileSourcePath), fileDestPath);
    }

    public static File copyFileFromResources(String fileSourcePath, File fileDest) throws IOException {
        copyStreamToPath(getResourceAsStream(fileSourcePath), fileDest.getAbsolutePath());
        return fileDest;
    }

    public static void copyFileFromResources(String fileSourcePath, String fileDestPath) throws IOException {
        copyStreamToPath(getResourceAsStream(fileSourcePath), fileDestPath);
    }

    @NonNull
    public static InputStream getAssetAsStream(Context context, String fileSourcePath) throws IOException {
        return context.getAssets().open(fileSourcePath);
    }

    @Nullable
    public static InputStream getResourceAsStream(String fileSourcePath) {
        return FileUtils.class.getResourceAsStream("/" + fileSourcePath);
    }

    private static void copyStreamToPath(InputStream inputStream, String fileDestPath) throws IOException {
        try (InputStream input = inputStream;
             OutputStream output = new FileOutputStream(fileDestPath)) {
            IOUtils.copy(input, output);
        }
    }
}
