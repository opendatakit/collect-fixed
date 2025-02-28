/*
 * Copyright (C) 2013 Nafundi
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

package org.odk.collect.android.widgets;

import static org.odk.collect.android.utilities.ApplicationConstants.RequestCodes;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.view.View;

import androidx.core.content.ContextCompat;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.R;
import org.odk.collect.android.activities.BearingActivity;
import org.odk.collect.android.databinding.BearingWidgetAnswerBinding;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.widgets.utilities.StringWidgetUtils;
import org.odk.collect.androidshared.ui.ToastUtils;
import org.odk.collect.android.widgets.interfaces.WidgetDataReceiver;
import org.odk.collect.android.widgets.utilities.WaitingForDataRegistry;

/**
 * BearingWidget is the widget that allows the user to get a compass heading.
 */
@SuppressLint("ViewConstructor")
public class BearingWidget extends QuestionWidget implements WidgetDataReceiver {
    BearingWidgetAnswerBinding binding;

    private final WaitingForDataRegistry waitingForDataRegistry;
    private final SensorManager sensorManager;

    public BearingWidget(Context context, QuestionDetails questionDetails, WaitingForDataRegistry waitingForDataRegistry, SensorManager sensorManager, Dependencies dependencies) {
        super(context, dependencies, questionDetails);
        render();

        this.waitingForDataRegistry = waitingForDataRegistry;
        this.sensorManager = sensorManager;
    }

    @Override
    protected View onCreateAnswerView(Context context, FormEntryPrompt prompt, int answerFontSize) {
        binding = BearingWidgetAnswerBinding.inflate(((Activity) context).getLayoutInflater());

        if (prompt.isReadOnly()) {
            binding.bearingButton.setVisibility(GONE);
        } else {
            binding.bearingButton.setOnClickListener(v -> onButtonClick());
        }
        binding.widgetAnswerText.init(answerFontSize, true, null, false, this::widgetValueChanged);
        Double answer = StringWidgetUtils.getDoubleAnswerValueFromIAnswerData(questionDetails.getPrompt().getAnswerValue());
        binding.widgetAnswerText.setDecimalType(false, answer);

        String answerText = prompt.getAnswerText();
        binding.widgetAnswerText.setAnswer(answerText);
        if (answerText != null && !answerText.isEmpty()) {
            binding.bearingButton.setText(getContext().getString(org.odk.collect.strings.R.string.replace_bearing));
        }

        return binding.getRoot();
    }

    @Override
    public void clearAnswer() {
        binding.bearingButton.setText(getContext().getString(org.odk.collect.strings.R.string.get_bearing));
        binding.widgetAnswerText.setAnswer(null);
    }

    @Override
    public IAnswerData getAnswer() {
        String answerText = binding.widgetAnswerText.getAnswer();
        return answerText.isEmpty() ? null : new StringData(answerText);
    }

    @Override
    public void setData(Object answer) {
        binding.widgetAnswerText.setAnswer((String) answer);
        binding.bearingButton.setText(getContext().getString(org.odk.collect.strings.R.string.replace_bearing));
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        binding.bearingButton.setOnLongClickListener(l);
        binding.widgetAnswerText.setOnLongClickListener(l);
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        binding.bearingButton.cancelLongPress();
        binding.widgetAnswerText.cancelLongPress();
    }

    private boolean areSensorsAvailable() {
        return sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
                && sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null;
    }

    private void onButtonClick() {
        if (areSensorsAvailable()) {
            Intent intent = new Intent(getContext(), BearingActivity.class);
            ((Activity) getContext()).startActivityForResult(intent, RequestCodes.BEARING_CAPTURE);

            waitingForDataRegistry.waitForData(getFormEntryPrompt().getIndex());
        } else {
            ToastUtils.showLongToast(org.odk.collect.strings.R.string.bearing_lack_of_sensors);

            binding.bearingButton.setEnabled(false);
            binding.widgetAnswerText.updateState(false);
        }
    }

    @Override
    public void hideError() {
        super.hideError();
        binding.widgetAnswerText.setError(null);
    }

    @Override
    public void displayError(String errorMessage) {
        hideError();

        if (binding.widgetAnswerText.isEditableState()) {
            binding.widgetAnswerText.setError(errorMessage);
            setBackground(ContextCompat.getDrawable(getContext(), R.drawable.question_with_error_border));
        } else {
            super.displayError(errorMessage);
        }
    }
}
