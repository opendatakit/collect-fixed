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

package org.odk.collect.android.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.appcompat.widget.AppCompatCheckBox;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.R;
import org.odk.collect.android.formentry.questions.QuestionDetails;

@SuppressLint("ViewConstructor")
public class TriggerWidget extends QuestionWidget {

    private static final String OK_TEXT = "OK";

    private AppCompatCheckBox triggerButton;

    public TriggerWidget(Context context, QuestionDetails prompt, Dependencies dependencies) {
        super(context, dependencies, prompt);
        render();
    }

    @Override
    protected View onCreateAnswerView(Context context, FormEntryPrompt prompt, int answerTextSize) {
        ViewGroup answerView = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.trigger_widget_answer, null);

        triggerButton = answerView.findViewById(R.id.check_box);
        triggerButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, answerTextSize);
        triggerButton.setEnabled(!prompt.isReadOnly());
        triggerButton.setChecked(OK_TEXT.equals(prompt.getAnswerText()));
        triggerButton.setOnCheckedChangeListener((buttonView, isChecked) -> widgetValueChanged());
        triggerButton.setId(View.generateViewId());

        return answerView;
    }

    @Override
    public void clearAnswer() {
        triggerButton.setChecked(false);
        widgetValueChanged();
    }

    @Override
    public IAnswerData getAnswer() {
        return triggerButton.isChecked() ? new StringData(OK_TEXT) : null;
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        triggerButton.setOnLongClickListener(l);
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        triggerButton.cancelLongPress();
    }

    public CheckBox getCheckBox() {
        return triggerButton;
    }

}
