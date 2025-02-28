package org.odk.collect.android.widgets.range;

import android.view.View;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.javarosa.core.model.RangeQuestion;
import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.listeners.WidgetValueChangedListener;
import org.odk.collect.android.support.MockFormEntryPromptBuilder;
import org.odk.collect.testshared.SliderExtKt;

import java.math.BigDecimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.mockValueChangedListener;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.promptWithQuestionDefAndAnswer;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.promptWithReadOnlyAndQuestionDef;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.widgetDependencies;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.widgetTestActivity;

@RunWith(AndroidJUnit4.class)
public class RangeIntegerWidgetTest {
    private static final String NO_TICKS_APPEARANCE = "no-ticks";

    private RangeQuestion rangeQuestion;

    @Before
    public void setup() {
        rangeQuestion = mock(RangeQuestion.class);
        when(rangeQuestion.getRangeStart()).thenReturn(BigDecimal.ONE);
        when(rangeQuestion.getRangeEnd()).thenReturn(BigDecimal.TEN);
        when(rangeQuestion.getRangeStep()).thenReturn(BigDecimal.ONE);
    }

    @Test
    public void getAnswer_whenPromptDoesNotHaveAnswer_returnsNull() {
        assertNull(createWidget(promptWithReadOnlyAndQuestionDef(rangeQuestion)).getAnswer());
    }

    @Test
    public void getAnswer_whenPromptHasAnswer_returnsAnswer() {
        RangeIntegerWidget widget = createWidget(promptWithQuestionDefAndAnswer(rangeQuestion, new StringData("4")));
        assertEquals(widget.getAnswer().getValue(), 4);
    }

    @Test
    public void whenPromptDoesNotHaveAnswer_sliderShowsNoAnswerMarked() {
        RangeIntegerWidget widget = createWidget(promptWithQuestionDefAndAnswer(rangeQuestion, null));
        assertThat(widget.slider.getValue(), equalTo(1.0F));
        assertThat(widget.slider.getThumbRadius(), equalTo(0));
    }

    @Test
    public void whenPromptHasAnswer_sliderShowsCorrectAnswer() {
        RangeIntegerWidget widget = createWidget(promptWithQuestionDefAndAnswer(rangeQuestion, new StringData("4")));
        assertThat(widget.slider.getValue(), equalTo(4.0F));
        assertThat(widget.slider.getThumbRadius(), not(0));
    }

    @Test
    public void whenPromptDoesNotHaveAnswer_widgetShowsNullAnswer() {
        RangeIntegerWidget widget = createWidget(promptWithQuestionDefAndAnswer(rangeQuestion, null));
        assertThat(widget.currentValue.getText(), equalTo(""));
    }

    @Test
    public void whenPromptHasAnswer_widgetShouldShowCorrectAnswer() {
        RangeIntegerWidget widget = createWidget(promptWithQuestionDefAndAnswer(rangeQuestion, new StringData("4")));
        assertThat(widget.currentValue.getText(), equalTo("4"));
    }

    @Test
    public void whenSliderIsDiscrete_widgetShowsCorrectSliderValues() {
        RangeIntegerWidget widget = createWidget(promptWithQuestionDefAndAnswer(rangeQuestion, new StringData("4")));

        assertThat(widget.slider.getValueFrom(), equalTo(1.0F));
        assertThat(widget.slider.getValueTo(), equalTo(10.0F));
        assertThat(widget.slider.getValue(), equalTo(4.0F));
        assertThat(widget.slider.isTickVisible(), equalTo(true));
    }

    @Test
    public void whenSliderIsContinuous_widgetShowsCorrectSliderValues() {
        FormEntryPrompt prompt = new MockFormEntryPromptBuilder()
                .withQuestion(rangeQuestion)
                .withAnswer(new StringData("4"))
                .withAppearance(NO_TICKS_APPEARANCE)
                .build();
        RangeIntegerWidget widget = createWidget(prompt);

        assertThat(widget.slider.getValueFrom(), equalTo(1.0F));
        assertThat(widget.slider.getValueTo(), equalTo(10.0F));
        assertThat(widget.slider.getValue(), equalTo(4.0F));
        assertThat(widget.slider.isTickVisible(), equalTo(false));
    }

    @Test
    public void clearAnswer_clearsWidgetAnswer() {
        RangeIntegerWidget widget = createWidget(promptWithQuestionDefAndAnswer(rangeQuestion, new StringData("4")));
        widget.clearAnswer();
        assertThat(widget.currentValue.getText(), equalTo(""));
    }

    @Test
    public void clearAnswer_hidesSliderThumb() {
        RangeIntegerWidget widget = createWidget(promptWithQuestionDefAndAnswer(rangeQuestion, new StringData("2")));
        widget.clearAnswer();
        assertThat(widget.slider.getThumbRadius(), equalTo(0));
    }

    @Test
    public void clearAnswer_callsValueChangeListener() {
        RangeIntegerWidget widget = createWidget(promptWithQuestionDefAndAnswer(rangeQuestion, null));
        WidgetValueChangedListener valueChangedListener = mockValueChangedListener(widget);
        widget.clearAnswer();

        verify(valueChangedListener).widgetValueChanged(widget);
    }

    @Test
    public void changingSliderValue_showsSliderThumb() {
        RangeIntegerWidget widget = createWidget(promptWithQuestionDefAndAnswer(rangeQuestion, null));
        SliderExtKt.clickOnMinValue(widget.slider);
        assertThat(widget.slider.getThumbRadius(), not(0));
    }

    @Test
    public void changingSliderValue_whenRangeStartIsSmallerThanRangeEnd_updatesAnswer() {
        RangeIntegerWidget widget = createWidget(promptWithQuestionDefAndAnswer(rangeQuestion, null));
        SliderExtKt.clickOnMaxValue(widget.slider);
        assertThat(widget.currentValue.getText(), equalTo("10"));
    }

    @Test
    public void changingSliderValue_whenRangeStartIsGreaterThanRangeEnd_updatesAnswer() {
        when(rangeQuestion.getRangeStart()).thenReturn(BigDecimal.TEN);
        when(rangeQuestion.getRangeEnd()).thenReturn(BigDecimal.ONE);

        RangeIntegerWidget widget = createWidget(promptWithQuestionDefAndAnswer(rangeQuestion, null));
        SliderExtKt.clickOnMaxValue(widget.slider);

        assertThat(widget.currentValue.getText(), equalTo("1"));
    }

    @Test
    public void changingSliderValue_callsValueChangeListener() {
        RangeIntegerWidget widget = createWidget(promptWithQuestionDefAndAnswer(rangeQuestion, null));
        WidgetValueChangedListener valueChangedListener = mockValueChangedListener(widget);
        SliderExtKt.clickOnMaxValue(widget.slider);

        verify(valueChangedListener).widgetValueChanged(widget);
    }

    @Test
    public void changingSliderValueProgramatically_doesNotUpdateAnswer() {
        RangeIntegerWidget widget = createWidget(promptWithQuestionDefAndAnswer(rangeQuestion, null));
        widget.slider.setValue(4);
        assertThat(widget.currentValue.getText(), equalTo(""));
    }

    @Test
    public void changingSliderValueProgramatically_doesNotCallValueChangeListener() {
        RangeIntegerWidget widget = createWidget(promptWithQuestionDefAndAnswer(rangeQuestion, null));
        WidgetValueChangedListener valueChangedListener = mockValueChangedListener(widget);
        widget.slider.setValue(4);

        verify(valueChangedListener, never()).widgetValueChanged(widget);
    }

    @Test
    public void clickingSliderForLong_doesNotCallLongClickListener() {
        View.OnLongClickListener listener = mock(View.OnLongClickListener.class);

        RangeIntegerWidget widget = createWidget(promptWithQuestionDefAndAnswer(rangeQuestion, null));
        widget.setOnLongClickListener(listener);
        widget.slider.performLongClick();

        verify(listener, never()).onLongClick(widget.slider);
    }

    @Test // https://github.com/getodk/collect/issues/5530
    public void everyTriggerWidgetShouldHaveCheckboxWithUniqueID() {
        RangeIntegerWidget widget1 = createWidget(promptWithQuestionDefAndAnswer(rangeQuestion, null));
        RangeIntegerWidget widget2 = createWidget(promptWithQuestionDefAndAnswer(rangeQuestion, null));

        assertThat(widget1.slider.getId(), not(equalTo(widget2.slider.getId())));
    }

    @Test
    public void changingSliderValueToTheMinOneWhenSliderHasNoValue_setsTheValueCorrectly() {
        RangeIntegerWidget widget = createWidget(promptWithQuestionDefAndAnswer(rangeQuestion, null));

        SliderExtKt.clickOnMinValue(widget.slider);

        assertThat(widget.currentValue.getText(), equalTo("1"));
    }

    @Test
    public void changingSliderValueToAnyOtherThanTheMinOne_setsTheValueCorrectly() {
        RangeIntegerWidget widget = createWidget(promptWithQuestionDefAndAnswer(rangeQuestion, null));

        SliderExtKt.clickOnMaxValue(widget.slider);

        assertThat(widget.currentValue.getText(), equalTo("10"));
    }

    private RangeIntegerWidget createWidget(FormEntryPrompt prompt) {
        RangeIntegerWidget widget = new RangeIntegerWidget(widgetTestActivity(), new QuestionDetails(prompt), widgetDependencies());
        widget.slider.layout(0, 0, 100, 10);
        return widget;
    }
}
