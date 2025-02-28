/*
 * Copyright (C) 2015 GeoODK
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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.util.TypedValue;
import android.view.View;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.databinding.GeotraceQuestionBinding;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.maps.MapConfigurator;
import org.odk.collect.android.widgets.interfaces.GeoDataRequester;
import org.odk.collect.android.widgets.interfaces.WidgetDataReceiver;
import org.odk.collect.android.widgets.utilities.GeoWidgetUtils;
import org.odk.collect.android.widgets.utilities.WaitingForDataRegistry;

/**
 * GeoTraceWidget allows the user to collect a trace of GPS points as the
 * device moves along a path.
 */
@SuppressLint("ViewConstructor")
public class GeoTraceWidget extends QuestionWidget implements WidgetDataReceiver {
    GeotraceQuestionBinding binding;

    private final WaitingForDataRegistry waitingForDataRegistry;
    private final MapConfigurator mapConfigurator;
    private final GeoDataRequester geoDataRequester;

    public GeoTraceWidget(Context context, QuestionDetails questionDetails, WaitingForDataRegistry waitingForDataRegistry,
                          MapConfigurator mapConfigurator, GeoDataRequester geoDataRequester, Dependencies dependencies) {
        super(context, dependencies, questionDetails);
        render();

        this.waitingForDataRegistry = waitingForDataRegistry;
        this.mapConfigurator = mapConfigurator;
        this.geoDataRequester = geoDataRequester;
    }

    @Override
    protected View onCreateAnswerView(Context context, FormEntryPrompt prompt, int answerFontSize) {
        binding = GeotraceQuestionBinding.inflate(((Activity) context).getLayoutInflater());

        binding.geoAnswerText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, answerFontSize);

        binding.simpleButton.setOnClickListener(v -> {
            if (mapConfigurator.isAvailable(context)) {
                geoDataRequester.requestGeoTrace(prompt, getAnswerText(), waitingForDataRegistry);
            } else {
                mapConfigurator.showUnavailableMessage(context);
            }
        });

        String stringAnswer = GeoWidgetUtils.getGeoPolyAnswerToDisplay(prompt.getAnswerText());
        binding.geoAnswerText.setText(stringAnswer);
        binding.geoAnswerText.setVisibility(binding.geoAnswerText.getText().toString().isBlank() ? GONE : VISIBLE);

        boolean dataAvailable = stringAnswer != null && !stringAnswer.isEmpty();

        if (getFormEntryPrompt().isReadOnly()) {
            if (dataAvailable) {
                binding.simpleButton.setText(org.odk.collect.strings.R.string.view_line);
            } else {
                binding.simpleButton.setVisibility(View.GONE);
            }
        } else {
            if (dataAvailable) {
                binding.simpleButton.setText(org.odk.collect.strings.R.string.view_or_change_line);
            } else {
                binding.simpleButton.setText(org.odk.collect.strings.R.string.get_line);
            }
        }

        return binding.getRoot();
    }

    @Override
    public IAnswerData getAnswer() {
        return getAnswerText().isEmpty() ? null : new StringData(getAnswerText());
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        binding.simpleButton.setOnLongClickListener(l);
        binding.geoAnswerText.setOnLongClickListener(l);
    }

    @Override
    public void clearAnswer() {
        binding.geoAnswerText.setText(null);
        binding.geoAnswerText.setVisibility(GONE);
        binding.simpleButton.setText(org.odk.collect.strings.R.string.get_line);
        widgetValueChanged();
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        binding.simpleButton.cancelLongPress();
        binding.geoAnswerText.cancelLongPress();
    }

    @Override
    public void setData(Object answer) {
        binding.geoAnswerText.setText(answer.toString());
        binding.geoAnswerText.setVisibility(binding.geoAnswerText.getText().toString().isBlank() ? GONE : VISIBLE);
        binding.simpleButton.setText(answer.toString().isEmpty() ? org.odk.collect.strings.R.string.get_line : org.odk.collect.strings.R.string.view_or_change_line);
        widgetValueChanged();
    }

    private String getAnswerText() {
        return binding.geoAnswerText.getText().toString();
    }
}
