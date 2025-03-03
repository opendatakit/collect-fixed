package org.odk.collect.android.widgets

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.View
import org.javarosa.form.api.FormEntryPrompt
import org.odk.collect.android.databinding.ArbitraryFileWidgetBinding
import org.odk.collect.android.formentry.questions.QuestionDetails
import org.odk.collect.android.utilities.ApplicationConstants
import org.odk.collect.android.utilities.QuestionMediaManager
import org.odk.collect.android.widgets.interfaces.FileWidget
import org.odk.collect.android.widgets.interfaces.WidgetDataReceiver
import org.odk.collect.android.widgets.utilities.WaitingForDataRegistry

@SuppressLint("ViewConstructor")
class ArbitraryFileWidget(
    context: Context,
    questionDetails: QuestionDetails,
    private val widgetAnswer: WidgetAnswer,
    questionMediaManager: QuestionMediaManager,
    waitingForDataRegistry: WaitingForDataRegistry
) : BaseArbitraryFileWidget(
        context,
        questionDetails,
        questionMediaManager,
        waitingForDataRegistry
    ), FileWidget, WidgetDataReceiver {
    lateinit var binding: ArbitraryFileWidgetBinding

    init {
        render()
    }

    override fun onCreateAnswerView(context: Context, prompt: FormEntryPrompt, answerFontSize: Int): View {
        binding = ArbitraryFileWidgetBinding.inflate((context as Activity).layoutInflater)
        setupAnswerFile(prompt.answerText)

        binding.arbitraryFileButton.visibility = if (questionDetails.isReadOnly) GONE else VISIBLE
        binding.arbitraryFileButton.setOnClickListener { onButtonClick() }
        binding.answerViewContainer.setOnClickListener {
            mediaUtils.openFile(
                getContext(),
                answerFile!!,
                null
            )
        }
        if (answerFile != null) {
            widgetAnswer.setAnswer(answerFile!!.name)
            binding.answerViewContainer.visibility = VISIBLE
        } else {
            binding.answerViewContainer.visibility = GONE
        }
        binding.answerViewContainer.addView(widgetAnswer)

        return binding.root
    }

    override fun clearAnswer() {
        binding.answerViewContainer.visibility = GONE
        deleteFile()
        widgetValueChanged()
    }

    private fun onButtonClick() {
        waitingForDataRegistry.waitForData(formEntryPrompt.index)
        mediaUtils.pickFile(
            (context as Activity),
            "*/*",
            ApplicationConstants.RequestCodes.ARBITRARY_FILE_CHOOSER
        )
    }

    override fun setOnLongClickListener(listener: OnLongClickListener?) {
        binding.arbitraryFileButton.setOnLongClickListener(listener)
        binding.answerViewContainer.setOnLongClickListener(listener)
    }

    override fun showAnswerText() {
        widgetAnswer.setAnswer(answerFile!!.name)
        binding.answerViewContainer.visibility = VISIBLE
    }

    override fun hideAnswerText() {
        binding.answerViewContainer.visibility = GONE
    }
}
