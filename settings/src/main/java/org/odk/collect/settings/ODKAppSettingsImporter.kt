package org.odk.collect.settings

import org.json.JSONObject
import org.odk.collect.projects.Project
import org.odk.collect.projects.ProjectConfigurationResult
import org.odk.collect.projects.ProjectsRepository
import org.odk.collect.settings.importing.ProjectDetailsCreatorImpl
import org.odk.collect.settings.importing.SettingsChangeHandler
import org.odk.collect.settings.importing.SettingsImporter
import org.odk.collect.settings.validation.JsonSchemaSettingsValidator

class ODKAppSettingsImporter(
    projectsRepository: ProjectsRepository,
    settingsProvider: SettingsProvider,
    generalDefaults: Map<String, Any>,
    adminDefaults: Map<String, Any>,
    projectColors: List<String>,
    settingsChangedHandler: SettingsChangeHandler,
    private val deviceUnsupportedSettings: JSONObject
) {

    private val settingsImporter = SettingsImporter(
        settingsProvider,
        ODKAppSettingsMigrator(settingsProvider.getMetaSettings()),
        JsonSchemaSettingsValidator { javaClass.getResourceAsStream("/client-settings.schema.json")!! },
        generalDefaults,
        adminDefaults,
        settingsChangedHandler,
        projectsRepository,
        ProjectDetailsCreatorImpl(projectColors, generalDefaults)
    )

    fun fromJSON(json: String, project: Project.Saved): ProjectConfigurationResult {
        return try {
            settingsImporter.fromJSON(json, project, deviceUnsupportedSettings)
        } catch (e: Throwable) {
            ProjectConfigurationResult.INVALID_SETTINGS
        }
    }
}
