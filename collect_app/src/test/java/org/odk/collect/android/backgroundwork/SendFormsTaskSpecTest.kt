package org.odk.collect.android.backgroundwork

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.odk.collect.android.injection.config.AppDependencyModule
import org.odk.collect.android.injection.config.ProjectDependencyModuleFactory
import org.odk.collect.android.instancemanagement.InstancesDataService
import org.odk.collect.android.notifications.Notifier
import org.odk.collect.android.projects.ProjectsDataService
import org.odk.collect.android.support.CollectHelpers
import org.odk.collect.metadata.PropertyManager
import org.odk.collect.openrosa.http.OpenRosaHttpInterface
import org.odk.collect.testshared.RobolectricHelpers

@RunWith(AndroidJUnit4::class)
class SendFormsTaskSpecTest {

    private val instancesDataService = mock<InstancesDataService>()
    private lateinit var projectId: String

    @Before
    fun setup() {
        CollectHelpers.overrideAppDependencyModule(object : AppDependencyModule() {
            override fun providesInstancesDataService(
                application: Application?,
                projectsDataService: ProjectsDataService?,
                instanceSubmitScheduler: InstanceSubmitScheduler?,
                projectsDependencyProviderFactory: ProjectDependencyModuleFactory?,
                notifier: Notifier?,
                propertyManager: PropertyManager?,
                httpInterface: OpenRosaHttpInterface
            ): InstancesDataService {
                return instancesDataService
            }
        })

        RobolectricHelpers.mountExternalStorage()
        projectId = CollectHelpers.setupDemoProject()
    }

    @Test
    fun `returns false if sending instances fails`() {
        whenever(instancesDataService.sendInstances(projectId)).doReturn(false)

        val inputData = mapOf(TaskData.DATA_PROJECT_ID to projectId)
        val spec = SendFormsTaskSpec()
        val task = spec.getTask(ApplicationProvider.getApplicationContext(), inputData, true)
        assertThat(task.get(), equalTo(false))
    }

    @Test
    fun `returns true if sending instances succeeds`() {
        whenever(instancesDataService.sendInstances(projectId)).doReturn(true)

        val inputData = mapOf(TaskData.DATA_PROJECT_ID to projectId)
        val spec = SendFormsTaskSpec()
        val task = spec.getTask(ApplicationProvider.getApplicationContext(), inputData, true)
        assertThat(task.get(), equalTo(true))
    }
}
