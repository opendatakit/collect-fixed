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
import android.content.Context;
import android.webkit.WebView;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.SelectMultiData;
import org.javarosa.core.model.data.helper.Selection;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.widgets.interfaces.SelectChoiceLoader;
import org.odk.collect.android.widgets.warnings.SpacesInUnderlyingValuesWarning;

import java.util.List;

/**
 * A widget which is responsible for single select questions represented by
 * an svg map. You can use maps of the world, countries, human body etc.
 */
@SuppressLint("ViewConstructor")
public class SelectMultiImageMapWidget extends SelectImageMapWidget {
    public SelectMultiImageMapWidget(Context context, QuestionDetails questionDetails, SelectChoiceLoader selectChoiceLoader, Dependencies dependencies) {
        super(context, questionDetails, selectChoiceLoader, dependencies);

        if (questionDetails.getPrompt().getAnswerValue() != null) {
            selections = (List<Selection>) getFormEntryPrompt().getAnswerValue().getValue();
            refreshSelectedItemsLabel();
        }
        SpacesInUnderlyingValuesWarning.forQuestionWidget(this).renderWarningIfNecessary(items);
    }

    @Override
    protected void highlightSelections(WebView view) {
        for (Selection selection : selections) {
            view.loadUrl("javascript:addSelectedArea('" + selection.getValue() + "')");
        }
    }

    @Override
    public IAnswerData getAnswer() {
        return selections.size() == 0 ? null : new SelectMultiData(selections);
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
    }
}
