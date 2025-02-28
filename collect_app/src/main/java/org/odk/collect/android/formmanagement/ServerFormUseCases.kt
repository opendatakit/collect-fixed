package org.odk.collect.android.formmanagement

import org.odk.collect.analytics.Analytics
import org.odk.collect.android.formmanagement.download.FormDownloadException
import org.odk.collect.android.formmanagement.download.FormDownloader
import org.odk.collect.android.utilities.FileUtils
import org.odk.collect.async.OngoingWorkListener
import org.odk.collect.entities.LocalEntityUseCases
import org.odk.collect.entities.server.EntitySource
import org.odk.collect.entities.storage.EntitiesRepository
import org.odk.collect.forms.Form
import org.odk.collect.forms.FormSource
import org.odk.collect.forms.FormSourceException
import org.odk.collect.forms.FormsRepository
import org.odk.collect.forms.MediaFile
import org.odk.collect.shared.strings.Md5.getMd5Hash
import java.io.File
import java.io.IOException

object ServerFormUseCases {

    fun downloadForms(
        forms: List<ServerFormDetails>,
        formDownloader: FormDownloader,
        progressReporter: ((Int, Int) -> Unit)? = null,
        isCancelled: (() -> Boolean)? = null,
    ): Map<ServerFormDetails, FormDownloadException?> {
        val results = mutableMapOf<ServerFormDetails, FormDownloadException?>()
        for (index in forms.indices) {
            val form = forms[index]

            try {
                formDownloader.downloadForm(
                    form,
                    object : FormDownloader.ProgressReporter {
                        override fun onDownloadingMediaFile(count: Int) {
                            progressReporter?.invoke(index, count)
                        }
                    },
                    { isCancelled?.invoke() ?: false }
                )

                results[form] = null
            } catch (e: FormDownloadException.DownloadingInterrupted) {
                break
            } catch (e: FormDownloadException) {
                results[form] = e
            }
        }

        return results
    }

    @JvmStatic
    fun copySavedFileFromPreviousFormVersionIfExists(formsRepository: FormsRepository, formId: String, mediaDirPath: String) {
        val lastSavedFile: File? = formsRepository
            .getAllByFormId(formId)
            .maxByOrNull { form -> form.date }
            ?.let {
                File(it.formMediaPath, FileUtils.LAST_SAVED_FILENAME)
            }

        if (lastSavedFile != null && lastSavedFile.exists()) {
            File(mediaDirPath).mkdir()
            FileUtils.copyFile(lastSavedFile, File(mediaDirPath, FileUtils.LAST_SAVED_FILENAME))
        }
    }

    @JvmStatic
    @Throws(IOException::class, FormSourceException::class, InterruptedException::class)
    fun downloadMediaFiles(
        formToDownload: ServerFormDetails,
        formSource: FormSource,
        formsRepository: FormsRepository,
        tempMediaPath: String,
        tempDir: File,
        entitiesRepository: EntitiesRepository,
        entitySource: EntitySource,
        stateListener: OngoingWorkListener
    ): MediaFilesDownloadResult {
        var newAttachmentsDownloaded = false
        var entitiesDownloaded = false

        val tempMediaDir = File(tempMediaPath).also { it.mkdir() }

        formToDownload.manifest!!.mediaFiles.forEachIndexed { i, mediaFile ->
            stateListener.progressUpdate(i + 1)

            val tempMediaFile = File(tempMediaDir, mediaFile.filename)

            val existingFile = searchForExistingMediaFile(formsRepository, formToDownload, mediaFile)
            existingFile.also {
                if (it != null) {
                    if (it.getMd5Hash().contentEquals(mediaFile.hash)) {
                        FileUtils.copyFile(it, tempMediaFile)
                    } else {
                        val existingFileHash = it.getMd5Hash()
                        val file = formSource.fetchMediaFile(mediaFile.downloadUrl)
                        FileUtils.interuptablyWriteFile(file, tempMediaFile, tempDir, stateListener)

                        if (!tempMediaFile.getMd5Hash().contentEquals(existingFileHash)) {
                            newAttachmentsDownloaded = true
                        }
                    }
                } else {
                    val file = formSource.fetchMediaFile(mediaFile.downloadUrl)
                    FileUtils.interuptablyWriteFile(file, tempMediaFile, tempDir, stateListener)
                    newAttachmentsDownloaded = true
                }
            }

            if (mediaFile.isEntityList) {
                /**
                 * We wrap and then rethrow exceptions that happen here to make them easier to
                 * track in Crashlytics. This can be removed in the next release once any
                 * unexpected exceptions "in the wild" are identified.
                 */
                try {
                    val entityListName = getEntityListFromFileName(mediaFile)
                    LocalEntityUseCases.updateLocalEntitiesFromServer(
                        entityListName,
                        tempMediaFile,
                        entitiesRepository,
                        entitySource,
                        mediaFile.integrityUrl
                    )
                    entitiesDownloaded = true
                } catch (t: Throwable) {
                    throw EntityListUpdateException(t)
                }
            } else {
                /**
                 * Track CSVs that have names that clash with entity lists in the project. If
                 * these CSVs are being used as part of a `select_one_from_file` question, the
                 * instance ID will be the file name with the extension removed.
                 */
                val isCsv = mediaFile.filename.endsWith(".csv")
                val mostLikelyInstanceId = getEntityListFromFileName(mediaFile)
                if (isCsv && entitiesRepository.getLists().contains(mostLikelyInstanceId)) {
                    Analytics.setUserProperty("HasEntityListCollision", "true")
                }
            }
        }

        return MediaFilesDownloadResult(newAttachmentsDownloaded, entitiesDownloaded)
    }

    private fun getEntityListFromFileName(mediaFile: MediaFile) =
        mediaFile.filename.substringBefore(".csv")

    private fun searchForExistingMediaFile(
        formsRepository: FormsRepository,
        formToDownload: ServerFormDetails,
        mediaFile: MediaFile
    ): File? {
        val allFormVersions = formsRepository.getAllByFormId(formToDownload.formId)
        return allFormVersions.sortedByDescending {
            it.date
        }.map { form: Form ->
            File(form.formMediaPath, mediaFile.filename)
        }.firstOrNull { file: File ->
            file.exists()
        }
    }
}

class EntityListUpdateException(cause: Throwable) : Exception(cause)

data class MediaFilesDownloadResult(
    val newAttachmentsDownloaded: Boolean,
    val entitiesDownloaded: Boolean
)
