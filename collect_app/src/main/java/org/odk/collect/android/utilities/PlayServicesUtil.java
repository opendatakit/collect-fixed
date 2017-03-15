package org.odk.collect.android.utilities;

import android.app.Activity;
import android.content.Context;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

/**
 * Created by Divya on 3/2/2017.
 */

public class PlayServicesUtil {

    public static int PLAY_SERVICE_ERROR_REQUEST_CODE = 1001;

    private static GoogleApiAvailability googleApiAvailability;
    private static int resultCode;

    /**
     * Check that Google Play services APK is installed and up to date.
     *
     * @return true if Google Play Services is available and up to
     * date on this device; false otherwise.
     */
    public static boolean isGooglePlayServicesAvailable(Context context) {
        googleApiAvailability = GoogleApiAvailability.getInstance();
        resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context);
        return resultCode == ConnectionResult.SUCCESS;
    }

    public static void requestPlayServicesErrorDialog(Context context) {
        if (googleApiAvailability.isUserResolvableError(resultCode)) {
            googleApiAvailability.getErrorDialog((Activity) context,
                    resultCode, PLAY_SERVICE_ERROR_REQUEST_CODE)
                    .show();
        } else {
            ((Activity) context).finish();
        }
    }


}
