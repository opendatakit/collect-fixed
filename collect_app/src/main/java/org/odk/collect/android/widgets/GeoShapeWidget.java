/*
 * Copyright (C) 2014 GeoODK
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

import org.odk.collect.android.databinding.GeoshapeQuestionBinding;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.widgets.interfaces.WidgetDataReceiver;
import org.odk.collect.android.widgets.interfaces.GeoDataRequester;
import org.odk.collect.android.widgets.utilities.GeoWidgetUtils;
import org.odk.collect.android.widgets.utilities.WaitingForDataRegistry;

@SuppressLint("ViewConstructor")
public class GeoShapeWidget extends QuestionWidget implements WidgetDataReceiver {
    GeoshapeQuestionBinding binding;

    private final WaitingForDataRegistry waitingForDataRegistry;
    private final GeoDataRequester geoDataRequester;

    public GeoShapeWidget(Context context, QuestionDetails questionDetails, WaitingForDataRegistry waitingForDataRegistry,
                          GeoDataRequester geoDataRequester, Dependencies dependencies) {
        super(context, dependencies, questionDetails);
        render();

        this.waitingForDataRegistry = waitingForDataRegistry;
        this.geoDataRequester = geoDataRequester;
    }

    @Override
    protected View onCreateAnswerView(Context context, FormEntryPrompt prompt, int answerFontSize) {
        binding = GeoshapeQuestionBinding.inflate(((Activity) context).getLayoutInflater());

        binding.geoAnswerText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, answerFontSize);

        binding.simpleButton.setOnClickListener(v ->
                geoDataRequester.requestGeoShape(prompt, getAnswerText(), waitingForDataRegistry));

        String stringAnswer = GeoWidgetUtils.getGeoPolyAnswerToDisplay(prompt.getAnswerText());
        binding.geoAnswerText.setText(stringAnswer);
        binding.geoAnswerText.setVisibility(binding.geoAnswerText.getText().toString().isBlank() ? GONE : VISIBLE);

        boolean dataAvailable = stringAnswer != null && !stringAnswer.isEmpty();

        if (getFormEntryPrompt().isReadOnly()) {
            if (dataAvailable) {
                binding.simpleButton.setText(org.odk.collect.strings.R.string.view_polygon);
            } else {
                binding.simpleButton.setVisibility(View.GONE);
            }
        } else {
            if (dataAvailable) {
                binding.simpleButton.setText(org.odk.collect.strings.R.string.view_or_change_polygon);
            } else {
                binding.simpleButton.setText(org.odk.collect.strings.R.string.get_polygon);
            }
        }

        return binding.getRoot();
    }

    @Override
    public IAnswerData getAnswer() {
        return getAnswerText().isEmpty() ? null : new StringData(getAnswerText());
    }

    @Override
    public void clearAnswer() {
        binding.geoAnswerText.setText(null);
        binding.geoAnswerText.setVisibility(GONE);
        binding.simpleButton.setText(org.odk.collect.strings.R.string.get_polygon);
        widgetValueChanged();
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        binding.simpleButton.setOnLongClickListener(l);
        binding.geoAnswerText.setOnLongClickListener(l);
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
        binding.simpleButton.setText(answer.toString().isEmpty() ? org.odk.collect.strings.R.string.get_polygon : org.odk.collect.strings.R.string.view_or_change_polygon);
        widgetValueChanged();
    }

    private String getAnswerText() {
        return binding.geoAnswerText.getText().toString();
    }
}
