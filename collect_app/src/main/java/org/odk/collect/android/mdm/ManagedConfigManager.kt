package org.odk.collect.android.mdm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.RestrictionsManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Manages configuration changes from a mobile device management system.
 *
 * See android.content.APP_RESTRICTIONS in AndroidManifest for supported configuration keys.
 */
class ManagedConfigManager(
    private val managedConfigSaver: ManagedConfigSaver,
    private val restrictionsManager: RestrictionsManager,
    private val context: Context
) : DefaultLifecycleObserver {

    private val restrictionsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            managedConfigSaver.applyConfig(restrictionsManager.applicationRestrictions)
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        managedConfigSaver.applyConfig(restrictionsManager.applicationRestrictions)

        val restrictionsFilter = IntentFilter(Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED)
        context.registerReceiver(restrictionsReceiver, restrictionsFilter)
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        context.unregisterReceiver(restrictionsReceiver)
    }
}
