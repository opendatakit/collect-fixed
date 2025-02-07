package org.odk.collect.android.application.initialization

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.RestrictionsManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import org.odk.collect.android.projects.ProjectCreator
import org.odk.collect.android.projects.SettingsConnectionMatcher
import org.odk.collect.projects.ProjectsRepository
import org.odk.collect.settings.ODKAppSettingsImporter
import org.odk.collect.settings.SettingsProvider
import org.odk.collect.settings.keys.MetaKeys.KEY_INSTALL_ID

/**
 * Manages configuration changes from a mobile device management system.
 *
 * See android.content.APP_RESTRICTIONS in AndroidManifest for supported configuration keys.
 */
class ManagedConfigManager(
    private val settingsProvider: SettingsProvider,
    private val projectsRepository: ProjectsRepository,
    private val projectCreator: ProjectCreator,
    private val settingsImporter: ODKAppSettingsImporter,
    private val restrictionsManager: RestrictionsManager,
    private val context: Context
) : DefaultLifecycleObserver {

    private val restrictionsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            applyConfig()
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        applyConfig()

        val restrictionsFilter = IntentFilter(Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED)
        context.registerReceiver(restrictionsReceiver, restrictionsFilter)
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        context.unregisterReceiver(restrictionsReceiver)
    }

    private fun applyConfig() {
        val managedConfig = restrictionsManager.applicationRestrictions

        if (managedConfig.containsKey(DEVICE_ID_KEY) && !managedConfig.getString(DEVICE_ID_KEY).isNullOrBlank()) {
            settingsProvider.getMetaSettings().save(KEY_INSTALL_ID, managedConfig.getString(DEVICE_ID_KEY))
        }

        if (managedConfig.containsKey(SETTINGS_JSON_KEY) && !managedConfig.getString(SETTINGS_JSON_KEY).isNullOrBlank()) {
            val settingsJson = managedConfig.getString(SETTINGS_JSON_KEY)

            val settingsConnectionMatcher = SettingsConnectionMatcher(projectsRepository, settingsProvider)
            when (val matchingProjectUUID = settingsJson?.let { settingsConnectionMatcher.getProjectWithMatchingConnection(it) }) {
                null -> projectCreator.createNewProject(settingsJson!!)
                else -> settingsImporter.fromJSON(settingsJson, projectsRepository.get(matchingProjectUUID)!!)
            }
        }
    }

    companion object {
        private const val DEVICE_ID_KEY = "device_id"
        private const val SETTINGS_JSON_KEY = "settings_json"
    }
}
