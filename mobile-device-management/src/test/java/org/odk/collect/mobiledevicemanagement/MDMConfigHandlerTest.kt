package org.odk.collect.mobiledevicemanagement

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
import org.mockito.kotlin.whenever
import org.odk.collect.mobiledevicemanagement.MDMConfigHandler.Companion.DEVICE_ID_KEY
import org.odk.collect.mobiledevicemanagement.MDMConfigHandler.Companion.SETTINGS_JSON_KEY
import org.odk.collect.projects.InMemProjectsRepository
import org.odk.collect.projects.Project
import org.odk.collect.projects.ProjectCreator
import org.odk.collect.projects.ProjectsRepository
import org.odk.collect.projects.SettingsConnectionMatcher
import org.odk.collect.settings.InMemSettingsProvider
import org.odk.collect.settings.ODKAppSettingsImporter
import org.odk.collect.settings.SettingsProvider
import org.odk.collect.settings.keys.MetaKeys.KEY_INSTALL_ID

@RunWith(AndroidJUnit4::class)
class MDMConfigHandlerTest {
    private lateinit var settingsProvider: SettingsProvider
    private lateinit var projectsRepository: ProjectsRepository
    private lateinit var projectCreator: ProjectCreator
    private lateinit var settingsImporter: ODKAppSettingsImporter
    private lateinit var settingsConnectionMatcher: SettingsConnectionMatcher
    private lateinit var mdmConfigHandler: MDMConfigHandler

    @Before
    fun setup() {
        settingsProvider = InMemSettingsProvider()
        projectsRepository = InMemProjectsRepository()
        projectCreator = mock<ProjectCreator>()
        settingsImporter = mock<ODKAppSettingsImporter>()
        settingsConnectionMatcher = mock<SettingsConnectionMatcher>()

        mdmConfigHandler = MDMConfigHandler(
            settingsProvider,
            projectsRepository,
            projectCreator,
            settingsImporter,
            settingsConnectionMatcher
        )
    }

    @Test
    fun `deviceId is ignored if it is null`() {
        settingsProvider.getMetaSettings().save(KEY_INSTALL_ID, "foo")

        val managedConfig = Bundle().apply {
            putString(DEVICE_ID_KEY, null)
        }
        mdmConfigHandler.applyConfig(managedConfig)

        assertThat(settingsProvider.getMetaSettings().getString(KEY_INSTALL_ID), equalTo("foo"))
    }

    @Test
    fun `deviceId is ignored if it is empty`() {
        settingsProvider.getMetaSettings().save(KEY_INSTALL_ID, "foo")

        val managedConfig = Bundle().apply {
            putString(DEVICE_ID_KEY, "")
        }
        mdmConfigHandler.applyConfig(managedConfig)

        assertThat(settingsProvider.getMetaSettings().getString(KEY_INSTALL_ID), equalTo("foo"))
    }

    @Test
    fun `deviceId is ignored if it is blank`() {
        settingsProvider.getMetaSettings().save(KEY_INSTALL_ID, "foo")

        val managedConfig = Bundle().apply {
            putString(DEVICE_ID_KEY, " ")
        }
        mdmConfigHandler.applyConfig(managedConfig)

        assertThat(settingsProvider.getMetaSettings().getString(KEY_INSTALL_ID), equalTo("foo"))
    }

    @Test
    fun `deviceId is saved if it is has value`() {
        settingsProvider.getMetaSettings().save(KEY_INSTALL_ID, "foo")

        val managedConfig = Bundle().apply {
            putString(DEVICE_ID_KEY, "bar")
        }
        mdmConfigHandler.applyConfig(managedConfig)

        assertThat(settingsProvider.getMetaSettings().getString(KEY_INSTALL_ID), equalTo("bar"))
    }

    @Test
    fun `settingsJson is ignored if it is null`() {
        val managedConfig = Bundle().apply {
            putString(SETTINGS_JSON_KEY, null)
        }
        mdmConfigHandler.applyConfig(managedConfig)

        verifyNoInteractions(projectCreator)
        verifyNoInteractions(settingsImporter)
    }

    @Test
    fun `settingsJson is ignored if it is empty`() {
        val managedConfig = Bundle().apply {
            putString(SETTINGS_JSON_KEY, "")
        }
        mdmConfigHandler.applyConfig(managedConfig)

        verifyNoInteractions(projectCreator)
        verifyNoInteractions(settingsImporter)
    }

    @Test
    fun `settingsJson is ignored if it is blank`() {
        val managedConfig = Bundle().apply {
            putString(SETTINGS_JSON_KEY, " ")
        }
        mdmConfigHandler.applyConfig(managedConfig)

        verifyNoInteractions(projectCreator)
        verifyNoInteractions(settingsImporter)
    }

    @Test
    fun `new project is created and switched to if there are no saved projects yet`() {
        val settingsJson = "{ \"general\": {}, \"admin\": {} }"
        val managedConfig = Bundle().apply {
            putString(SETTINGS_JSON_KEY, settingsJson)
        }
        whenever(settingsConnectionMatcher.getProjectWithMatchingConnection(settingsJson)).thenReturn(null)

        mdmConfigHandler.applyConfig(managedConfig)

        verify(projectCreator).createNewProject(settingsJson, true)
    }

    @Test
    fun `new project is created but not switched to if there are projects saved but none of them contains a URL and username combination that matches an existing project`() {
        val project = Project.Saved("1", "project", "Q", "#000000")
        projectsRepository.save(project)

        val settingsJson = "{ \"general\": {}, \"admin\": {} }"
        val managedConfig = Bundle().apply {
            putString(SETTINGS_JSON_KEY, settingsJson)
        }
        whenever(settingsConnectionMatcher.getProjectWithMatchingConnection(settingsJson)).thenReturn(null)

        mdmConfigHandler.applyConfig(managedConfig)

        verify(projectCreator).createNewProject(settingsJson, false)
    }

    @Test
    fun `existing project is updated when settingsJson contains a URL and username combination that matches an existing project`() {
        val project = Project.Saved("1", "project", "Q", "#000000")
        projectsRepository.save(project)

        val settingsJson = "{ \"general\": {}, \"admin\": {} }"
        val managedConfig = Bundle().apply {
            putString(SETTINGS_JSON_KEY, settingsJson)
        }
        whenever(settingsConnectionMatcher.getProjectWithMatchingConnection(settingsJson)).thenReturn("1")

        mdmConfigHandler.applyConfig(managedConfig)

        verifyNoInteractions(projectCreator)
        verify(settingsImporter).fromJSON(settingsJson, project)
    }

    @Test
    fun `unsupported settings are ignored`() {
        val managedConfig = Bundle().apply {
            putString("foo", "bar")
        }
        mdmConfigHandler.applyConfig(managedConfig)

        assertThat(settingsProvider.getMetaSettings().contains("foo"), equalTo(false))
    }
}
