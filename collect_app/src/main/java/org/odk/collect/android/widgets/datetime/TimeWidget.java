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

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.TimeData;
import org.javarosa.form.api.FormEntryPrompt;

import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.odk.collect.android.databinding.TimeWidgetAnswerBinding;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.widgets.QuestionWidget;
import org.odk.collect.android.widgets.interfaces.WidgetDataReceiver;
import org.odk.collect.android.widgets.utilities.DateTimeWidgetUtils;
import org.odk.collect.android.widgets.utilities.WaitingForDataRegistry;

@SuppressLint("ViewConstructor")
public class TimeWidget extends QuestionWidget implements WidgetDataReceiver {
    private final WaitingForDataRegistry waitingForDataRegistry;
    TimeWidgetAnswerBinding binding;

    private final DateTimeWidgetUtils widgetUtils;

    private LocalDateTime selectedTime;

    public TimeWidget(Context context, final QuestionDetails prompt, DateTimeWidgetUtils widgetUtils, WaitingForDataRegistry waitingForDataRegistry, Dependencies dependencies) {
        super(context, dependencies, prompt);
        render();

        this.widgetUtils = widgetUtils;
        this.waitingForDataRegistry = waitingForDataRegistry;
    }

    @Override
    protected View onCreateAnswerView(Context context, FormEntryPrompt prompt, int answerFontSize) {
        binding = TimeWidgetAnswerBinding.inflate(((Activity) context).getLayoutInflater());

        if (prompt.isReadOnly()) {
            binding.timeButton.setVisibility(GONE);
        } else {
            binding.timeButton.setOnClickListener(v -> {
                waitingForDataRegistry.waitForData(prompt.getIndex());
                widgetUtils.showTimePickerDialog(context, selectedTime);
            });
        }
        binding.timeAnswerText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, answerFontSize);

        if (prompt.getAnswerValue() == null) {
            selectedTime = DateTimeUtils.getCurrentDateTime();
        } else {
            DateTime dateTime = new DateTime(getFormEntryPrompt().getAnswerValue().getValue());
            selectedTime = DateTimeUtils.getSelectedTime(dateTime.toLocalDateTime(), LocalDateTime.now());
            binding.timeAnswerText.setText(DateTimeUtils.getTimeData(dateTime).getDisplayText());
        }

        return binding.getRoot();
    }

    @Override
    public void clearAnswer() {
        selectedTime = DateTimeUtils.getCurrentDateTime();
        binding.timeAnswerText.setText(org.odk.collect.strings.R.string.no_time_selected);
        widgetValueChanged();
    }

    @Override
    public IAnswerData getAnswer() {
        return binding.timeAnswerText.getText().equals(getContext().getString(org.odk.collect.strings.R.string.no_time_selected))
                ? null
                : new TimeData(selectedTime.toDateTime().toDate());
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        binding.timeButton.setOnLongClickListener(l);
        binding.timeAnswerText.setOnLongClickListener(l);
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        binding.timeButton.cancelLongPress();
        binding.timeAnswerText.cancelLongPress();
    }

    @Override
    public void setData(Object answer) {
        if (answer instanceof DateTime) {
            selectedTime = DateTimeUtils.getSelectedTime(((DateTime) answer).toLocalDateTime(), LocalDateTime.now());
            binding.timeAnswerText.setText(new TimeData(selectedTime.toDate()).getDisplayText());
            widgetValueChanged();
        }
    }
}
