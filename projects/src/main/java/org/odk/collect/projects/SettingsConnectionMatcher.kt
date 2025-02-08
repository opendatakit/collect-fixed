package org.odk.collect.projects

interface SettingsConnectionMatcher {
    fun getProjectWithMatchingConnection(settingsJson: String): String?
}
