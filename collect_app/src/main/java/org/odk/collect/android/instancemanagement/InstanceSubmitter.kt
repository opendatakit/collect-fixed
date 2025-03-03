package org.odk.collect.android.instancemanagement

import org.odk.collect.analytics.Analytics
import org.odk.collect.android.analytics.AnalyticsEvents
import org.odk.collect.android.application.Collect
import org.odk.collect.android.upload.FormUploadException
import org.odk.collect.android.upload.InstanceServerUploader
import org.odk.collect.android.upload.InstanceUploader
import org.odk.collect.android.utilities.FormsRepositoryProvider
import org.odk.collect.android.utilities.InstanceAutoDeleteChecker
import org.odk.collect.android.utilities.InstancesRepositoryProvider
import org.odk.collect.android.utilities.WebCredentialsUtils
import org.odk.collect.forms.FormsRepository
import org.odk.collect.forms.instances.Instance
import org.odk.collect.forms.instances.InstancesRepository
import org.odk.collect.metadata.PropertyManager
import org.odk.collect.metadata.PropertyManager.Companion.PROPMGR_DEVICE_ID
import org.odk.collect.openrosa.http.OpenRosaHttpInterface
import org.odk.collect.settings.keys.ProjectKeys
import org.odk.collect.shared.settings.Settings
import timber.log.Timber

class InstanceSubmitter(
    private val formsRepository: FormsRepository,
    private val generalSettings: Settings,
    private val propertyManager: PropertyManager,
    private val httpInterface: OpenRosaHttpInterface,
    private val instancesRepository: InstancesRepository
) {

    fun submitInstances(toUpload: List<Instance>): Map<Instance, FormUploadException?> {
        val result = mutableMapOf<Instance, FormUploadException?>()
        val deviceId = propertyManager.getSingularProperty(PROPMGR_DEVICE_ID)

        val uploader = setUpODKUploader()

        for (instance in toUpload.sortedBy { it.lastStatusChangeDate }) {
            try {
                val destinationUrl = uploader.getUrlToSubmitTo(instance, deviceId, null, null)
                uploader.uploadOneSubmission(instance, destinationUrl)
                result[instance] = null

                deleteInstance(instance)
                logUploadedForm(instance)
            } catch (e: FormUploadException) {
                Timber.d(e)
                result[instance] = e
            }
        }
        return result
    }

    private fun setUpODKUploader(): InstanceUploader {
        return InstanceServerUploader(
            httpInterface,
            WebCredentialsUtils(generalSettings),
            generalSettings,
            instancesRepository
        )
    }

    private fun deleteInstance(instance: Instance) {
        // If the submission was successful, delete the instance if either the app-level
        // delete preference is set or the form definition requests auto-deletion.
        // TODO: this could take some time so might be better to do in a separate process,
        // perhaps another worker. It also feels like this could fail and if so should be
        // communicated to the user. Maybe successful delete should also be communicated?
        if (InstanceAutoDeleteChecker.shouldInstanceBeDeleted(formsRepository, generalSettings.getBoolean(ProjectKeys.KEY_DELETE_AFTER_SEND), instance)) {
            InstanceDeleter(
                InstancesRepositoryProvider(Collect.getInstance()).create(),
                FormsRepositoryProvider(Collect.getInstance()).create()
            ).delete(instance.dbId)
        }
    }

    private fun logUploadedForm(instance: Instance) {
        val value = Collect.getFormIdentifierHash(instance.formId, instance.formVersion)

        Analytics.log(AnalyticsEvents.SUBMISSION, "HTTP auto", value)
    }
}
