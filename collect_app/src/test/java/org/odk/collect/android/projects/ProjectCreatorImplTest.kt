package org.odk.collect.android.projects

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.odk.collect.projects.Project
import org.odk.collect.projects.ProjectConfigurationResult
import org.odk.collect.projects.ProjectCreator
import org.odk.collect.projects.ProjectsRepository
import org.odk.collect.settings.ODKAppSettingsImporter
import org.odk.collect.settings.SettingsProvider
import org.odk.collect.shared.settings.Settings

class ProjectCreatorImplTest {
    private val json = "{\"general\":{\"server_url\":\"https:\\/\\/my-server.com\",\"username\":\"adam\",\"password\":\"1234\"},\"admin\":{}}"
    private val newProject = Project.New("my-server.com", "M", "#3e9fcc")
    private val savedProject = Project.Saved("1", newProject)

    private var projectsRepository = mock<ProjectsRepository> {
        on { getAll() } doReturn listOf(savedProject)
        on { save(Project.New("", "", "")) } doReturn savedProject
    }

    private var projectsDataService = mock<ProjectsDataService> {
        on { requireCurrentProject() } doReturn savedProject
    }
    private var settingsImporter = mock<ODKAppSettingsImporter> {}

    private var unProtectedSettings = mock<Settings>()
    private var protectedSettings = mock<Settings>()
    private var settingsProvider = mock<SettingsProvider> {
        on { getUnprotectedSettings("1") } doReturn unProtectedSettings
        on { getProtectedSettings("1") } doReturn protectedSettings
    }

    private lateinit var projectCreator: ProjectCreator

    @Before
    fun setup() {
        projectCreator = ProjectCreatorImpl(
            projectsRepository,
            projectsDataService,
            settingsImporter,
            settingsProvider
        )
    }

    @Test
    fun `Importing settings should be triggered with proper params`() {
        projectCreator.createNewProject(json, true)
        verify(settingsImporter).fromJSON(json, savedProject)
    }

    @Test
    fun `When importing settings failed createNewProject() should return 'INVALID_SETTINGS'`() {
        whenever(settingsImporter.fromJSON(json, savedProject)).thenReturn(ProjectConfigurationResult.INVALID_SETTINGS)

        projectCreator.createNewProject(json, true)
        assertThat(projectCreator.createNewProject(json, true), `is`(ProjectConfigurationResult.INVALID_SETTINGS))
    }

    @Test
    fun `When importing settings contain GD protocol createNewProject() should return 'GD_PROJECT'`() {
        whenever(settingsImporter.fromJSON(json, savedProject)).thenReturn(ProjectConfigurationResult.GD_PROJECT)

        projectCreator.createNewProject(json, true)
        assertThat(projectCreator.createNewProject(json, true), `is`(ProjectConfigurationResult.GD_PROJECT))
    }

    @Test
    fun `When importing settings succeeded createNewProject() should return 'SUCCESS'`() {
        whenever(settingsImporter.fromJSON(json, savedProject)).thenReturn(ProjectConfigurationResult.SUCCESS)

        projectCreator.createNewProject(json, true)
        assertThat(projectCreator.createNewProject(json, true), `is`(ProjectConfigurationResult.SUCCESS))
    }

    @Test
    fun `When importing settings failed should created project be deleted`() {
        whenever(settingsImporter.fromJSON(json, savedProject)).thenReturn(ProjectConfigurationResult.INVALID_SETTINGS)

        projectCreator.createNewProject(json, true)
        verify(projectsRepository).delete(savedProject.uuid)
    }

    @Test
    fun `When importing settings contain GD protocol should created project be deleted`() {
        whenever(settingsImporter.fromJSON(json, savedProject)).thenReturn(ProjectConfigurationResult.GD_PROJECT)

        projectCreator.createNewProject(json, true)
        verify(projectsRepository).delete(savedProject.uuid)
    }

    @Test
    fun `When importing settings failed should prefs be cleared`() {
        whenever(settingsImporter.fromJSON(json, savedProject)).thenReturn(ProjectConfigurationResult.INVALID_SETTINGS)

        projectCreator.createNewProject(json, true)

        verify(unProtectedSettings).clear()
        verify(protectedSettings).clear()
    }

    @Test
    fun `New project id should be set when switchToTheNewProject is true`() {
        whenever(settingsImporter.fromJSON(json, savedProject)).thenReturn(ProjectConfigurationResult.SUCCESS)

        projectCreator.createNewProject(json, true)
        verify(projectsDataService).setCurrentProject("1")
    }

    @Test
    fun `New project id should not be set when switchToTheNewProject is false`() {
        whenever(settingsImporter.fromJSON(json, savedProject)).thenReturn(ProjectConfigurationResult.SUCCESS)

        projectCreator.createNewProject(json, false)
        verifyNoInteractions(projectsDataService)
    }
}
