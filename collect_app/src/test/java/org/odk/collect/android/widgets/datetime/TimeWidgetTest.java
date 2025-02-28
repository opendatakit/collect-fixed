package org.odk.collect.android.widgets.datetime;

import android.view.View;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.model.data.TimeData;
import org.javarosa.form.api.FormEntryPrompt;
import org.joda.time.LocalDateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.listeners.WidgetValueChangedListener;
import org.odk.collect.android.support.WidgetTestActivity;
import org.odk.collect.android.widgets.support.FakeWaitingForDataRegistry;
import org.odk.collect.android.widgets.utilities.DateTimeWidgetUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.mockValueChangedListener;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.promptWithQuestionDefAndAnswer;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.promptWithReadOnlyAndQuestionDef;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.widgetDependencies;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.widgetTestActivity;

@RunWith(AndroidJUnit4.class)
public class TimeWidgetTest {
    private WidgetTestActivity widgetActivity;
    private DateTimeWidgetUtils widgetUtils;
    private View.OnLongClickListener onLongClickListener;

    private QuestionDef questionDef;
    private LocalDateTime timeAnswer;

    @Before
    public void setUp() {
        widgetActivity = widgetTestActivity();

        questionDef = mock(QuestionDef.class);
        onLongClickListener = mock(View.OnLongClickListener.class);
        widgetUtils = mock(DateTimeWidgetUtils.class);

        timeAnswer = DateTimeUtils.getSelectedTime(new LocalDateTime().withTime(12, 10, 0, 0), LocalDateTime.now());
    }

    @Test
    public void usingReadOnlyOption_doesNotShowButton() {
        TimeWidget widget = createWidget(promptWithReadOnlyAndQuestionDef(questionDef));
        assertEquals(widget.binding.timeButton.getVisibility(), View.GONE);
    }

    @Test
    public void whenPromptIsNotReadOnly_buttonShowsCorrectText() {
        TimeWidget widget = createWidget(promptWithQuestionDefAndAnswer(questionDef, null));
        assertEquals(widget.binding.timeButton.getText(), widget.getContext().getString(org.odk.collect.strings.R.string.select_time));
    }

    @Test
    public void getAnswer_whenPromptDoesNotHaveAnswer_returnsNull() {
        assertThat(createWidget(promptWithQuestionDefAndAnswer(questionDef, null)).getAnswer(), nullValue());
    }

    @Test
    public void getAnswer_whenPromptHasAnswer_returnsTime() {
        TimeWidget widget = createWidget(promptWithQuestionDefAndAnswer(questionDef, new TimeData(timeAnswer.toDateTime().toDate())));
        assertEquals(widget.getAnswer().getDisplayText(), new TimeData(timeAnswer.toDateTime().toDate()).getDisplayText());
    }

    @Test
    public void whenPromptDoesNotHaveAnswer_answerTextViewShowsNoTimeSelected() {
        TimeWidget widget = createWidget(promptWithQuestionDefAndAnswer(questionDef, null));
        assertEquals(widget.binding.timeAnswerText.getText(), widget.getContext().getString(org.odk.collect.strings.R.string.no_time_selected));
    }

    @Test
    public void whenPromptHasAnswer_answerTextViewShowsCorrectTime() {
        TimeWidget widget = createWidget(promptWithQuestionDefAndAnswer(questionDef, new TimeData(timeAnswer.toDateTime().toDate())));
        assertEquals(widget.binding.timeAnswerText.getText(), DateTimeUtils.getTimeData(timeAnswer.toDateTime()).getDisplayText());
    }

    @Test
    public void clickingButton_callsDisplayTimePickerDialogWithCurrentTime_whenPromptDoesNotHaveAnswer() {
        FormEntryPrompt prompt = promptWithQuestionDefAndAnswer(questionDef, null);
        TimeWidget widget = createWidget(prompt);
        widget.binding.timeButton.performClick();

        verify(widgetUtils).showTimePickerDialog(widgetActivity, DateTimeUtils.getCurrentDateTime());
    }

    @Test
    public void clickingButton_callsDisplayTimePickerDialogWithSelectedTime_whenPromptHasAnswer() {
        FormEntryPrompt prompt = promptWithQuestionDefAndAnswer(questionDef, new TimeData(timeAnswer.toDateTime().toDate()));
        TimeWidget widget = createWidget(prompt);
        widget.binding.timeButton.performClick();

        verify(widgetUtils).showTimePickerDialog(widgetActivity, timeAnswer);
    }

    @Test
    public void clearAnswer_clearsWidgetAnswer() {
        TimeWidget widget = createWidget(promptWithQuestionDefAndAnswer(questionDef, new TimeData(timeAnswer.toDateTime().toDate())));
        widget.clearAnswer();
        assertEquals(widget.binding.timeAnswerText.getText(), widget.getContext().getString(org.odk.collect.strings.R.string.no_time_selected));
    }

    @Test
    public void clearAnswer_callsValueChangeListener() {
        TimeWidget widget = createWidget(promptWithQuestionDefAndAnswer(questionDef, new TimeData(timeAnswer.toDateTime().toDate())));
        WidgetValueChangedListener valueChangedListener = mockValueChangedListener(widget);
        widget.clearAnswer();

        verify(valueChangedListener).widgetValueChanged(widget);
    }

    @Test
    public void clickingButtonAndAnswerTextViewForLong_callsLongClickListener() {
        TimeWidget widget = createWidget(promptWithQuestionDefAndAnswer(questionDef, null));
        widget.setOnLongClickListener(onLongClickListener);
        widget.binding.timeButton.performLongClick();
        widget.binding.timeAnswerText.performLongClick();

        verify(onLongClickListener).onLongClick(widget.binding.timeButton);
        verify(onLongClickListener).onLongClick(widget.binding.timeAnswerText);
    }

    @Test
    public void setData_updatesWidgetAnswer() {
        TimeWidget widget = createWidget(promptWithQuestionDefAndAnswer(questionDef, null));
        widget.setData(timeAnswer.toDateTime());
        assertEquals(widget.getAnswer().getDisplayText(), new TimeData(timeAnswer.toDateTime().toDate()).getDisplayText());
    }

    @Test
    public void setData_updatesValueDisplayedInAnswerTextView() {
        TimeWidget widget = createWidget(promptWithQuestionDefAndAnswer(questionDef, null));
        widget.setData(timeAnswer.toDateTime());
        assertEquals(widget.binding.timeAnswerText.getText(), DateTimeUtils.getTimeData(timeAnswer.toDateTime()).getDisplayText());
    }

    @Test
    public void setData_callsValueChangeListener() {
        TimeWidget widget = createWidget(promptWithQuestionDefAndAnswer(questionDef, null));
        WidgetValueChangedListener valueChangedListener = mockValueChangedListener(widget);
        widget.setValueChangedListener(valueChangedListener);
        widget.setData(timeAnswer.toDateTime());

        verify(valueChangedListener).widgetValueChanged(widget);
    }

    private TimeWidget createWidget(FormEntryPrompt prompt) {
        return new TimeWidget(widgetActivity, new QuestionDetails(prompt), widgetUtils, new FakeWaitingForDataRegistry(), widgetDependencies());
    }
}
