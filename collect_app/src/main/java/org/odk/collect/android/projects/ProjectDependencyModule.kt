package org.odk.collect.android.projects

import org.odk.collect.android.storage.StoragePaths
import org.odk.collect.android.utilities.ChangeLocks
import org.odk.collect.entities.server.EntitySource
import org.odk.collect.entities.storage.EntitiesRepository
import org.odk.collect.forms.FormSource
import org.odk.collect.forms.FormsRepository
import org.odk.collect.forms.instances.InstancesRepository
import org.odk.collect.forms.savepoints.SavepointsRepository
import org.odk.collect.projects.ProjectDependencyFactory
import org.odk.collect.shared.settings.Settings

/**
 * Provides all the basic/building block dependencies needed when performing logic inside a
 * project.
 */
data class ProjectDependencyModule(
    val projectId: String,
    private val settingsFactory: ProjectDependencyFactory<Settings>,
    private val formsRepositoryFactory: ProjectDependencyFactory<FormsRepository>,
    private val instancesRepositoryProvider: ProjectDependencyFactory<InstancesRepository>,
    private val storagePathsFactory: ProjectDependencyFactory<StoragePaths>,
    private val changeLockFactory: ProjectDependencyFactory<ChangeLocks>,
    private val formSourceFactory: ProjectDependencyFactory<FormSource>,
    private val savepointsRepositoryFactory: ProjectDependencyFactory<SavepointsRepository>,
    private val entitiesRepositoryFactory: ProjectDependencyFactory<EntitiesRepository>,
    private val entitySourceFactory: ProjectDependencyFactory<EntitySource>
) {
    val generalSettings by lazy { settingsFactory.create(projectId) }
    val formsRepository by lazy { formsRepositoryFactory.create(projectId) }
    val instancesRepository by lazy { instancesRepositoryProvider.create(projectId) }
    val formSource by lazy { formSourceFactory.create(projectId) }
    val formsLock by lazy { changeLockFactory.create(projectId).formsLock }
    val instancesLock by lazy { changeLockFactory.create(projectId).instancesLock }
    val formsDir by lazy { storagePathsFactory.create(projectId).formsDir }
    val cacheDir by lazy { storagePathsFactory.create(projectId).cacheDir }
    val entitiesRepository by lazy { entitiesRepositoryFactory.create(projectId) }
    val savepointsRepository by lazy { savepointsRepositoryFactory.create(projectId) }
    val rootDir by lazy { storagePathsFactory.create(projectId).rootDir }
    val instancesDir by lazy { storagePathsFactory.create(projectId).instancesDir }
    val entitySource by lazy { entitySourceFactory.create(projectId) }
}
