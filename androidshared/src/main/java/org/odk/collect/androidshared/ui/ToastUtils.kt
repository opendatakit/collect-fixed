package org.odk.collect.androidshared.ui

import android.app.Activity
import android.app.Application
import android.os.Build
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.odk.collect.strings.localization.getLocalizedString

/**
 * Convenience wrapper around Android's [Toast] API.
 */
object ToastUtils {

    @JvmStatic
    var recordToasts = false
    private var recordedToasts = mutableListOf<String>()

    private lateinit var lastToast: Toast
    private lateinit var application: Application

    @JvmStatic
    fun showShortToast(message: String) {
        showToast(message)
    }

    @JvmStatic
    fun showShortToast(messageResource: Int) {
        showToast(
            application.getLocalizedString(messageResource)
        )
    }

    @JvmStatic
    fun showLongToast(message: String) {
        showToast(message, Toast.LENGTH_LONG)
    }

    @JvmStatic
    fun showLongToast(messageResource: Int) {
        showToast(
            application.getLocalizedString(messageResource),
            Toast.LENGTH_LONG
        )
    }

    @JvmStatic
    @Deprecated("Toast position cannot be customized on API 30 and above. A dialog is shown instead for this API levels.")
    fun showShortToastInMiddle(activity: Activity, message: String) {
        showToastInMiddle(activity, message)
    }

    @JvmStatic
    fun popRecordedToasts(): List<String> {
        val copy = recordedToasts.toList()
        recordedToasts.clear()

        return copy
    }

    @JvmStatic
    fun setApplication(application: Application) {
        this.application = application
    }

    private fun showToast(
        message: String,
        duration: Int = Toast.LENGTH_SHORT
    ) {
        hideLastToast()
        lastToast = Toast.makeText(application, message, duration)
        lastToast.show()

        if (recordToasts) {
            recordedToasts.add(message)
        }
    }

    private fun showToastInMiddle(
        activity: Activity,
        message: String,
        duration: Int = Toast.LENGTH_SHORT
    ) {
        if (Build.VERSION.SDK_INT < 30) {
            hideLastToast()
            lastToast = Toast.makeText(activity.applicationContext, message, duration)
            try {
                val group = lastToast.view as ViewGroup?
                val messageTextView = group!!.getChildAt(0) as TextView
                messageTextView.textSize = 21f
                messageTextView.gravity = Gravity.CENTER
            } catch (ignored: Exception) {
                // ignored
            }
            lastToast.setGravity(Gravity.CENTER, 0, 0)
            lastToast.show()

            if (recordToasts) {
                recordedToasts.add(message)
            }
        } else {
            MaterialAlertDialogBuilder(activity)
                .setMessage(message)
                .setPositiveButton(org.odk.collect.strings.R.string.ok, null)
                .create()
                .show()
        }
    }

    private fun hideLastToast() {
        if (ToastUtils::lastToast.isInitialized) {
            lastToast.cancel()
        }
    }
}
