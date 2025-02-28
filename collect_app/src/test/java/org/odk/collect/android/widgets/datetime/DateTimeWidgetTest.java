package org.odk.collect.android.widgets.datetime;

import android.view.View;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.model.data.DateData;
import org.javarosa.core.model.data.DateTimeData;
import org.javarosa.core.model.data.TimeData;
import org.javarosa.form.api.FormEntryPrompt;
import org.joda.time.DateTime;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.mockValueChangedListener;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.promptWithQuestionDefAndAnswer;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.promptWithReadOnlyAndQuestionDef;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.widgetDependencies;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.widgetTestActivity;

@RunWith(AndroidJUnit4.class)
public class DateTimeWidgetTest {
    private WidgetTestActivity widgetActivity;
    private DateTimeWidgetUtils widgetUtils;
    private View.OnLongClickListener onLongClickListener;

    private QuestionDef questionDef;
    private LocalDateTime localDateTime;

    @Before
    public void setUp() {
        widgetActivity = widgetTestActivity();

        questionDef = mock(QuestionDef.class);
        onLongClickListener = mock(View.OnLongClickListener.class);
        widgetUtils = mock(DateTimeWidgetUtils.class);

        localDateTime = new LocalDateTime()
                .withDate(2010, 5, 12)
                .withTime(12, 10, 0, 0);
    }

    @Test
    public void usingReadOnlyOption_doesNotShowButtons() {
        DateTimeWidget widget = createWidget(promptWithReadOnlyAndQuestionDef(questionDef));

        assertEquals(widget.binding.dateWidget.dateButton.getVisibility(), View.GONE);
        assertEquals(widget.binding.timeWidget.timeButton.getVisibility(), View.GONE);
    }

    @Test
    public void whenPromptIsNotReadOnly_buttonShowsCorrectText() {
        DateTimeWidget widget = createWidget(promptWithQuestionDefAndAnswer(questionDef, null));

        assertEquals(widget.binding.dateWidget.dateButton.getText(), widget.getContext().getString(org.odk.collect.strings.R.string.select_date));
        assertEquals(widget.binding.timeWidget.timeButton.getText(), widget.getContext().getString(org.odk.collect.strings.R.string.select_time));
    }

    @Test
    public void getAnswer_whenPromptDoesNotHaveAnswer_returnsNull() {
        assertThat(createWidget(promptWithQuestionDefAndAnswer(questionDef, null)).getAnswer(), nullValue());
    }

    @Test
    public void getAnswer_whenPromptHasDateAnswer_returnsDateAnswerAndCurrentTime() {
        DateTimeWidget widget = createWidget(promptWithQuestionDefAndAnswer(questionDef, new DateTimeData(localDateTime.toDate())));
        widget.binding.timeWidget.timeAnswerText.setText(widget.getContext().getString(org.odk.collect.strings.R.string.no_time_selected));

        assertEquals(widget.getAnswer().getDisplayText(),
                new DateTimeData(DateTimeUtils.getSelectedDate(localDateTime, LocalDateTime.now()).toDate()).getDisplayText());
    }

    @Test
    public void getAnswer_whenPromptHasTimeAnswer_returnsTimeAnswerAndCurrentDate() {
        DateTimeWidget widget = createWidget(promptWithQuestionDefAndAnswer(questionDef, new DateTimeData(localDateTime.toDate())));
        widget.binding.dateWidget.dateAnswerText.setText(widget.getContext().getString(org.odk.collect.strings.R.string.no_date_selected));

        assertEquals(widget.getAnswer().getDisplayText(),
                new DateTimeData(DateTimeUtils.getSelectedTime(localDateTime, LocalDateTime.now()).toDate()).getDisplayText());
    }

    @Test
    public void getAnswer_whenPromptHasAnswer_returnsAnswer() {
        DateTimeWidget widget = createWidget(promptWithQuestionDefAndAnswer(questionDef, new DateTimeData(localDateTime.toDate())));
        assertEquals(widget.getAnswer().getDisplayText(), new DateTimeData(localDateTime.toDate()).getDisplayText());
    }

    @Test
    public void whenPromptDoesNotHaveAnswer_answerTextViewShowsNoValueSelected() {
        DateTimeWidget widget = createWidget(promptWithQuestionDefAndAnswer(questionDef, null));

        assertEquals(widget.binding.dateWidget.dateAnswerText.getText(), widget.getContext().getString(org.odk.collect.strings.R.string.no_date_selected));
        assertEquals(widget.binding.timeWidget.timeAnswerText.getText(), widget.getContext().getString(org.odk.collect.strings.R.string.no_time_selected));
    }

    @Test
    public void whenPromptHasAnswer_answerTextViewShowsCorrectDateAndTime() {
        FormEntryPrompt prompt = promptWithQuestionDefAndAnswer(questionDef, new DateTimeData(localDateTime.toDate()));
        DatePickerDetails datePickerDetails = DateTimeWidgetUtils.getDatePickerDetails(prompt.getAppearanceHint());
        DateTimeWidget widget = createWidget(prompt);

        assertEquals(widget.binding.dateWidget.dateAnswerText.getText(),
                DateTimeWidgetUtils.getDateTimeLabel(localDateTime.toDate(), datePickerDetails, false, widget.getContext()));
        assertEquals(widget.binding.timeWidget.timeAnswerText.getText(), DateTimeUtils.getTimeData(localDateTime.toDateTime()).getDisplayText());
    }

    @Test
    public void clickingSetDateButton_callsDisplayDatePickerDialogWithCurrentDate_whenPromptDoesNotHaveAnswer() {
        FormEntryPrompt prompt = promptWithQuestionDefAndAnswer(questionDef, null);
        DateTimeWidget widget = createWidget(prompt);
        widget.binding.dateWidget.dateButton.performClick();

        verify(widgetUtils).showDatePickerDialog(widgetActivity, DateTimeWidgetUtils.getDatePickerDetails(
                prompt.getAppearanceHint()), DateTimeUtils.getCurrentDateTime());
    }

    @Test
    public void clickingSetTimeButton_callsDisplayTimePickerDialogWithCurrentTime_whenPromptDoesNotHaveAnswer() {
        FormEntryPrompt prompt = promptWithQuestionDefAndAnswer(questionDef, null);
        DateTimeWidget widget = createWidget(prompt);
        widget.binding.timeWidget.timeButton.performClick();

        verify(widgetUtils).showTimePickerDialog(widgetActivity, DateTimeUtils.getCurrentDateTime());
    }

    @Test
    public void clickingSetDateButton_callsDisplayDatePickerDialogWithSelectedDate_whenPromptHasAnswer() {
        FormEntryPrompt prompt = promptWithQuestionDefAndAnswer(questionDef, new DateData(localDateTime.toDate()));
        DateTimeWidget widget = createWidget(prompt);
        widget.binding.dateWidget.dateButton.performClick();

        verify(widgetUtils).showDatePickerDialog(widgetActivity, DateTimeWidgetUtils.getDatePickerDetails(prompt.getAppearanceHint()),
                DateTimeUtils.getSelectedDate(localDateTime, new LocalDateTime().withTime(0, 0, 0, 0)));
    }

    @Test
    public void clickingSetTimeButton_callsDisplayTimePickerDialogWithSelectedTime_whenPromptHasAnswer() {
        FormEntryPrompt prompt = promptWithQuestionDefAndAnswer(questionDef, new TimeData(localDateTime.toDateTime().toDate()));
        DateTimeWidget widget = createWidget(prompt);
        widget.binding.timeWidget.timeButton.performClick();

        verify(widgetUtils).showTimePickerDialog(widgetActivity, localDateTime);
    }

    @Test
    public void clearAnswer_clearsWidgetAnswer() {
        DateTimeWidget widget = createWidget(promptWithQuestionDefAndAnswer(questionDef, new DateTimeData(localDateTime.toDate())));
        widget.clearAnswer();

        assertThat(widget.getAnswer(), nullValue());
        assertEquals(widget.binding.dateWidget.dateAnswerText.getText(), widget.getContext().getString(org.odk.collect.strings.R.string.no_date_selected));
        assertEquals(widget.binding.timeWidget.timeAnswerText.getText(), widget.getContext().getString(org.odk.collect.strings.R.string.no_time_selected));
    }

    @Test
    public void clearAnswer_callValueChangeListener() {
        DateTimeWidget widget = createWidget(promptWithQuestionDefAndAnswer(questionDef, new DateTimeData(localDateTime.toDate())));
        WidgetValueChangedListener valueChangedListener = mockValueChangedListener(widget);
        widget.clearAnswer();

        verify(valueChangedListener).widgetValueChanged(widget);
    }

    @Test
    public void clickingButtonForLong_callsLongClickListener() {
        DateTimeWidget widget = createWidget(promptWithQuestionDefAndAnswer(questionDef, null));
        widget.setOnLongClickListener(onLongClickListener);

        widget.binding.dateWidget.dateButton.performLongClick();
        widget.binding.timeWidget.timeButton.performLongClick();

        verify(onLongClickListener).onLongClick(widget.binding.dateWidget.dateButton);
        verify(onLongClickListener).onLongClick(widget.binding.timeWidget.timeButton);
    }

    @Test
    public void clickingAnswerTextViewForLong_callsLongClickListener() {
        DateTimeWidget widget = createWidget(promptWithQuestionDefAndAnswer(questionDef, null));
        widget.setOnLongClickListener(onLongClickListener);

        widget.binding.dateWidget.dateAnswerText.performLongClick();
        widget.binding.timeWidget.timeAnswerText.performLongClick();

        verify(onLongClickListener).onLongClick(widget.binding.dateWidget.dateAnswerText);
        verify(onLongClickListener).onLongClick(widget.binding.timeWidget.timeAnswerText);
    }

    @Test
    public void setDateData_updatesValueShownInDateAnswerTextView() {
        FormEntryPrompt prompt = promptWithQuestionDefAndAnswer(questionDef, null);
        DatePickerDetails datePickerDetails = DateTimeWidgetUtils.getDatePickerDetails(prompt.getAppearanceHint());
        DateTimeWidget widget = createWidget(prompt);
        widget.setData(new LocalDateTime().withDate(2010, 5, 12));

        assertEquals(widget.binding.dateWidget.dateAnswerText.getText(),
                DateTimeWidgetUtils.getDateTimeLabel(localDateTime.toDate(), datePickerDetails, false, widget.getContext()));
    }

    @Test
    public void setDateData_updatesWidgetAnswer() {
        LocalDateTime answer = new LocalDateTime().withDate(2010, 5, 12);
        DateTimeWidget widget = createWidget(promptWithQuestionDefAndAnswer(questionDef, null));
        widget.setData(answer);

        assertEquals(widget.getAnswer().getDisplayText(),
                new DateTimeData(DateTimeUtils.getSelectedDate((LocalDateTime) answer, LocalDateTime.now()).toDate()).getDisplayText());
    }

    @Test
    public void setTimeData_updatesValueShownInTimeAnswerTextView() {
        DateTimeWidget widget = createWidget(promptWithQuestionDefAndAnswer(questionDef, null));
        widget.setData(new DateTime().withTime(12, 10, 0, 0));
        assertEquals(widget.binding.timeWidget.timeAnswerText.getText(), DateTimeUtils.getTimeData(localDateTime.toDateTime()).getDisplayText());
    }

    @Test
    public void setTimeData_updatesWidgetAnswer() {
        DateTime answer = new DateTime().withTime(12, 10, 0, 0);
        DateTimeWidget widget = createWidget(promptWithQuestionDefAndAnswer(questionDef, null));
        widget.setData(answer);

        assertEquals(widget.getAnswer().getDisplayText(),
                new DateTimeData(DateTimeUtils.getSelectedTime(((DateTime) answer).toLocalDateTime(), LocalDateTime.now()).toDate()).getDisplayText());
    }

    @Test
    public void setData_callsValueChangeListener() {
        DateTimeWidget widget = createWidget(promptWithQuestionDefAndAnswer(questionDef, null));
        WidgetValueChangedListener valueChangedListener = mockValueChangedListener(widget);
        widget.setValueChangedListener(valueChangedListener);

        widget.setData(new LocalDateTime().withDate(2010, 5, 12));
        verify(valueChangedListener, times(1)).widgetValueChanged(widget);

        widget.setData(new DateTime().withDate(2010, 5, 12));
        verify(valueChangedListener, times(2)).widgetValueChanged(widget);
    }

    private DateTimeWidget createWidget(FormEntryPrompt prompt) {
        return new DateTimeWidget(widgetActivity, new QuestionDetails(prompt), widgetUtils, new FakeWaitingForDataRegistry(), widgetDependencies());
    }
}
