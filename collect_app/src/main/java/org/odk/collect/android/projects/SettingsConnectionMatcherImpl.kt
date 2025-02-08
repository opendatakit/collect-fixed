package org.odk.collect.android.projects

import org.json.JSONException
import org.json.JSONObject
import org.odk.collect.android.preferences.Defaults
import org.odk.collect.projects.ProjectsRepository
import org.odk.collect.projects.SettingsConnectionMatcher
import org.odk.collect.settings.SettingsProvider
import org.odk.collect.settings.keys.AppConfigurationKeys
import org.odk.collect.settings.keys.ProjectKeys

class SettingsConnectionMatcherImpl(
    private val projectsRepository: ProjectsRepository,
    private val settingsProvider: SettingsProvider
) : SettingsConnectionMatcher {

    override fun getProjectWithMatchingConnection(settingsJson: String): String? {
        try {
            val jsonObject = JSONObject(settingsJson)
            val jsonSettings = jsonObject.getJSONObject(AppConfigurationKeys.GENERAL)

            val jsonUrl = try { jsonSettings.get(ProjectKeys.KEY_SERVER_URL) } catch (e: JSONException) { Defaults.unprotected[ProjectKeys.KEY_SERVER_URL]!! }
            val jsonUsername = try { jsonSettings.get(ProjectKeys.KEY_USERNAME) } catch (e: JSONException) { "" }

            projectsRepository.getAll().forEach {
                val projectSettings = settingsProvider.getUnprotectedSettings(it.uuid)
                val projectUrl = projectSettings.getString(ProjectKeys.KEY_SERVER_URL)
                val projectUsername = projectSettings.getString(ProjectKeys.KEY_USERNAME)

                if (jsonUrl.equals(projectUrl) && jsonUsername.equals(projectUsername)) {
                    return it.uuid
                }
            }
        } catch (e: JSONException) {
            return null
        }
        return null
    }
}
