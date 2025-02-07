package org.odk.collect.android.application.initialization

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.RestrictionsManager
import android.os.Bundle
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
    private val context: Context
) {
    private val restrictionsManager = context.getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager

    private val restrictionsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            applyConfig(restrictionsManager.applicationRestrictions)
        }
    }

    fun initialize() {
        applyConfig(restrictionsManager.applicationRestrictions)

        val restrictionsFilter = IntentFilter(Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED)
        context.registerReceiver(restrictionsReceiver, restrictionsFilter)
    }

    fun applyConfig(managedConfig: Bundle) {
        if (managedConfig.containsKey("device_id") && !managedConfig.getString("device_id").isNullOrBlank()) {
            settingsProvider.getMetaSettings().save(KEY_INSTALL_ID, managedConfig.getString("device_id"))
        }

        if (managedConfig.containsKey("settings_json") && !managedConfig.getString("settings_json").isNullOrBlank()) {
            val settingsJson = managedConfig.getString("settings_json")

            val settingsConnectionMatcher = SettingsConnectionMatcher(projectsRepository, settingsProvider)
            when (val matchingProjectUUID = settingsJson?.let { settingsConnectionMatcher.getProjectWithMatchingConnection(it) }) {
                null -> projectCreator.createNewProject(settingsJson!!)
                else -> settingsImporter.fromJSON(settingsJson, projectsRepository.get(matchingProjectUUID)!!)
            }
        }
    }

    fun unregisterReceiver() {
        context.unregisterReceiver(restrictionsReceiver)
    }
}
