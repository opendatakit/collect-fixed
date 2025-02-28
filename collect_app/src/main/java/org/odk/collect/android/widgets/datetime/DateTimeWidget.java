/*
 * Copyright (C) 2009 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.odk.collect.android.widgets.datetime;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.util.TypedValue;
import android.view.View;

import org.javarosa.core.model.data.DateTimeData;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.TimeData;
import org.javarosa.form.api.FormEntryPrompt;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.odk.collect.android.databinding.DateTimeWidgetAnswerBinding;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.widgets.QuestionWidget;
import org.odk.collect.android.widgets.interfaces.WidgetDataReceiver;
import org.odk.collect.android.widgets.utilities.DateTimeWidgetUtils;
import org.odk.collect.android.widgets.utilities.WaitingForDataRegistry;

/**
 * Displays a DatePicker widget. DateWidget handles leap years and does not allow dates that do not
 * exist.
 */
@SuppressLint("ViewConstructor")
public class DateTimeWidget extends QuestionWidget implements WidgetDataReceiver {
    private final WaitingForDataRegistry waitingForDataRegistry;
    DateTimeWidgetAnswerBinding binding;

    private final DateTimeWidgetUtils widgetUtils;

    private LocalDateTime selectedDateTime;
    private DatePickerDetails datePickerDetails;

    public DateTimeWidget(Context context, QuestionDetails prompt, DateTimeWidgetUtils widgetUtils, WaitingForDataRegistry waitingForDataRegistry, Dependencies dependencies) {
        super(context, dependencies, prompt);
        render();

        this.widgetUtils = widgetUtils;
        this.waitingForDataRegistry = waitingForDataRegistry;
    }

    @Override
    protected View onCreateAnswerView(Context context, FormEntryPrompt prompt, int answerFontSize) {
        binding = DateTimeWidgetAnswerBinding.inflate(((Activity) context).getLayoutInflater());
        datePickerDetails = DateTimeWidgetUtils.getDatePickerDetails(prompt.getAppearanceHint());

        if (prompt.isReadOnly()) {
            binding.dateWidget.dateButton.setVisibility(GONE);
            binding.timeWidget.timeButton.setVisibility(GONE);
        } else {
            binding.dateWidget.dateButton.setOnClickListener(v -> {
                waitingForDataRegistry.waitForData(prompt.getIndex());
                widgetUtils.showDatePickerDialog(context, datePickerDetails, selectedDateTime);
            });

            binding.timeWidget.timeButton.setOnClickListener(v -> {
                waitingForDataRegistry.waitForData(prompt.getIndex());
                widgetUtils.showTimePickerDialog(context, selectedDateTime);
            });
        }
        binding.dateWidget.dateAnswerText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, answerFontSize);
        binding.timeWidget.timeAnswerText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, answerFontSize);

        selectedDateTime = DateTimeUtils.getCurrentDateTime();

        if (getFormEntryPrompt().getAnswerValue() != null) {
            LocalDateTime selectedDate = new LocalDateTime(getFormEntryPrompt().getAnswerValue().getValue());
            selectedDateTime = DateTimeUtils.getSelectedDate(selectedDate, selectedDateTime);
            binding.dateWidget.dateAnswerText.setText(DateTimeWidgetUtils.getDateTimeLabel(
                    selectedDate.toDate(), datePickerDetails, false, context));

            DateTime selectedTime = new DateTime(getFormEntryPrompt().getAnswerValue().getValue());
            selectedDateTime = DateTimeUtils.getSelectedTime(selectedTime.toLocalDateTime(), selectedDateTime);
            binding.timeWidget.timeAnswerText.setText(DateTimeUtils.getTimeData(selectedTime).getDisplayText());
        }

        return binding.getRoot();
    }

    @Override
    public IAnswerData getAnswer() {
        if (isNullValue()) {
            return null;
        } else {
            if (isTimeNull()) {
                selectedDateTime = DateTimeUtils.getSelectedDate(selectedDateTime, LocalDateTime.now());
            } else if (isDateNull()) {
                selectedDateTime = DateTimeUtils.getSelectedTime(selectedDateTime, LocalDateTime.now());
            }
            return new DateTimeData(selectedDateTime.toDate());
        }
    }

    @Override
    public void clearAnswer() {
        resetAnswerFields();
        widgetValueChanged();
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        binding.dateWidget.dateButton.setOnLongClickListener(l);
        binding.dateWidget.dateAnswerText.setOnLongClickListener(l);

        binding.timeWidget.timeButton.setOnLongClickListener(l);
        binding.timeWidget.timeAnswerText.setOnLongClickListener(l);
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        binding.dateWidget.dateButton.cancelLongPress();
        binding.dateWidget.dateAnswerText.cancelLongPress();

        binding.timeWidget.timeButton.cancelLongPress();
        binding.timeWidget.timeAnswerText.cancelLongPress();
    }

    @Override
    public void setData(Object answer) {
        if (answer instanceof LocalDateTime) {
            selectedDateTime = DateTimeUtils.getSelectedDate((LocalDateTime) answer, selectedDateTime);
            binding.dateWidget.dateAnswerText.setText(DateTimeWidgetUtils.getDateTimeLabel(
                    selectedDateTime.toDate(), datePickerDetails, false, getContext()));
            widgetValueChanged();
        }
        if (answer instanceof DateTime) {
            selectedDateTime = DateTimeUtils.getSelectedTime(((DateTime) answer).toLocalDateTime(), selectedDateTime);
            binding.timeWidget.timeAnswerText.setText(new TimeData(selectedDateTime.toDate()).getDisplayText());
            widgetValueChanged();
        }
    }

    private void resetAnswerFields() {
        selectedDateTime = DateTimeUtils.getCurrentDateTime();
        binding.dateWidget.dateAnswerText.setText(org.odk.collect.strings.R.string.no_date_selected);
        binding.timeWidget.timeAnswerText.setText(org.odk.collect.strings.R.string.no_time_selected);
    }

    private boolean isNullValue() {
        return getFormEntryPrompt().isRequired()
                ? isDateNull() || isTimeNull()
                : isDateNull() && isTimeNull();
    }

    private boolean isDateNull() {
        return binding.dateWidget.dateAnswerText.getText().equals(getContext().getString(org.odk.collect.strings.R.string.no_date_selected));
    }

    private boolean isTimeNull() {
        return binding.timeWidget.timeAnswerText.getText().equals(getContext().getString(org.odk.collect.strings.R.string.no_time_selected));
    }
}
