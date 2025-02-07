package org.odk.collect.android.mdm

import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.odk.collect.android.mdm.ManagedConfigSaver.Companion.DEVICE_ID_KEY
import org.odk.collect.android.mdm.ManagedConfigSaver.Companion.SETTINGS_JSON_KEY
import org.odk.collect.android.projects.ProjectCreator
import org.odk.collect.projects.InMemProjectsRepository
import org.odk.collect.projects.Project
import org.odk.collect.projects.ProjectsRepository
import org.odk.collect.settings.InMemSettingsProvider
import org.odk.collect.settings.ODKAppSettingsImporter
import org.odk.collect.settings.SettingsProvider
import org.odk.collect.settings.keys.MetaKeys.KEY_INSTALL_ID
import org.odk.collect.settings.keys.ProjectKeys.KEY_SERVER_URL
import org.odk.collect.settings.keys.ProjectKeys.KEY_USERNAME

@RunWith(AndroidJUnit4::class)
class ManagedConfigSaverTest {
    private lateinit var settingsProvider: SettingsProvider
    private lateinit var projectsRepository: ProjectsRepository
    private lateinit var projectCreator: ProjectCreator
    private lateinit var settingsImporter: ODKAppSettingsImporter
    private lateinit var managedConfigSaver: ManagedConfigSaver

    @Before
    fun setup() {
        settingsProvider = InMemSettingsProvider()
        projectsRepository = InMemProjectsRepository()
        projectCreator = mock<ProjectCreator>()
        settingsImporter = mock<ODKAppSettingsImporter>()

        managedConfigSaver = ManagedConfigSaver(settingsProvider, projectsRepository, projectCreator, settingsImporter)
    }

    @Test
    fun `deviceId is ignored if it is null`() {
        settingsProvider.getMetaSettings().save(KEY_INSTALL_ID, "foo")

        val managedConfig = Bundle().apply {
            putString(DEVICE_ID_KEY, null)
        }
        managedConfigSaver.applyConfig(managedConfig)

        assertThat(settingsProvider.getMetaSettings().getString(KEY_INSTALL_ID), equalTo("foo"))
    }

    @Test
    fun `deviceId is ignored if it is empty`() {
        settingsProvider.getMetaSettings().save(KEY_INSTALL_ID, "foo")

        val managedConfig = Bundle().apply {
            putString(DEVICE_ID_KEY, "")
        }
        managedConfigSaver.applyConfig(managedConfig)

        assertThat(settingsProvider.getMetaSettings().getString(KEY_INSTALL_ID), equalTo("foo"))
    }

    @Test
    fun `deviceId is ignored if it is blank`() {
        settingsProvider.getMetaSettings().save(KEY_INSTALL_ID, "foo")

        val managedConfig = Bundle().apply {
            putString(DEVICE_ID_KEY, " ")
        }
        managedConfigSaver.applyConfig(managedConfig)

        assertThat(settingsProvider.getMetaSettings().getString(KEY_INSTALL_ID), equalTo("foo"))
    }

    @Test
    fun `deviceId is saved if it is has value`() {
        settingsProvider.getMetaSettings().save(KEY_INSTALL_ID, "foo")

        val managedConfig = Bundle().apply {
            putString(DEVICE_ID_KEY, "bar")
        }
        managedConfigSaver.applyConfig(managedConfig)

        assertThat(settingsProvider.getMetaSettings().getString(KEY_INSTALL_ID), equalTo("bar"))
    }

    @Test
    fun `settingsJson is ignored if it is null`() {
        val managedConfig = Bundle().apply {
            putString(SETTINGS_JSON_KEY, null)
        }
        managedConfigSaver.applyConfig(managedConfig)

        verifyNoInteractions(projectCreator)
        verifyNoInteractions(settingsImporter)
    }

    @Test
    fun `settingsJson is ignored if it is empty`() {
        val managedConfig = Bundle().apply {
            putString(SETTINGS_JSON_KEY, "")
        }
        managedConfigSaver.applyConfig(managedConfig)

        verifyNoInteractions(projectCreator)
        verifyNoInteractions(settingsImporter)
    }

    @Test
    fun `settingsJson is ignored if it is blank`() {
        val managedConfig = Bundle().apply {
            putString(SETTINGS_JSON_KEY, " ")
        }
        managedConfigSaver.applyConfig(managedConfig)

        verifyNoInteractions(projectCreator)
        verifyNoInteractions(settingsImporter)
    }

    @Test
    fun `new project is created when settingsJson contains a URL and username combination that does not match an existing project`() {
        val project = Project.Saved("1", "project", "Q", "#000000")
        projectsRepository.save(project)
        settingsProvider.getUnprotectedSettings("1").save(KEY_SERVER_URL, "https://example.com")
        settingsProvider.getUnprotectedSettings("1").save(KEY_USERNAME, "foo")

        val settingsJson = "{ \"general\": { \"server_url\": \"https://example.com\", \"username\": \"bar\" }, \"admin\": {} }"
        val managedConfig = Bundle().apply {
            putString(SETTINGS_JSON_KEY, settingsJson)
        }
        managedConfigSaver.applyConfig(managedConfig)

        verify(projectCreator).createNewProject(settingsJson)
    }

    @Test
    fun `existing project is updated when settingsJson contains a URL and username combination that matches an existing project`() {
        val project = Project.Saved("1", "project", "Q", "#000000")
        projectsRepository.save(project)
        settingsProvider.getUnprotectedSettings("1").save(KEY_SERVER_URL, "https://example.com")
        settingsProvider.getUnprotectedSettings("1").save(KEY_USERNAME, "foo")

        val settingsJson = "{ \"general\": { \"server_url\": \"https://example.com\", \"username\": \"foo\" }, \"admin\": {} }"
        val managedConfig = Bundle().apply {
            putString(SETTINGS_JSON_KEY, settingsJson)
        }
        managedConfigSaver.applyConfig(managedConfig)

        verifyNoInteractions(projectCreator)
        verify(settingsImporter).fromJSON(settingsJson, project)
    }

    @Test
    fun `unsupported settings are ignored`() {
        val managedConfig = Bundle().apply {
            putString("foo", "bar")
        }
        managedConfigSaver.applyConfig(managedConfig)

        assertThat(settingsProvider.getMetaSettings().contains("foo"), equalTo(false))
    }
}
