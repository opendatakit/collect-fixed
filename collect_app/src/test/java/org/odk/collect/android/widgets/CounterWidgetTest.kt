package org.odk.collect.android.widgets

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.javarosa.core.model.data.IAnswerData
import org.javarosa.core.model.data.IntegerData
import org.junit.Test
import org.mockito.Mockito.verify
import org.mockito.kotlin.whenever
import org.odk.collect.android.formentry.questions.QuestionDetails
import org.odk.collect.android.widgets.CounterWidget.Companion.MAX_VALUE
import org.odk.collect.android.widgets.base.QuestionWidgetTest
import org.odk.collect.android.widgets.support.QuestionWidgetHelpers

class CounterWidgetTest : QuestionWidgetTest<CounterWidget, IAnswerData>() {

    override fun createWidget() = CounterWidget(
        activity,
        QuestionDetails(formEntryPrompt),
        dependencies
    )

    override fun getNextAnswer() = IntegerData(10)

    @Test
    override fun usingReadOnlyOptionShouldMakeAllClickableElementsDisabled() {
        whenever(formEntryPrompt.isReadOnly).thenReturn(true)

        assertThat(widget.binding.minusButton.isEnabled, equalTo(false))
        assertThat(widget.binding.plusButton.isEnabled, equalTo(false))
    }

    @Test
    fun `only the minus button should be disabled when there is no answer`() {
        assertThat(widget.binding.minusButton.isEnabled, equalTo(false))
        assertThat(widget.binding.plusButton.isEnabled, equalTo(true))
    }

    @Test
    fun `only the minus button should be disabled when the answer is 0`() {
        whenever(formEntryPrompt.answerValue).thenReturn(IntegerData(0))

        assertThat(widget.binding.minusButton.isEnabled, equalTo(false))
        assertThat(widget.binding.plusButton.isEnabled, equalTo(true))
    }

    @Test
    fun `both buttons should be enabled when the answer is greater than 0`() {
        whenever(formEntryPrompt.answerValue).thenReturn(IntegerData(1))

        assertThat(widget.binding.minusButton.isEnabled, equalTo(true))
        assertThat(widget.binding.plusButton.isEnabled, equalTo(true))
    }

    @Test
    fun `only the plus button should be disabled when the answer is 999 999 999`() {
        whenever(formEntryPrompt.answerValue).thenReturn(IntegerData(MAX_VALUE))

        assertThat(widget.binding.minusButton.isEnabled, equalTo(true))
        assertThat(widget.binding.plusButton.isEnabled, equalTo(false))
    }

    @Test
    fun `the min supported value is 0`() {
        whenever(formEntryPrompt.answerValue).thenReturn(IntegerData(-1))

        assertThat(widget.binding.value.text, equalTo(""))
        assertThat(widget.answer, equalTo(null))
    }

    @Test
    fun `the max supported value is 999 999 999`() {
        whenever(formEntryPrompt.answerValue).thenReturn(IntegerData(MAX_VALUE + 1))

        assertThat(widget.binding.value.text, equalTo(""))
        assertThat(widget.answer, equalTo(null))
    }

    @Test
    fun `clicking the minus button decreases the value`() {
        whenever(formEntryPrompt.answerValue).thenReturn(IntegerData(10))

        widget.binding.minusButton.performClick()
        assertThat(widget.binding.value.text.toString(), equalTo("9"))

        widget.binding.minusButton.performClick()
        assertThat(widget.binding.value.text.toString(), equalTo("8"))
    }

    @Test
    fun `clicking the plus button increases the value`() {
        widget.binding.plusButton.performClick()
        assertThat(widget.binding.value.text.toString(), equalTo("1"))

        widget.binding.plusButton.performClick()
        assertThat(widget.binding.value.text.toString(), equalTo("2"))
    }

    @Test
    fun `clicking the minus button disables the button when the value is the min supported one`() {
        whenever(formEntryPrompt.answerValue).thenReturn(IntegerData(1))

        widget.binding.minusButton.performClick()
        assertThat(widget.binding.value.text.toString(), equalTo("0"))
        assertThat(widget.binding.minusButton.isEnabled, equalTo(false))
    }

    @Test
    fun `clicking the plus button disables the button when the value is the max supported one`() {
        whenever(formEntryPrompt.answerValue).thenReturn(IntegerData(MAX_VALUE - 1))

        widget.binding.plusButton.performClick()
        assertThat(widget.binding.value.text.toString(), equalTo(MAX_VALUE.toString()))
        assertThat(widget.binding.plusButton.isEnabled, equalTo(false))
    }

    @Test
    fun `clicking the minus button calls widgetValueChanged listener`() {
        whenever(formEntryPrompt.answerValue).thenReturn(IntegerData(1))
        val valueChangedListener = QuestionWidgetHelpers.mockValueChangedListener<QuestionWidget>(widget)

        widget.binding.minusButton.performClick()
        verify(valueChangedListener).widgetValueChanged(widget)
    }

    @Test
    fun `clicking the plus button calls widgetValueChanged listener`() {
        val valueChangedListener = QuestionWidgetHelpers.mockValueChangedListener<QuestionWidget>(widget)

        widget.binding.plusButton.performClick()
        verify(valueChangedListener).widgetValueChanged(widget)
    }

    @Test
    fun `#clearAnswer should remove the displayed value`() {
        whenever(formEntryPrompt.answerValue).thenReturn(IntegerData(10))

        assertThat(widget.binding.value.text.toString(), equalTo("10"))
        widget.clearAnswer()
        assertThat(widget.binding.value.text.toString(), equalTo(""))
    }
}
