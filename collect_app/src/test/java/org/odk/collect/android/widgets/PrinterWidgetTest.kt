package org.odk.collect.android.widgets

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.javarosa.core.model.data.IAnswerData
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.odk.collect.android.R
import org.odk.collect.android.formentry.PrinterWidgetViewModel
import org.odk.collect.android.formentry.questions.QuestionDetails
import org.odk.collect.android.support.WidgetTestActivity
import org.odk.collect.android.utilities.QuestionMediaManager
import org.odk.collect.android.widgets.base.QuestionWidgetTest
import org.odk.collect.androidshared.ui.multiclicksafe.MultiClickSafeMaterialButton
import org.odk.collect.printer.HtmlPrinter
import org.odk.collect.qrcode.QRCodeCreator
import org.odk.collect.testshared.FakeScheduler

class PrinterWidgetTest : QuestionWidgetTest<PrinterWidget, IAnswerData>() {
    private val scheduler = FakeScheduler()
    private val questionMediaManager = mock<QuestionMediaManager>()
    private val qrCodeCreator = mock<QRCodeCreator>()
    private val htmlPrinter = mock<HtmlPrinter>()

    override fun createWidget() = PrinterWidget(
        activity,
        QuestionDetails(formEntryPrompt),
        PrinterWidgetViewModel(scheduler, qrCodeCreator, htmlPrinter),
        questionMediaManager,
        dependencies
    )

    @Test
    fun `clicking the button should trigger printing html document if answer exists`() {
        whenever(formEntryPrompt.answerText).thenReturn("blah")

        val widget = createWidget()
        widget.findViewById<MultiClickSafeMaterialButton>(R.id.printer_button).performClick()
        scheduler.runBackground()
        scheduler.runForeground()

        verify(htmlPrinter).print(any(), any())
    }

    @Test
    fun `clicking the button should not trigger printing if there is no answer`() {
        whenever(formEntryPrompt.answerText).thenReturn(null)

        val widget = createWidget()
        widget.findViewById<MultiClickSafeMaterialButton>(R.id.printer_button).performClick()

        verifyNoInteractions(htmlPrinter)
    }

    @Test
    override fun widgetShouldBeRegisteredForContextMenu() {
        val viewsRegisterForContextMenu = (activity as WidgetTestActivity).viewsRegisterForContextMenu

        assertThat(viewsRegisterForContextMenu.isEmpty(), equalTo(true))
    }

    @Test
    override fun usingReadOnlyOptionShouldMakeAllClickableElementsDisabled() = Unit // ignore

    @Test
    override fun callingClearShouldRemoveTheExistingAnswer() = Unit // ignore

    @Test
    override fun callingClearShouldCallValueChangeListeners() = Unit // ignore

    @Test
    override fun getAnswerShouldReturnExistingAnswerIfPromptHasExistingAnswer() = Unit // ignore

    @Test
    override fun whenReadOnlyQuestionHasAnswer_answerContainerShouldBeDisplayed() = Unit // ignore

    override fun getNextAnswer() = null
}
