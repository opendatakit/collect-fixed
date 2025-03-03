/*
 * Copyright (C) 2018 Nafundi
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

package org.odk.collect.android.upload;

import static org.odk.collect.strings.localization.LocalizedApplicationKt.getLocalizedString;

import android.net.Uri;

import androidx.annotation.NonNull;

import org.odk.collect.android.application.Collect;
import org.odk.collect.android.utilities.ResponseMessageParser;
import org.odk.collect.android.utilities.WebCredentialsUtils;
import org.odk.collect.forms.instances.Instance;
import org.odk.collect.forms.instances.InstancesRepository;
import org.odk.collect.openrosa.http.CaseInsensitiveHeaders;
import org.odk.collect.openrosa.http.HttpHeadResult;
import org.odk.collect.openrosa.http.HttpPostResult;
import org.odk.collect.openrosa.http.OpenRosaConstants;
import org.odk.collect.openrosa.http.OpenRosaHttpInterface;
import org.odk.collect.settings.keys.ProjectKeys;
import org.odk.collect.shared.settings.Settings;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import timber.log.Timber;

public class InstanceServerUploader extends InstanceUploader {
    private static final String URL_PATH_SEP = "/";

    private final OpenRosaHttpInterface httpInterface;
    private final WebCredentialsUtils webCredentialsUtils;
    private final Settings generalSettings;
    private final Map<Uri, Uri> uriRemap = new HashMap<>();

    public InstanceServerUploader(OpenRosaHttpInterface httpInterface,
                                  WebCredentialsUtils webCredentialsUtils,
                                  Settings generalSettings, InstancesRepository instancesRepository) {
        super(instancesRepository);
        this.httpInterface = httpInterface;
        this.webCredentialsUtils = webCredentialsUtils;
        this.generalSettings = generalSettings;
    }

    /**
     * Uploads all files associated with an instance to the specified URL. Writes fail/success
     * status to database.
     * <p>
     * Returns a custom success message if one is provided by the server.
     */
    @Override
    public String uploadOneSubmission(Instance instance, String urlString) throws FormUploadException {
        markSubmissionFailed(instance);

        Uri submissionUri = Uri.parse(urlString);

        long contentLength = 10000000L;

        // We already issued a head request and got a response, so we know it was an
        // OpenRosa-compliant server. We also know the proper URL to send the submission to and
        // the proper scheme.
        if (uriRemap.containsKey(submissionUri)) {
            submissionUri = uriRemap.get(submissionUri);
            Timber.i("Using Uri remap for submission %s. Now: %s", instance.getDbId(),
                    submissionUri.toString());
        } else {
            if (submissionUri.getHost() == null) {
                throw new FormUploadException(FAIL + "Host name may not be null");
            }

            URI uri;
            try {
                uri = URI.create(submissionUri.toString());
            } catch (IllegalArgumentException e) {
                Timber.d(e.getMessage() != null ? e.getMessage() : e.toString());
                throw new FormUploadException(getLocalizedString(Collect.getInstance(), org.odk.collect.strings.R.string.url_error));
            }

            HttpHeadResult headResult;
            CaseInsensitiveHeaders responseHeaders;
            try {
                headResult = httpInterface.executeHeadRequest(uri, webCredentialsUtils.getCredentials(uri));
                responseHeaders = headResult.getHeaders();

                if (responseHeaders.containsHeader(OpenRosaConstants.ACCEPT_CONTENT_LENGTH_HEADER)) {
                    String contentLengthString = responseHeaders.getAnyValue(OpenRosaConstants.ACCEPT_CONTENT_LENGTH_HEADER);
                    try {
                        contentLength = Long.parseLong(contentLengthString);
                    } catch (Exception e) {
                        Timber.e(e, "Exception thrown parsing contentLength %s", contentLengthString);
                    }
                }

            } catch (Exception e) {
                throw new FormUploadException(FAIL
                        + (e.getMessage() != null ? e.getMessage() : e.toString()));
            }

            if (headResult.getStatusCode() == HttpsURLConnection.HTTP_UNAUTHORIZED) {
                throw new FormUploadAuthRequestedException(getLocalizedString(Collect.getInstance(), org.odk.collect.strings.R.string.server_auth_credentials, submissionUri.getHost()),
                        submissionUri);
            } else if (headResult.getStatusCode() == HttpsURLConnection.HTTP_NO_CONTENT) {
                // Redirect header received
                if (responseHeaders.containsHeader("Location")) {
                    try {
                        Uri newURI = Uri.parse(URLDecoder.decode(responseHeaders.getAnyValue("Location"), "utf-8"));
                        // Allow redirects within same host. This could be redirecting to HTTPS.
                        if (submissionUri.getHost().equalsIgnoreCase(newURI.getHost())) {
                            // Re-add params if server didn't respond with params
                            if (newURI.getQuery() == null) {
                                newURI = newURI.buildUpon()
                                        .encodedQuery(submissionUri.getEncodedQuery())
                                        .build();
                            }
                            uriRemap.put(submissionUri, newURI);
                            submissionUri = newURI;
                        } else {
                            // Don't follow a redirection attempt to a different host.
                            // We can't tell if this is a spoof or not.
                            throw new FormUploadException(FAIL
                                    + "Unexpected redirection attempt to a different host: "
                                    + newURI.toString());
                        }
                    } catch (Exception e) {
                        throw new FormUploadException(FAIL + urlString + " " + e.toString());
                    }
                }
            } else {
                if (headResult.getStatusCode() >= HttpsURLConnection.HTTP_OK
                        && headResult.getStatusCode() < HttpsURLConnection.HTTP_MULT_CHOICE) {
                    throw new FormUploadException("Failed to send to " + uri + ". Is this an OpenRosa " +
                            "submission endpoint? If you have a web proxy you may need to log in to " +
                            "your network.\n\nHEAD request result status code: " + headResult.getStatusCode());
                }
            }
        }

        // When encrypting submissions, there is a failure window that may mark the submission as
        // complete but leave the file-to-be-uploaded with the name "submission.xml" and the plaintext
        // submission files on disk.  In this case, upload the submission.xml and all the files in
        // the directory. This means the plaintext files and the encrypted files will be sent to the
        // server and the server will have to figure out what to do with them.
        File instanceFile = new File(instance.getInstanceFilePath());
        File submissionFile = new File(instanceFile.getParentFile(), "submission.xml");
        if (submissionFile.exists()) {
            Timber.w("submission.xml will be uploaded instead of %s", instanceFile.getAbsolutePath());
        } else {
            submissionFile = instanceFile;
        }

        if (!instanceFile.exists() && !submissionFile.exists()) {
            throw new FormUploadException(FAIL + "instance XML file does not exist!");
        }

        List<File> files = getFilesInParentDirectory(instanceFile, submissionFile);

        // TODO: when can this happen? It used to cause the whole submission attempt to fail. Should it?
        if (files == null) {
            throw new FormUploadException("Error reading files to upload");
        }

        HttpPostResult postResult;
        ResponseMessageParser messageParser = new ResponseMessageParser();

        try {
            URI uri = URI.create(submissionUri.toString());

            postResult = httpInterface.uploadSubmissionAndFiles(submissionFile, files, uri,
                    webCredentialsUtils.getCredentials(uri), contentLength);

            int responseCode = postResult.getResponseCode();
            messageParser.setMessageResponse(postResult.getHttpResponse());

            if (responseCode != HttpsURLConnection.HTTP_CREATED && responseCode != HttpsURLConnection.HTTP_ACCEPTED) {
                FormUploadException exception;
                if (responseCode == HttpsURLConnection.HTTP_OK) {
                    exception = new FormUploadException(FAIL + "Network login failure? Again?");
                } else if (responseCode == HttpsURLConnection.HTTP_UNAUTHORIZED) {
                    exception = new FormUploadException(FAIL + postResult.getReasonPhrase()
                            + " (" + responseCode + ") at " + urlString);
                } else {
                    if (messageParser.isValid()) {
                        exception = new FormUploadException(FAIL + messageParser.getMessageResponse());
                    } else if (responseCode == HttpsURLConnection.HTTP_BAD_REQUEST) {
                        Timber.w(FAIL + postResult.getReasonPhrase() + " (" + responseCode + ") at " + urlString);
                        exception = new FormUploadException("Failed to upload. Please make sure the form is configured to accept submissions on the server");
                    } else {
                        exception = new FormUploadException(FAIL + postResult.getReasonPhrase() + " (" + responseCode + ") at " + urlString);
                    }

                }
                throw exception;
            }

        } catch (Exception e) {
            throw new FormUploadException(e.getMessage() != null ? e.getMessage() : e.toString());
        }

        markSubmissionComplete(instance);

        if (messageParser.isValid()) {
            return messageParser.getMessageResponse();
        }

        return null;
    }

    private List<File> getFilesInParentDirectory(File instanceFile, File submissionFile) {
        List<File> files = new ArrayList<>();

        // find all files in parent directory
        File[] allFiles = instanceFile.getParentFile().listFiles();
        if (allFiles == null) {
            return null;
        }

        for (File f : allFiles) {
            String fileName = f.getName();

            if (fileName.startsWith(".")) {
                continue; // ignore invisible files
            } else if (fileName.equals(instanceFile.getName())) {
                continue; // the xml file has already been added
            } else if (fileName.equals(submissionFile.getName())) {
                continue; // the xml file has already been added
            }

            files.add(f);
        }

        return files;
    }


    /**
     * Returns the URL this instance should be submitted to with appended deviceId.
     * <p>
     * If the upload was triggered by an external app and specified an override URL, use that one.
     * Otherwise, use the submission URL configured in the form
     * (https://getodk.github.io/xforms-spec/#submission-attributes). Finally, default to the
     * URL configured at the app level.
     */
    @Override
    @NonNull
    public String getUrlToSubmitTo(Instance currentInstance, String deviceId, String overrideURL, String urlFromSettings) {
        String urlString;

        if (overrideURL != null) {
            urlString = overrideURL;
        } else if (currentInstance.getSubmissionUri() != null) {
            urlString = currentInstance.getSubmissionUri().trim();
        } else {
            urlString = getServerSubmissionURL();
        }

        // add deviceID to request
        try {
            urlString += "?deviceID=" + URLEncoder.encode(deviceId != null ? deviceId : "", "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Timber.i(e, "Error encoding URL for device id : %s", deviceId);
        }

        return urlString;
    }

    private String getServerSubmissionURL() {
        String serverBase = generalSettings.getString(ProjectKeys.KEY_SERVER_URL);

        if (serverBase.endsWith(URL_PATH_SEP)) {
            serverBase = serverBase.substring(0, serverBase.length() - 1);
        }

        return serverBase + OpenRosaConstants.SUBMISSION;
    }
}
