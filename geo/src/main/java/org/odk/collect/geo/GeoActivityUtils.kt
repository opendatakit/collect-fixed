package org.odk.collect.geo

import android.Manifest
import android.app.Activity
import org.odk.collect.androidshared.ui.ToastUtils
import org.odk.collect.permissions.ContextCompatPermissionChecker

internal object GeoActivityUtils {

    @JvmStatic
    fun requireLocationPermissions(activity: Activity) {
        val permissionGranted = ContextCompatPermissionChecker(activity).isPermissionGranted(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (!permissionGranted) {
            ToastUtils.showLongToast(org.odk.collect.strings.R.string.not_granted_permission)
            activity.finish()
        }
    }
}
