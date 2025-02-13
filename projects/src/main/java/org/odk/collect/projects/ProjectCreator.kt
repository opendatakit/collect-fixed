package org.odk.collect.projects

interface ProjectCreator {
    fun createNewProject(settingsJson: String, switchToTheNewProject: Boolean): ProjectConfigurationResult
}
