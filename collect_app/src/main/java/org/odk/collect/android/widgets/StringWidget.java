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
import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.R;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.utilities.Appearances;
import org.odk.collect.android.views.WidgetAnswerText;
import org.odk.collect.android.widgets.utilities.QuestionFontSizeUtils;
import org.odk.collect.android.widgets.utilities.StringWidgetUtils;

/**
 * The most basic widget that allows for entry of any text.
 */
@SuppressLint("ViewConstructor")
public class StringWidget extends QuestionWidget {
    public final WidgetAnswerText widgetAnswerText;

    protected StringWidget(Context context, QuestionDetails questionDetails, Dependencies dependencies) {
        super(context, dependencies, questionDetails);

        widgetAnswerText = new WidgetAnswerText(context);
        widgetAnswerText.init(
                QuestionFontSizeUtils.getFontSize(settings, QuestionFontSizeUtils.FontSize.HEADLINE_6),
                questionDetails.isReadOnly(),
                StringWidgetUtils.getNumberOfRows(questionDetails.getPrompt()),
                Appearances.isMasked(questionDetails.getPrompt()),
                this::widgetValueChanged
        );

        render();
    }

    @Override
    protected View onCreateAnswerView(@NonNull Context context, @NonNull FormEntryPrompt prompt, int answerFontSize) {
        setDisplayValueFromModel();
        return widgetAnswerText;
    }

    @Override
    public void clearAnswer() {
        widgetAnswerText.clearAnswer();
    }

    @Override
    public IAnswerData getAnswer() {
        String answer = getAnswerText();
        return !answer.isEmpty() ? new StringData(answer) : null;
    }

    @NonNull
    public String getAnswerText() {
        return widgetAnswerText.getAnswer();
    }

    @Override
    public void setFocus(Context context) {
        widgetAnswerText.setFocus(!questionDetails.isReadOnly());
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        widgetAnswerText.setOnLongClickListener(l);
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        widgetAnswerText.cancelLongPress();
    }

    /**
     * Registers all subviews except for the answer_container (which contains the EditText) to clear on long press.
     * This makes it possible to long-press to paste or perform other text editing functions.
     */
    @Override
    protected void registerToClearAnswerOnLongPress(Activity activity, ViewGroup viewGroup) {
        ViewGroup view = findViewById(R.id.question_widget_container);
        for (int i = 0; i < view.getChildCount(); i++) {
            View childView = view.getChildAt(i);
            if (childView.getId() != R.id.answer_container) {
                childView.setTag(childView.getId());
                childView.setId(getId());
                activity.registerForContextMenu(childView);
            }
        }
    }

    public void setDisplayValueFromModel() {
        String currentAnswer = getFormEntryPrompt().getAnswerText();

        if (currentAnswer != null) {
            widgetAnswerText.setAnswer(currentAnswer);
        }
    }

    @Override
    public void hideError() {
        widgetAnswerText.setError(null);
        setBackground(null);
    }

    @Override
    public void displayError(String errorMessage) {
        widgetAnswerText.setError(errorMessage);
        setBackground(ContextCompat.getDrawable(getContext(), R.drawable.question_with_error_border));
    }
}
