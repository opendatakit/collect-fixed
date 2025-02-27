package org.odk.collect.android.widgets

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.TypedValue
import android.view.View
import org.javarosa.form.api.FormEntryPrompt
import org.odk.collect.android.databinding.ExArbitraryFileWidgetAnswerBinding
import org.odk.collect.android.formentry.questions.QuestionDetails
import org.odk.collect.android.utilities.ApplicationConstants
import org.odk.collect.android.utilities.QuestionMediaManager
import org.odk.collect.android.widgets.utilities.FileRequester
import org.odk.collect.android.widgets.utilities.WaitingForDataRegistry

@SuppressLint("ViewConstructor")
class ExArbitraryFileWidget(
    context: Context,
    questionDetails: QuestionDetails,
    questionMediaManager: QuestionMediaManager,
    waitingForDataRegistry: WaitingForDataRegistry,
    private val fileRequester: FileRequester
) : BaseArbitraryFileWidget(
        context,
        questionDetails,
        questionMediaManager,
        waitingForDataRegistry
    ) {
    lateinit var binding: ExArbitraryFileWidgetAnswerBinding

    override fun onCreateAnswerView(context: Context, prompt: FormEntryPrompt, answerFontSize: Int): View {
        binding = ExArbitraryFileWidgetAnswerBinding.inflate((context as Activity).layoutInflater)
        setupAnswerFile(prompt.answerText)

        binding.exArbitraryFileAnswerText.setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            answerFontSize.toFloat()
        )

        if (questionDetails.isReadOnly) {
            binding.exArbitraryFileButton.visibility = GONE
        } else {
            binding.exArbitraryFileButton.setOnClickListener { onButtonClick() }
            binding.exArbitraryFileAnswerText.setOnClickListener {
                mediaUtils.openFile(
                    getContext(),
                    answerFile,
                    null
                )
            }
        }

        if (answerFile != null) {
            binding.exArbitraryFileAnswerText.text = answerFile.name
            binding.exArbitraryFileAnswerText.visibility = VISIBLE
        }

        return binding.root
    }

    override fun clearAnswer() {
        binding.exArbitraryFileAnswerText.visibility = GONE
        deleteFile()
        widgetValueChanged()
    }

    override fun setOnLongClickListener(listener: OnLongClickListener?) {
        binding.exArbitraryFileButton.setOnLongClickListener(listener)
        binding.exArbitraryFileAnswerText.setOnLongClickListener(listener)
    }

    override fun showAnswerText() {
        binding.exArbitraryFileAnswerText.text = answerFile.name
        binding.exArbitraryFileAnswerText.visibility = VISIBLE
    }

    override fun hideAnswerText() {
        binding.exArbitraryFileAnswerText.visibility = GONE
    }

    private fun onButtonClick() {
        waitingForDataRegistry.waitForData(formEntryPrompt.index)
        fileRequester.launch(
            (context as Activity), ApplicationConstants.RequestCodes.EX_ARBITRARY_FILE_CHOOSER,
            formEntryPrompt
        )
    }
}
