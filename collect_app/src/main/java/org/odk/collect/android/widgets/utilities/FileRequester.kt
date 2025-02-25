package org.odk.collect.android.widgets.utilities

import android.app.Activity
import org.javarosa.form.api.FormEntryPrompt
import org.odk.collect.android.javarosawrapper.FormController
import org.odk.collect.android.utilities.ExternalAppIntentProvider
import org.odk.collect.androidshared.system.IntentLauncher
import org.odk.collect.androidshared.ui.ToastUtils.showLongToast
import java.lang.Error
import java.lang.Exception

class FileRequesterImpl(
    val intentLauncher: IntentLauncher,
    val externalAppIntentProvider: ExternalAppIntentProvider,
    private val formController: FormController
) : FileRequester {

    override fun launch(
        activity: Activity,
        requestCode: Int,
        formEntryPrompt: FormEntryPrompt
    ) {
        try {
            val intent = externalAppIntentProvider.getIntentToRunExternalApp(formController, formEntryPrompt)
            val intentWithoutDefaultCategory =
                externalAppIntentProvider.getIntentToRunExternalAppWithoutDefaultCategory(
                    formController,
                    formEntryPrompt,
                    activity.packageManager
                )

            intentLauncher.launchForResult(
                activity,
                intent,
                requestCode
            ) {
                intentLauncher.launchForResult(
                    activity,
                    intentWithoutDefaultCategory,
                    requestCode
                ) {
                    showLongToast(getErrorMessage(formEntryPrompt, activity))
                }
            }
        } catch (e: Exception) {
            showLongToast(e.message!!)
        } catch (e: Error) {
            showLongToast(e.message!!)
        }
    }

    private fun getErrorMessage(formEntryPrompt: FormEntryPrompt, activity: Activity): String {
        val customErrorMessage = formEntryPrompt.getSpecialFormQuestionText("noAppErrorString")
        return customErrorMessage ?: activity.getString(org.odk.collect.strings.R.string.no_app)
    }
}

interface FileRequester {
    fun launch(
        activity: Activity,
        requestCode: Int,
        formEntryPrompt: FormEntryPrompt
    )
}
