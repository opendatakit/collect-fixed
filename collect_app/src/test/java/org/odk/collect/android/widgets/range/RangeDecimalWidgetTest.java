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
import static org.hamcrest.Matchers.nullValue;
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
public class RangeDecimalWidgetTest {
    private static final String NO_TICKS_APPEARANCE = "no-ticks";

    private RangeQuestion rangeQuestion;

    @Before
    public void setup() {
        rangeQuestion = mock(RangeQuestion.class);
        when(rangeQuestion.getRangeStart()).thenReturn(BigDecimal.valueOf(1.5));
        when(rangeQuestion.getRangeEnd()).thenReturn(BigDecimal.valueOf(5.5));
        when(rangeQuestion.getRangeStep()).thenReturn(BigDecimal.valueOf(0.5));
    }

    @Test
    public void getAnswer_whenPromptDoesNotHaveAnswer_returnsNull() {
        assertThat(createWidget(promptWithReadOnlyAndQuestionDef(rangeQuestion)).getAnswer(), nullValue());
    }

    @Test
    public void getAnswer_whenPromptHasAnswer_returnsAnswer() {
        RangeDecimalWidget widget = createWidget(promptWithQuestionDefAndAnswer(rangeQuestion, new StringData("2.5")));
        assertThat(widget.getAnswer().getValue(), equalTo(2.5));
    }

    @Test
    public void whenPromptDoesNotHaveAnswer_sliderShowsNoAnswerMarked() {
        RangeDecimalWidget widget = createWidget(promptWithQuestionDefAndAnswer(rangeQuestion, null));
        assertThat(widget.slider.getValue(), equalTo(1.5F));
        assertThat(widget.slider.getThumbRadius(), equalTo(0));
    }

    @Test
    public void whenPromptHasAnswer_sliderShowsCorrectAnswer() {
        RangeDecimalWidget widget = createWidget(promptWithQuestionDefAndAnswer(rangeQuestion, new StringData("2.5")));
        assertThat(widget.slider.getValue(), equalTo(2.5F));
        assertThat(widget.slider.getThumbRadius(), not(0));
    }

    @Test
    public void whenPromptDoesNotHaveAnswer_widgetShouldShowNullAnswer() {
        RangeDecimalWidget widget = createWidget(promptWithQuestionDefAndAnswer(rangeQuestion, null));
        assertThat(widget.currentValue.getText(), equalTo(""));
    }

    @Test
    public void whenPromptHasAnswer_widgetShouldShowCorrectAnswer() {
        RangeDecimalWidget widget = createWidget(promptWithQuestionDefAndAnswer(rangeQuestion, new StringData("2.5")));
        assertThat(widget.currentValue.getText(), equalTo("2.5"));
    }

    @Test
    public void whenSliderIsDiscrete_widgetShowsCorrectSlider() {
        RangeDecimalWidget widget = createWidget(promptWithQuestionDefAndAnswer(rangeQuestion, new StringData("2.5")));

        assertThat(widget.slider.getValueFrom(), equalTo(1.5F));
        assertThat(widget.slider.getValueTo(), equalTo(5.5F));
        assertThat(widget.slider.getValue(), equalTo(2.5F));
        assertThat(widget.slider.isTickVisible(), equalTo(true));
    }

    @Test
    public void whenSliderIsContinuous_widgetShowsCorrectSlider() {
        FormEntryPrompt prompt = new MockFormEntryPromptBuilder()
                .withQuestion(rangeQuestion)
                .withAnswer(new StringData("2.5"))
                .withAppearance(NO_TICKS_APPEARANCE)
                .build();
        RangeDecimalWidget widget = createWidget(prompt);

        assertThat(widget.slider.getValueFrom(), equalTo(1.5F));
        assertThat(widget.slider.getValueTo(), equalTo(5.5F));
        assertThat(widget.slider.getValue(), equalTo(2.5F));
        assertThat(widget.slider.isTickVisible(), equalTo(false));
    }

    @Test
    public void clearAnswer_clearsWidgetAnswer() {
        RangeDecimalWidget widget = createWidget(promptWithQuestionDefAndAnswer(rangeQuestion, new StringData("2.5")));
        widget.clearAnswer();
        assertThat(widget.currentValue.getText(), equalTo(""));
    }

    @Test
    public void clearAnswer_hidesSliderThumb() {
        RangeDecimalWidget widget = createWidget(promptWithQuestionDefAndAnswer(rangeQuestion, new StringData("2.5")));
        widget.clearAnswer();
        assertThat(widget.slider.getThumbRadius(), equalTo(0));
    }

    @Test
    public void clearAnswer_callsValueChangeListener() {
        RangeDecimalWidget widget = createWidget(promptWithQuestionDefAndAnswer(rangeQuestion, new StringData("2.5")));
        WidgetValueChangedListener valueChangedListener = mockValueChangedListener(widget);
        widget.clearAnswer();

        verify(valueChangedListener).widgetValueChanged(widget);
    }

    @Test
    public void changingSliderValue_updatesAnswer() {
        RangeDecimalWidget widget = createWidget(promptWithQuestionDefAndAnswer(rangeQuestion, null));
        SliderExtKt.clickOnMaxValue(widget.slider);
        assertThat(widget.currentValue.getText(), equalTo("5.5"));
    }

    @Test
    public void changingSliderValue_showsSliderThumb() {
        RangeDecimalWidget widget = createWidget(promptWithQuestionDefAndAnswer(rangeQuestion, null));
        SliderExtKt.clickOnMinValue(widget.slider);
        assertThat(widget.slider.getThumbRadius(), not(0));
    }

    @Test
    public void changingSliderValue_whenRangeStartIsGreaterThanRangeEnd_updatesAnswer() {
        when(rangeQuestion.getRangeStart()).thenReturn(BigDecimal.valueOf(5.5));
        when(rangeQuestion.getRangeEnd()).thenReturn(BigDecimal.valueOf(1.5));

        RangeDecimalWidget widget = createWidget(promptWithQuestionDefAndAnswer(rangeQuestion, null));
        SliderExtKt.clickOnMaxValue(widget.slider);

        assertThat(widget.currentValue.getText(), equalTo("1.5"));
    }

    @Test
    public void changingSliderValue_callsValueChangeListener() {
        RangeDecimalWidget widget = createWidget(promptWithQuestionDefAndAnswer(rangeQuestion, null));
        WidgetValueChangedListener valueChangedListener = mockValueChangedListener(widget);
        SliderExtKt.clickOnMaxValue(widget.slider);

        verify(valueChangedListener).widgetValueChanged(widget);
    }

    @Test
    public void changingSliderValueProgramatically_doesNotUpdateAnswer() {
        RangeDecimalWidget widget = createWidget(promptWithQuestionDefAndAnswer(rangeQuestion, null));
        widget.slider.setValue(2.5F);
        assertThat(widget.currentValue.getText(), equalTo(""));
    }

    @Test
    public void changingSliderValueProgramatically_doesNotCallValueChangeListener() {
        RangeDecimalWidget widget = createWidget(promptWithQuestionDefAndAnswer(rangeQuestion, null));
        WidgetValueChangedListener valueChangedListener = mockValueChangedListener(widget);
        widget.slider.setValue(2.5F);
        verify(valueChangedListener, never()).widgetValueChanged(widget);
    }

    @Test
    public void clickingSliderForLong_doesNotCallLongClickListener() {
        View.OnLongClickListener listener = mock(View.OnLongClickListener.class);
        RangeDecimalWidget widget = createWidget(promptWithQuestionDefAndAnswer(rangeQuestion, null));
        widget.setOnLongClickListener(listener);
        widget.slider.performLongClick();

        verify(listener, never()).onLongClick(widget.slider);
    }

    @Test // https://github.com/getodk/collect/issues/5530
    public void everyTriggerWidgetShouldHaveCheckboxWithUniqueID() {
        RangeDecimalWidget widget1 = createWidget(promptWithQuestionDefAndAnswer(rangeQuestion, null));
        RangeDecimalWidget widget2 = createWidget(promptWithQuestionDefAndAnswer(rangeQuestion, null));

        assertThat(widget1.slider.getId(), not(equalTo(widget2.slider.getId())));
    }

    @Test
    public void changingSliderValueToTheMinOneWhenSliderHasNoValue_setsTheValue() {
        RangeDecimalWidget widget = createWidget(promptWithQuestionDefAndAnswer(rangeQuestion, null));

        SliderExtKt.clickOnMinValue(widget.slider);

        assertThat(widget.currentValue.getText(), equalTo("1.5"));
    }

    @Test
    public void changingSliderValueToAnyOtherThanTheMinOne_setsTheValueCorrectly() {
        RangeDecimalWidget widget = createWidget(promptWithQuestionDefAndAnswer(rangeQuestion, null));

        SliderExtKt.clickOnMaxValue(widget.slider);

        assertThat(widget.currentValue.getText(), equalTo("5.5"));
    }

    private RangeDecimalWidget createWidget(FormEntryPrompt prompt) {
        RangeDecimalWidget widget = new RangeDecimalWidget(widgetTestActivity(), new QuestionDetails(prompt), widgetDependencies());
        widget.slider.layout(0, 0, 100, 10);
        return widget;
    }
}
