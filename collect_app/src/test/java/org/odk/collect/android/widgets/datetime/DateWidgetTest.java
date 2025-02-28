package org.odk.collect.android.widgets.datetime;

import android.view.View;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.model.data.DateData;
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
import static org.hamcrest.Matchers.equalTo;
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
public class DateWidgetTest {
    private WidgetTestActivity widgetActivity;
    private DateTimeWidgetUtils widgetUtils;
    private View.OnLongClickListener onLongClickListener;

    private QuestionDef questionDef;
    private LocalDateTime dateAnswer;

    @Before
    public void setUp() {
        widgetActivity = widgetTestActivity();

        questionDef = mock(QuestionDef.class);
        onLongClickListener = mock(View.OnLongClickListener.class);
        widgetUtils = mock(DateTimeWidgetUtils.class);

        dateAnswer = DateTimeUtils.getSelectedDate(new LocalDateTime().withDate(2010, 5, 12), LocalDateTime.now());
    }

    @Test
    public void usingReadOnlyOption_doesNotShowButton() {
        DateWidget widget = createWidget(promptWithReadOnlyAndQuestionDef(questionDef));
        assertEquals(widget.binding.dateButton.getVisibility(), View.GONE);
    }

    @Test
    public void whenPromptIsNotReadOnly_buttonShowsCorrectText() {
        DateWidget widget = createWidget(promptWithQuestionDefAndAnswer(questionDef, null));
        assertEquals(widget.binding.dateButton.getText(), widget.getContext().getString(org.odk.collect.strings.R.string.select_date));
    }

    @Test
    public void getAnswer_whenPromptDoesNotHaveAnswer_returnsNull() {
        assertThat(createWidget(promptWithQuestionDefAndAnswer(questionDef, null)).getAnswer(), nullValue());
    }

    @Test
    public void getAnswer_whenPromptHasAnswer_returnsDate() {
        DateWidget widget = createWidget(promptWithQuestionDefAndAnswer(questionDef, new DateData(dateAnswer.toDate())));
        assertEquals(widget.getAnswer().getDisplayText(), new DateData(dateAnswer.toDate()).getDisplayText());
    }

    @Test
    public void whenPromptDoesNotHaveAnswer_answerTextViewShowsNoDateSelected() {
        DateWidget widget = createWidget(promptWithQuestionDefAndAnswer(questionDef, null));
        assertEquals(widget.binding.dateAnswerText.getText(), widget.getContext().getString(org.odk.collect.strings.R.string.no_date_selected));
    }

    @Test
    public void whenPromptHasAnswer_answerTextViewShowsCorrectDate() {
        FormEntryPrompt prompt = promptWithQuestionDefAndAnswer(questionDef, new DateData(dateAnswer.toDate()));
        DatePickerDetails datePickerDetails = DateTimeWidgetUtils.getDatePickerDetails(prompt.getAppearanceHint());
        DateWidget widget = createWidget(prompt);

        assertEquals(widget.binding.dateAnswerText.getText(),
                DateTimeWidgetUtils.getDateTimeLabel(dateAnswer.toDate(), datePickerDetails, false, widget.getContext()));
    }

    @Test
    public void clickingButton_callsDisplayDatePickerDialogWithCurrentDate_whenPromptDoesNotHaveAnswer() {
        FormEntryPrompt prompt = promptWithQuestionDefAndAnswer(questionDef, null);
        DateWidget widget = createWidget(prompt);
        widget.binding.dateButton.performClick();

        verify(widgetUtils).showDatePickerDialog(widgetActivity, DateTimeWidgetUtils.getDatePickerDetails(
                prompt.getAppearanceHint()), DateTimeUtils.getCurrentDateTime());
    }

    @Test
    public void clickingButton_callsDisplayDatePickerDialogWithSelectedDate_whenPromptHasAnswer() {
        FormEntryPrompt prompt = promptWithQuestionDefAndAnswer(questionDef, new DateData(dateAnswer.toDate()));
        DateWidget widget = createWidget(prompt);
        widget.binding.dateButton.performClick();

        verify(widgetUtils).showDatePickerDialog(widgetActivity, DateTimeWidgetUtils.getDatePickerDetails(
                prompt.getAppearanceHint()), dateAnswer);
    }

    @Test
    public void clearAnswer_clearsWidgetAnswer() {
        DateWidget widget = createWidget(promptWithQuestionDefAndAnswer(questionDef, null));
        widget.clearAnswer();
        assertEquals(widget.binding.dateAnswerText.getText(), widget.getContext().getString(org.odk.collect.strings.R.string.no_date_selected));
    }

    @Test
    public void clearAnswer_callsValueChangeListener() {
        DateWidget widget = createWidget(promptWithQuestionDefAndAnswer(questionDef, new DateData(dateAnswer.toDate())));
        WidgetValueChangedListener valueChangedListener = mockValueChangedListener(widget);
        widget.clearAnswer();

        verify(valueChangedListener).widgetValueChanged(widget);
    }

    @Test
    public void clickingButtonAndAnswerTextViewForLong_callsLongClickListener() {
        DateWidget widget = createWidget(promptWithQuestionDefAndAnswer(questionDef, new DateData(dateAnswer.toDate())));
        widget.setOnLongClickListener(onLongClickListener);
        widget.binding.dateButton.performLongClick();
        widget.binding.dateAnswerText.performLongClick();

        verify(onLongClickListener).onLongClick(widget.binding.dateButton);
        verify(onLongClickListener).onLongClick(widget.binding.dateAnswerText);
    }

    @Test
    public void setData_updatesWidgetAnswer() {
        DateWidget widget = createWidget(promptWithQuestionDefAndAnswer(questionDef, null));
        widget.setData(dateAnswer);
        assertThat(widget.getAnswer().getDisplayText(), equalTo(new DateData(dateAnswer.toDate()).getDisplayText()));
    }

    @Test
    public void setData_updatesValueDisplayedInAnswerTextView() {
        FormEntryPrompt prompt = promptWithQuestionDefAndAnswer(questionDef, null);
        DatePickerDetails datePickerDetails = DateTimeWidgetUtils.getDatePickerDetails(prompt.getAppearanceHint());
        DateWidget widget = createWidget(prompt);
        widget.setData(dateAnswer);

        assertEquals(widget.binding.dateAnswerText.getText(),
                DateTimeWidgetUtils.getDateTimeLabel(dateAnswer.toDate(), datePickerDetails, false, widget.getContext()));
    }

    @Test
    public void setData_callsValueChangeListener() {
        DateWidget widget = createWidget(promptWithQuestionDefAndAnswer(questionDef, null));
        WidgetValueChangedListener valueChangedListener = mockValueChangedListener(widget);
        widget.setValueChangedListener(valueChangedListener);
        widget.setData(dateAnswer);

        verify(valueChangedListener).widgetValueChanged(widget);
    }

    private DateWidget createWidget(FormEntryPrompt prompt) {
        return new DateWidget(widgetActivity, new QuestionDetails(prompt), widgetUtils, new FakeWaitingForDataRegistry(), widgetDependencies());
    }
}
