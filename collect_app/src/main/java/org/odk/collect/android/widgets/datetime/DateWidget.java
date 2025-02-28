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

import org.javarosa.core.model.data.DateData;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.form.api.FormEntryPrompt;
import org.joda.time.LocalDateTime;
import org.odk.collect.android.databinding.DateWidgetAnswerBinding;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.widgets.QuestionWidget;
import org.odk.collect.android.widgets.interfaces.WidgetDataReceiver;
import org.odk.collect.android.widgets.utilities.DateTimeWidgetUtils;
import org.odk.collect.android.widgets.utilities.WaitingForDataRegistry;

@SuppressLint("ViewConstructor")
public class DateWidget extends QuestionWidget implements WidgetDataReceiver {
    private final WaitingForDataRegistry waitingForDataRegistry;
    DateWidgetAnswerBinding binding;

    private final DateTimeWidgetUtils widgetUtils;

    private LocalDateTime selectedDate;
    private DatePickerDetails datePickerDetails;

    public DateWidget(Context context, QuestionDetails prompt, DateTimeWidgetUtils widgetUtils, WaitingForDataRegistry waitingForDataRegistry, Dependencies dependencies) {
        super(context, dependencies, prompt);
        render();

        this.widgetUtils = widgetUtils;
        this.waitingForDataRegistry = waitingForDataRegistry;
    }

    @Override
    protected View onCreateAnswerView(Context context, FormEntryPrompt prompt, int answerFontSize) {
        binding = DateWidgetAnswerBinding.inflate(((Activity) context).getLayoutInflater());
        datePickerDetails = DateTimeWidgetUtils.getDatePickerDetails(prompt.getAppearanceHint());

        if (prompt.isReadOnly()) {
            binding.dateButton.setVisibility(GONE);
        } else {
            binding.dateButton.setOnClickListener(v -> {
                waitingForDataRegistry.waitForData(prompt.getIndex());
                widgetUtils.showDatePickerDialog(context, datePickerDetails, selectedDate);
            });
        }
        binding.dateAnswerText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, answerFontSize);

        if (prompt.getAnswerValue() == null) {
            selectedDate = DateTimeUtils.getCurrentDateTime();
        } else {
            selectedDate = DateTimeUtils.getSelectedDate(new LocalDateTime(getFormEntryPrompt().getAnswerValue().getValue()),
                    LocalDateTime.now());
            binding.dateAnswerText.setText(DateTimeWidgetUtils.getDateTimeLabel(selectedDate.toDate(),
                    datePickerDetails, false, context));
        }

        return binding.getRoot();
    }

    @Override
    public IAnswerData getAnswer() {
        return binding.dateAnswerText.getText().equals(getContext().getString(org.odk.collect.strings.R.string.no_date_selected))
                ? null
                : new DateData(selectedDate.toDate());
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        binding.dateButton.setOnLongClickListener(l);
        binding.dateAnswerText.setOnLongClickListener(l);
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        binding.dateButton.cancelLongPress();
        binding.dateAnswerText.cancelLongPress();
    }

    @Override
    public void clearAnswer() {
        selectedDate = DateTimeUtils.getCurrentDateTime();
        binding.dateAnswerText.setText(org.odk.collect.strings.R.string.no_date_selected);
        widgetValueChanged();
    }

    @Override
    public void setData(Object answer) {
        if (answer instanceof LocalDateTime) {
            selectedDate = DateTimeUtils.getSelectedDate((LocalDateTime) answer, LocalDateTime.now());
            binding.dateAnswerText.setText(DateTimeWidgetUtils.getDateTimeLabel(
                    selectedDate.toDate(), datePickerDetails, false, getContext()));
            widgetValueChanged();
        }
    }
}
