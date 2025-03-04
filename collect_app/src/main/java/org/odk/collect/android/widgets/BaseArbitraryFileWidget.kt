package org.odk.collect.android.widgets

import android.content.Context
import org.javarosa.core.model.data.IAnswerData
import org.javarosa.core.model.data.StringData
import org.odk.collect.android.formentry.questions.QuestionDetails
import org.odk.collect.android.utilities.QuestionMediaManager
import org.odk.collect.android.widgets.interfaces.FileWidget
import org.odk.collect.android.widgets.interfaces.WidgetDataReceiver
import org.odk.collect.android.widgets.utilities.WaitingForDataRegistry
import timber.log.Timber
import java.io.File

abstract class BaseArbitraryFileWidget(
    context: Context,
    questionDetails: QuestionDetails,
    private val questionMediaManager: QuestionMediaManager,
    protected val waitingForDataRegistry: WaitingForDataRegistry
) : QuestionWidget(context, questionDetails), FileWidget, WidgetDataReceiver {
    var answerFile: File? = null

    override fun getAnswer(): IAnswerData? {
        return if (answerFile != null) StringData(answerFile!!.name) else null
    }

    override fun deleteFile() {
        questionMediaManager.deleteAnswerFile(
            formEntryPrompt.index.toString(),
            answerFile!!.absolutePath
        )
        answerFile = null
        hideAnswerText()
    }

    override fun setData(answer: Any?) {
        if (answerFile != null) {
            deleteFile()
        }

        if (answer is File) {
            answerFile = answer
            if (answerFile!!.exists()) {
                questionMediaManager.replaceAnswerFile(
                    formEntryPrompt.index.toString(),
                    answerFile!!.absolutePath
                )
                showAnswerText()
                widgetValueChanged()
            } else {
                answerFile = null
                Timber.e(Error("Inserting Arbitrary file FAILED"))
            }
        } else if (answer != null) {
            Timber.e(Error("FileWidget's setBinaryData must receive a File but received: " + answer.javaClass))
        }
    }

    protected fun setupAnswerFile(fileName: String?) {
        if (!fileName.isNullOrEmpty()) {
            answerFile = questionMediaManager.getAnswerFile(fileName)
        }
    }

    protected abstract fun showAnswerText()

    protected abstract fun hideAnswerText()
}
