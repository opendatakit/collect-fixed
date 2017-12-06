/*
 * Copyright (C) 2017 Shobhit
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

package org.odk.collect.android.google;


import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.DriveScopes;

import org.odk.collect.android.R;
import org.odk.collect.android.preferences.GeneralSharedPreferences;
import org.odk.collect.android.preferences.PreferenceKeys;
import org.odk.collect.android.utilities.ToastUtils;

import java.util.Collections;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;
import timber.log.Timber;

public class GoogleAccountsManager implements EasyPermissions.PermissionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    public static final int REQUEST_ACCOUNT_PICKER = 1000;
    private static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1002;
    private static final int RESOLVE_CONNECTION_REQUEST_CODE = 5555;

    @Nullable
    private Fragment fragment;
    @Nullable
    private Activity activity;
    @Nullable
    private GoogleAccountSelectionListener listener;

    private Context context;
    private GoogleAccountCredential credential;

    private DriveHelper driveHelper;
    private SheetsHelper sheetsHelper;

    private HttpTransport transport;
    private JsonFactory jsonFactory;

    private boolean autoChooseAccount = true;
    private GeneralSharedPreferences preferences;
    private Intent intentChooseAccount;

    public GoogleAccountsManager(@NonNull Activity activity) {
        this.activity = activity;
        initCredential(activity);
    }

    public GoogleAccountsManager(@NonNull Fragment fragment) {
        this.fragment = fragment;
        initCredential(fragment.getActivity());
    }

    public GoogleAccountsManager(@NonNull Context context) {
        initCredential(context);
    }

    /**
     * This constructor should be used only for testing purposes
     */
    public GoogleAccountsManager(@NonNull GoogleAccountCredential credential,
                                 @NonNull GeneralSharedPreferences preferences,
                                 @NonNull Intent intentChooseAccount) {
        this.credential = credential;
        this.preferences = preferences;
        this.intentChooseAccount = intentChooseAccount;
    }

    private void initCredential(Context context) {
        this.context = context;

        transport = AndroidHttp.newCompatibleTransport();
        jsonFactory = JacksonFactory.getDefaultInstance();
        preferences = GeneralSharedPreferences.getInstance();

        credential = GoogleAccountCredential
                .usingOAuth2(context, Collections.singletonList(DriveScopes.DRIVE))
                .setBackOff(new ExponentialBackOff());

        intentChooseAccount = credential.newChooseAccountIntent();
    }

    public void setSelectedAccountName(String accountName) {
        if (accountName != null) {
            preferences.save(PreferenceKeys.KEY_SELECTED_GOOGLE_ACCOUNT, accountName);
            selectAccount(accountName);
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        if (listener != null) {
            listener.onGoogleAccountSelected(credential.getSelectedAccountName());
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        ToastUtils.showShortToast("Permission denied");
    }

    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    public void chooseAccount() {
        if (hasPermissions()) {
            String accountName = getSelectedAccount();
            if (autoChooseAccount && !accountName.isEmpty()) {
                selectAccount(accountName);
            } else {
                showAccountPickerDialog();
            }
        }
    }

    public boolean hasPermissions() {
        boolean hasPermissions = checkAccountPermission();
        if (!hasPermissions) {
            requestAccountPermission();
        }
        return hasPermissions;
    }

    public void requestAccountPermission() {
        EasyPermissions.requestPermissions(
                context,
                context.getString(R.string.request_permissions_google_account),
                REQUEST_PERMISSION_GET_ACCOUNTS, Manifest.permission.GET_ACCOUNTS);
    }

    public boolean checkAccountPermission() {
        return EasyPermissions.hasPermissions(context, Manifest.permission.GET_ACCOUNTS);
    }

    public String getSelectedAccount() {
        return (String) preferences.get(PreferenceKeys.KEY_SELECTED_GOOGLE_ACCOUNT);
    }

    public void showAccountPickerDialog() {
        intentChooseAccount.putExtra("overrideTheme", 1);
        intentChooseAccount.putExtra("overrideCustomTheme", 0);

        if (fragment != null) {
            fragment.startActivityForResult(intentChooseAccount, REQUEST_ACCOUNT_PICKER);
        } else if (activity != null) {
            activity.startActivityForResult(intentChooseAccount, REQUEST_ACCOUNT_PICKER);
        }
    }

    public void selectAccount(String accountName) {
        credential.setSelectedAccountName(accountName);
        if (listener != null) {
            listener.onGoogleAccountSelected(accountName);
        }
    }

    public boolean isGoogleAccountSelected() {
        return credential.getSelectedAccountName() != null;
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(activity, RESOLVE_CONNECTION_REQUEST_CODE);
            } catch (IntentSender.SendIntentException e) {
                Timber.e(e);
            }
        } else {
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), activity, 0).show();
        }
    }

    public DriveHelper getDriveHelper() {
        if (driveHelper == null) {
            driveHelper = new DriveHelper(credential, transport, jsonFactory);
        }
        return driveHelper;
    }

    public SheetsHelper getSheetsHelper() {
        if (sheetsHelper == null) {
            sheetsHelper = new SheetsHelper(credential, transport, jsonFactory);
        }
        return sheetsHelper;
    }

    @Nullable
    public Activity getActivity() {
        return activity;
    }

    @NonNull
    public Context getContext() {
        return context;
    }

    public GoogleAccountCredential getCredential() {
        return credential;
    }

    public void disableAutoChooseAccount() {
        autoChooseAccount = false;
    }

    public void setListener(@Nullable GoogleAccountSelectionListener listener) {
        this.listener = listener;
    }

    public interface GoogleAccountSelectionListener {
        void onGoogleAccountSelected(String accountName);
    }
}
