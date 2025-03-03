/*
 * Copyright (C) 2009 University of Washington
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

package org.odk.collect.android.instancemanagement.send;

import static java.util.Arrays.stream;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import org.odk.collect.android.activities.FormFillingActivity;
import org.odk.collect.android.fragments.dialogs.SimpleDialog;
import org.odk.collect.android.injection.DaggerUtils;
import org.odk.collect.android.listeners.InstanceUploaderListener;
import org.odk.collect.android.tasks.InstanceUploaderTask;
import org.odk.collect.android.utilities.ApplicationConstants;
import org.odk.collect.android.utilities.ArrayUtils;
import org.odk.collect.android.utilities.AuthDialogUtility;
import org.odk.collect.android.utilities.FormsRepositoryProvider;
import org.odk.collect.android.utilities.InstanceUploaderUtils;
import org.odk.collect.android.utilities.InstancesRepositoryProvider;
import org.odk.collect.android.views.DayNightProgressDialog;
import org.odk.collect.forms.FormsRepository;
import org.odk.collect.forms.instances.InstancesRepository;
import org.odk.collect.openrosa.http.OpenRosaConstants;
import org.odk.collect.settings.SettingsProvider;
import org.odk.collect.strings.localization.LocalizedActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import javax.inject.Inject;

import timber.log.Timber;

/**
 * Activity to upload completed forms.
 *
 * @author Carl Hartung (carlhartung@gmail.com)
 */
public class InstanceUploaderActivity extends LocalizedActivity implements InstanceUploaderListener,
        AuthDialogUtility.AuthDialogUtilityResultListener {
    private static final int PROGRESS_DIALOG = 1;
    private static final int AUTH_DIALOG = 2;

    private static final String AUTH_URI = "auth";
    private static final String ALERT_MSG = "alertmsg";
    private static final String TO_SEND = "tosend";

    private ProgressDialog progressDialog;

    private String alertMsg;

    private InstanceUploaderTask instanceUploaderTask;

    // maintain a list of what we've yet to send, in case we're interrupted by auth requests
    private Long[] instancesToSend;

    // URL specified when authentication is requested or specified from intent extra as override
    private String url;

    // Set from intent extras
    private String username;
    private String password;
    private Boolean deleteInstanceAfterUpload;

    private boolean isInstanceStateSaved;

    @Inject
    InstancesRepositoryProvider instancesRepositoryProvider;
    private InstancesRepository instancesRepository;

    @Inject
    FormsRepositoryProvider formsRepositoryProvider;
    private FormsRepository formsRepository;

    @Inject
    SettingsProvider settingsProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DaggerUtils.getComponent(this).inject(this);
        instancesRepository = instancesRepositoryProvider.create();
        formsRepository = formsRepositoryProvider.create();

        init(savedInstanceState);
    }

    private void init(Bundle savedInstanceState) {
        alertMsg = getString(org.odk.collect.strings.R.string.please_wait);

        setTitle(getString(org.odk.collect.strings.R.string.send_data));

        // Get simple saved state
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(ALERT_MSG)) {
                alertMsg = savedInstanceState.getString(ALERT_MSG);
            }

            url = savedInstanceState.getString(AUTH_URI);
        }

        Bundle dataBundle;

        // If we are resuming, use the TO_SEND list of not-yet-sent submissions
        // Otherwise, construct the list from the incoming intent value
        long[] selectedInstanceIDs;
        if (savedInstanceState != null && savedInstanceState.containsKey(TO_SEND)) {
            selectedInstanceIDs = savedInstanceState.getLongArray(TO_SEND);
            dataBundle = savedInstanceState;
        } else {
            selectedInstanceIDs = getIntent().getLongArrayExtra(FormFillingActivity.KEY_INSTANCES);
            dataBundle = getIntent().getExtras();

            boolean missingInstances = stream(selectedInstanceIDs).anyMatch(id -> instancesRepository.get(id) == null);
            if (missingInstances) {
                selectedInstanceIDs = new long[]{};
            }
        }

        // An external application can temporarily override destination URL, username, password
        // and whether instances should be deleted after submission by specifying intent extras.
        if (dataBundle != null && dataBundle.containsKey(ApplicationConstants.BundleKeys.URL)) {
            // TODO: I think this means redirection from a URL set through an extra is not supported
            url = dataBundle.getString(ApplicationConstants.BundleKeys.URL);

            // Remove trailing slashes (only necessary for the intent case but doesn't hurt on resume)
            while (url != null && url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }

            if (dataBundle.containsKey(ApplicationConstants.BundleKeys.USERNAME)
                    && dataBundle.containsKey(ApplicationConstants.BundleKeys.PASSWORD)) {
                username = dataBundle.getString(ApplicationConstants.BundleKeys.USERNAME);
                password = dataBundle.getString(ApplicationConstants.BundleKeys.PASSWORD);
            }

            if (dataBundle.containsKey(ApplicationConstants.BundleKeys.DELETE_INSTANCE_AFTER_SUBMISSION)) {
                deleteInstanceAfterUpload = dataBundle.getBoolean(ApplicationConstants.BundleKeys.DELETE_INSTANCE_AFTER_SUBMISSION);
            }
        }

        instancesToSend = ArrayUtils.toObject(selectedInstanceIDs);

        if (instancesToSend.length == 0) {
            Timber.e(new Error("onCreate: No instances to upload!"));
            // drop through -- everything will process through OK
        } else {
            Timber.i("onCreate: Beginning upload of %d instances!", instancesToSend.length);
        }

        // Get the task if there was a configuration change but the app did not go out of memory.
        // If the app went out of memory, the task is null but the simple state was saved so
        // the task status is reconstructed from that state.
        instanceUploaderTask = (InstanceUploaderTask) getLastCustomNonConfigurationInstance();

        if (instanceUploaderTask == null) {
            // set up dialog and upload task
            showDialog(PROGRESS_DIALOG);
            instanceUploaderTask = new InstanceUploaderTask();

            if (url != null) {
                instanceUploaderTask.setCompleteDestinationUrl(url + OpenRosaConstants.SUBMISSION, getReferrerUri(), true);

                if (deleteInstanceAfterUpload != null) {
                    instanceUploaderTask.setDeleteInstanceAfterSubmission(deleteInstanceAfterUpload);
                }

                String host = Uri.parse(url).getHost();
                if (host != null) {
                    // We do not need to clear the cookies since they are cleared before any request is made and the Credentials provider is used
                    if (password != null && username != null) {
                        instanceUploaderTask.setCustomUsername(username);
                        instanceUploaderTask.setCustomPassword(password);
                    }
                }
            }

            // register this activity with the new uploader task
            instanceUploaderTask.setUploaderListener(this);
            instanceUploaderTask.setRepositories(instancesRepository, formsRepository, settingsProvider);
            instanceUploaderTask.execute(instancesToSend);
        }
    }

    @Override
    protected void onResume() {
        if (instancesToSend != null) {
            Timber.i("onResume: Resuming upload of %d instances!", instancesToSend.length);
        }
        if (instanceUploaderTask != null) {
            instanceUploaderTask.setUploaderListener(this);
        }
        super.onResume();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        isInstanceStateSaved = false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        isInstanceStateSaved = true;
        super.onSaveInstanceState(outState);
        outState.putString(ALERT_MSG, alertMsg);
        outState.putString(AUTH_URI, url);
        outState.putLongArray(TO_SEND, ArrayUtils.toPrimitive(instancesToSend));

        if (url != null) {
            outState.putString(ApplicationConstants.BundleKeys.URL, url);

            if (username != null && password != null) {
                outState.putString(ApplicationConstants.BundleKeys.USERNAME, username);
                outState.putString(ApplicationConstants.BundleKeys.PASSWORD, password);
            }
        }
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return instanceUploaderTask;
    }

    @Override
    protected void onDestroy() {
        if (instanceUploaderTask != null) {
            instanceUploaderTask.setUploaderListener(null);
        }
        super.onDestroy();
    }

    @Override
    public void uploadingComplete(HashMap<String, String> result) {
        Timber.i("uploadingComplete: Processing results (%d) from upload of %d instances!",
                result.size(), instancesToSend.length);

        try {
            dismissDialog(PROGRESS_DIALOG);
        } catch (Exception e) {
            // tried to close a dialog not open. don't care.
        }

        // If the activity is paused or in the process of pausing, don't show the dialog
        if (!isInstanceStateSaved) {
            createUploadInstancesResultDialog(InstanceUploaderUtils.getUploadResultMessage(instancesRepository, this, result));
        } else {
            // Clean up
            finish();
        }
    }

    @Override
    public void progressUpdate(int progress, int total) {
        alertMsg = getString(org.odk.collect.strings.R.string.sending_items, String.valueOf(progress), String.valueOf(total));
        progressDialog.setMessage(alertMsg);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case PROGRESS_DIALOG:
                progressDialog = new DayNightProgressDialog(this);
                DialogInterface.OnClickListener loadingButtonListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                instanceUploaderTask.cancel(true);
                                instanceUploaderTask.setUploaderListener(null);
                                finish();
                            }
                        };
                progressDialog.setTitle(getString(org.odk.collect.strings.R.string.uploading_data));
                progressDialog.setMessage(alertMsg);
                progressDialog.setIndeterminate(true);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.setCancelable(false);
                progressDialog.setButton(getString(org.odk.collect.strings.R.string.cancel), loadingButtonListener);
                return progressDialog;
            case AUTH_DIALOG:
                Timber.i("onCreateDialog(AUTH_DIALOG): for upload of %d instances!",
                        instancesToSend.length);

                AuthDialogUtility authDialogUtility = new AuthDialogUtility();
                if (username != null && password != null && url != null) {
                    authDialogUtility.setCustomUsername(username);
                    authDialogUtility.setCustomPassword(password);
                }

                return authDialogUtility.createDialog(this, this, this.url);
        }

        return null;
    }

    /**
     * Prompts the user for credentials for the server at the given URL. Once credentials are
     * provided, starts a new upload task with just the instances that were not yet reached.
     *
     * messagesByInstanceIdAttempted makes it possible to identify the instances that were part
     * of the latest submission attempt. The database provides generic status which could have come
     * from an unrelated submission attempt.
     */
    @Override
    public void authRequest(Uri url, HashMap<String, String> messagesByInstanceIdAttempted) {
        if (progressDialog.isShowing()) {
            // should always be showing here
            progressDialog.dismiss();
        }

        // Remove sent instances from instances to send
        ArrayList<Long> workingSet = new ArrayList<>();
        Collections.addAll(workingSet, instancesToSend);
        if (messagesByInstanceIdAttempted != null) {
            Set<String> uploadedInstances = messagesByInstanceIdAttempted.keySet();

            for (String uploadedInstance : uploadedInstances) {
                Long removeMe = Long.valueOf(uploadedInstance);
                boolean removed = workingSet.remove(removeMe);
                if (removed) {
                    Timber.i("%d was already attempted, removing from queue before restarting task",
                            removeMe);
                }
            }
        }

        // and reconstruct the pending set of instances to send
        Long[] updatedToSend = new Long[workingSet.size()];
        for (int i = 0; i < workingSet.size(); ++i) {
            updatedToSend[i] = workingSet.get(i);
        }
        instancesToSend = updatedToSend;

        this.url = url.toString();

        /** Once credentials are provided in the dialog, {@link #updatedCredentials()} is called */
        showDialog(AUTH_DIALOG);
    }

    private void createUploadInstancesResultDialog(String message) {
        String dialogTitle = getString(org.odk.collect.strings.R.string.upload_results);
        String buttonTitle = getString(org.odk.collect.strings.R.string.ok);

        SimpleDialog simpleDialog = SimpleDialog.newInstance(dialogTitle, 0, message, buttonTitle, true);
        simpleDialog.show(getSupportFragmentManager(), SimpleDialog.COLLECT_DIALOG_TAG);
    }

    @Override
    public void updatedCredentials() {
        showDialog(PROGRESS_DIALOG);
        instanceUploaderTask = new InstanceUploaderTask();

        // register this activity with the new uploader task
        instanceUploaderTask.setUploaderListener(this);
        // In the case of credentials set via intent extras, the credentials are stored in the
        // global WebCredentialsUtils but the task also needs to know what server to set to
        // TODO: is this really needed here? When would the task not have gotten a server set in
        // init already?
        if (url != null) {
            instanceUploaderTask.setCompleteDestinationUrl(url + OpenRosaConstants.SUBMISSION, getReferrerUri(), false);
        }
        instanceUploaderTask.setRepositories(instancesRepository, formsRepository, settingsProvider);
        instanceUploaderTask.execute(instancesToSend);
    }

    @Override
    public void cancelledUpdatingCredentials() {
        finish();
    }

    private String getReferrerUri() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            Uri referrerUri = getReferrer();
            if (referrerUri != null) {
                return referrerUri.toString();
            }
        }
        return "unknown";
    }
}
