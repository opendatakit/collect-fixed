package org.odk.collect.android.widgets

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.TypedValue
import android.view.View
import org.javarosa.form.api.FormEntryPrompt
import org.odk.collect.android.databinding.ArbitraryFileWidgetAnswerBinding
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
    questionMediaManager: QuestionMediaManager,
    waitingForDataRegistry: WaitingForDataRegistry
) : BaseArbitraryFileWidget(
        context,
        questionDetails,
        questionMediaManager,
        waitingForDataRegistry
    ), FileWidget, WidgetDataReceiver {
    lateinit var binding: ArbitraryFileWidgetAnswerBinding

    override fun onCreateAnswerView(context: Context, prompt: FormEntryPrompt, answerFontSize: Int): View {
        binding = ArbitraryFileWidgetAnswerBinding.inflate((context as Activity).layoutInflater)
        setupAnswerFile(prompt.answerText)

        binding.arbitraryFileAnswerText.setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            answerFontSize.toFloat()
        )

        binding.arbitraryFileButton.visibility = if (questionDetails.isReadOnly) GONE else VISIBLE
        binding.arbitraryFileButton.setOnClickListener { onButtonClick() }
        binding.arbitraryFileAnswerText.setOnClickListener {
            mediaUtils.openFile(
                getContext(),
                answerFile,
                null
            )
        }

        if (answerFile != null) {
            binding.arbitraryFileAnswerText.text = answerFile.name
            binding.arbitraryFileAnswerText.visibility = VISIBLE
        }

        return binding.root
    }

    override fun clearAnswer() {
        binding.arbitraryFileAnswerText.visibility = GONE
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
        binding.arbitraryFileAnswerText.setOnLongClickListener(listener)
    }

    override fun showAnswerText() {
        binding.arbitraryFileAnswerText.text = answerFile.name
        binding.arbitraryFileAnswerText.visibility = VISIBLE
    }

    override fun hideAnswerText() {
        binding.arbitraryFileAnswerText.visibility = GONE
    }
}
