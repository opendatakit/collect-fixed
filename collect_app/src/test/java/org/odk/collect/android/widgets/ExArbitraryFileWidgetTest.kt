package org.odk.collect.android.widgets

import android.view.View
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.javarosa.core.model.data.StringData
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.odk.collect.android.formentry.questions.QuestionDetails
import org.odk.collect.android.injection.config.AppDependencyModule
import org.odk.collect.android.support.CollectHelpers
import org.odk.collect.android.utilities.ApplicationConstants
import org.odk.collect.android.utilities.MediaUtils
import org.odk.collect.android.widgets.base.FileWidgetTest
import org.odk.collect.android.widgets.support.FakeQuestionMediaManager
import org.odk.collect.android.widgets.support.FakeWaitingForDataRegistry
import org.odk.collect.android.widgets.utilities.FileRequester
import org.odk.collect.android.widgets.utilities.QuestionFontSizeUtils
import org.odk.collect.android.widgets.utilities.QuestionFontSizeUtils.getFontSize
import org.odk.collect.androidshared.system.IntentLauncher
import org.odk.collect.settings.keys.ProjectKeys.KEY_FONT_SIZE

class ExArbitraryFileWidgetTest : FileWidgetTest<ExArbitraryFileWidget?>() {
    private val fileRequester = mock<FileRequester>()
    private val mediaUtils = mock<MediaUtils>().also {
        whenever(it.isAudioFile(any())).thenReturn(true)
    }

    @Before
    fun setup() {
        CollectHelpers.overrideAppDependencyModule(object : AppDependencyModule() {
            override fun providesMediaUtils(intentLauncher: IntentLauncher): MediaUtils {
                return mediaUtils
            }
        })
    }

    override fun getInitialAnswer(): StringData {
        return StringData("document.pdf")
    }

    override fun getNextAnswer(): StringData {
        return StringData("document.xlsx")
    }

    override fun createWidget(): ExArbitraryFileWidget {
        return ExArbitraryFileWidget(
            activity, QuestionDetails(formEntryPrompt, readOnlyOverride),
            FakeQuestionMediaManager(), FakeWaitingForDataRegistry(), fileRequester
        )
    }

    @Test
    fun `Use custom font size when font size changes`() {
        settingsProvider.getUnprotectedSettings().save(KEY_FONT_SIZE, "30")

        assertThat(
            widget!!.binding.exArbitraryFileButton.textSize.toInt(), equalTo(
                getFontSize(
                    settingsProvider.getUnprotectedSettings(),
                    QuestionFontSizeUtils.FontSize.BODY_LARGE
                )
            )
        )
        assertThat(
            widget!!.binding.exArbitraryFileAnswerText.textSize.toInt(), equalTo(
                getFontSize(
                    settingsProvider.getUnprotectedSettings(),
                    QuestionFontSizeUtils.FontSize.HEADLINE_6
                )
            )
        )
    }

    @Test
    fun `Hide the answer text when there is no answer`() {
        assertThat(widget!!.binding.exArbitraryFileAnswerText.visibility, equalTo(View.GONE))
    }

    @Test
    fun `Display the answer text when there is answer`() {
        whenever(formEntryPrompt.answerText).thenReturn(initialAnswer.displayText)

        val widget = widget!!
        assertThat(widget.binding.exArbitraryFileAnswerText.visibility, equalTo(View.VISIBLE))
        assertThat(widget.binding.exArbitraryFileAnswerText.text, equalTo(initialAnswer.displayText))
    }

    @Test
    fun `External file picker should be called when clicking on button`() {
        widget!!.binding.exArbitraryFileButton.performClick()
        verify(fileRequester).launch(activity, ApplicationConstants.RequestCodes.EX_ARBITRARY_FILE_CHOOSER, formEntryPrompt)
    }

    @Test
    fun `File viewer should be called when clicking on answer`() {
        whenever(formEntryPrompt.answerText).thenReturn(initialAnswer.displayText)

        val widget = widget!!
        widget.binding.exArbitraryFileAnswerText.performClick()
        verify(mediaUtils).openFile(activity, widget.answerFile!!, null)
    }

    @Test
    fun `Hide the answer when clear answer is called`() {
        whenever(formEntryPrompt.answerText).thenReturn(initialAnswer.displayText)

        val widget = widget!!
        widget.clearAnswer()
        assertThat(widget.binding.exArbitraryFileAnswerText.visibility, equalTo(View.GONE))
    }

    @Test
    fun `Remove the answer when set data called with unsupported type`() {
        whenever(formEntryPrompt.answerText).thenReturn(initialAnswer.displayText)

        val widget = widget!!
        widget.setData(null)
        assertThat(widget.answer, equalTo(null))
        assertThat(widget.binding.exArbitraryFileAnswerText.visibility, equalTo(View.GONE))
    }

    @Test
    fun `All clickable elements should be disabled when read-only override option is used`() {
        readOnlyOverride = true
        whenever(formEntryPrompt.isReadOnly).thenReturn(false)
        whenever(formEntryPrompt.answerText).thenReturn(initialAnswer.displayText)

        val widget = widget!!
        assertThat(widget.binding.exArbitraryFileButton.visibility, equalTo(View.GONE))
        assertThat(widget.binding.exArbitraryFileAnswerText.visibility, equalTo(View.VISIBLE))
        assertThat(widget.binding.exArbitraryFileAnswerText.text, equalTo(initialAnswer.displayText))
    }

    @Test
    override fun usingReadOnlyOptionShouldMakeAllClickableElementsDisabled() {
        whenever(formEntryPrompt.isReadOnly).thenReturn(true)
        whenever(formEntryPrompt.answerText).thenReturn(initialAnswer.displayText)

        val widget = widget!!
        assertThat(widget.binding.exArbitraryFileButton.visibility, equalTo(View.GONE))
        assertThat(widget.binding.exArbitraryFileAnswerText.visibility, equalTo(View.VISIBLE))
        assertThat(widget.binding.exArbitraryFileAnswerText.text, equalTo(initialAnswer.displayText))
    }
}
