package org.odk.collect.android.widgets

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.View
import org.javarosa.form.api.FormEntryPrompt
import org.odk.collect.android.databinding.ExArbitraryFileWidgetBinding
import org.odk.collect.android.formentry.questions.QuestionDetails
import org.odk.collect.android.utilities.ApplicationConstants
import org.odk.collect.android.utilities.QuestionMediaManager
import org.odk.collect.android.widgets.utilities.FileRequester
import org.odk.collect.android.widgets.utilities.WaitingForDataRegistry

@SuppressLint("ViewConstructor")
class ExArbitraryFileWidget(
    context: Context,
    questionDetails: QuestionDetails,
    private val widgetAnswer: WidgetAnswer,
    questionMediaManager: QuestionMediaManager,
    waitingForDataRegistry: WaitingForDataRegistry,
    private val fileRequester: FileRequester
) : BaseArbitraryFileWidget(
        context,
        questionDetails,
        questionMediaManager,
        waitingForDataRegistry
    ) {
    lateinit var binding: ExArbitraryFileWidgetBinding

    init {
        render()
    }

    override fun onCreateAnswerView(context: Context, prompt: FormEntryPrompt, answerFontSize: Int): View {
        binding = ExArbitraryFileWidgetBinding.inflate((context as Activity).layoutInflater)
        setupAnswerFile(prompt.answerText)

        if (questionDetails.isReadOnly) {
            binding.exArbitraryFileButton.visibility = GONE
        } else {
            binding.exArbitraryFileButton.setOnClickListener { onButtonClick() }
            binding.answerViewContainer.setOnClickListener {
                mediaUtils.openFile(
                    getContext(),
                    answerFile!!,
                    null
                )
            }
        }
        if (answerFile != null) {
            widgetAnswer.setAnswer(answerFile!!.name)
            binding.answerViewContainer.visibility = VISIBLE
        } else {
            binding.answerViewContainer.visibility = GONE
        }
        widgetAnswer.setTextSize(answerFontSize.toFloat())
        binding.answerViewContainer.addView(widgetAnswer)

        return binding.root
    }

    override fun clearAnswer() {
        binding.answerViewContainer.visibility = GONE
        deleteFile()
        widgetValueChanged()
    }

    override fun setOnLongClickListener(listener: OnLongClickListener?) {
        binding.exArbitraryFileButton.setOnLongClickListener(listener)
        binding.answerViewContainer.setOnLongClickListener(listener)
    }

    override fun showAnswerText() {
        widgetAnswer.setAnswer(answerFile!!.name)
        binding.answerViewContainer.visibility = VISIBLE
    }

    override fun hideAnswerText() {
        binding.answerViewContainer.visibility = GONE
    }

    private fun onButtonClick() {
        waitingForDataRegistry.waitForData(formEntryPrompt.index)
        fileRequester.launch(
            (context as Activity), ApplicationConstants.RequestCodes.EX_ARBITRARY_FILE_CHOOSER,
            formEntryPrompt
        )
    }
}
