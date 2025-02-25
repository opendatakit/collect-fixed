package org.odk.collect.android.preferences

import org.odk.collect.android.BuildConfig
import org.odk.collect.android.version.VersionInformation
import org.odk.collect.shared.settings.Settings

object SettingsExt {
    fun Settings.getExperimentalOptIn(key: String): Boolean {
        val versionInformation = VersionInformation { BuildConfig.VERSION_NAME }

        return if (versionInformation.isRelease) {
            this.getBoolean(key)
        } else {
            false
        }
    }
}
