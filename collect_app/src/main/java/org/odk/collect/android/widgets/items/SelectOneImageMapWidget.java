/*
 * Copyright 2018 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.odk.collect.android.widgets.items;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.webkit.WebView;

import androidx.annotation.Nullable;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.SelectOneData;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.listeners.AdvanceToNextListener;
import org.odk.collect.android.utilities.SelectOneWidgetUtils;
import org.odk.collect.android.widgets.interfaces.SelectChoiceLoader;

/**
 * A widget which is responsible for multi select questions represented by
 * an svg map. You can use maps of the world, countries, human body etc.
 */
@SuppressLint("ViewConstructor")
public class SelectOneImageMapWidget extends SelectImageMapWidget {

    @Nullable
    private AdvanceToNextListener listener;

    private final boolean autoAdvance;

    public SelectOneImageMapWidget(Context context, QuestionDetails questionDetails, boolean autoAdvance, SelectChoiceLoader selectChoiceLoader, Dependencies dependencies) {
        super(context, questionDetails, selectChoiceLoader, dependencies);

        this.autoAdvance = autoAdvance;

        if (context instanceof AdvanceToNextListener) {
            listener = (AdvanceToNextListener) context;
        }

        if (questionDetails.getPrompt().getAnswerValue() != null) {

            selections.add(SelectOneWidgetUtils.getSelectedItem(questionDetails.getPrompt(), items));
            refreshSelectedItemsLabel();
        }
    }

    @Override
    protected void highlightSelections(WebView view) {
        if (!selections.isEmpty()) {
            view.loadUrl("javascript:addSelectedArea('" + selections.get(0).getValue() + "')");
        }
    }

    @Override
    public IAnswerData getAnswer() {
        return selections.isEmpty() ? null
                : new SelectOneData(selections.get(0));
    }

    @Override
    protected void selectArea(String areaId) {
        super.selectArea(areaId);

        ((Activity) getContext()).runOnUiThread(() -> {
            if (autoAdvance && listener != null) {
                listener.advance();
            }
        });
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
    }
}
