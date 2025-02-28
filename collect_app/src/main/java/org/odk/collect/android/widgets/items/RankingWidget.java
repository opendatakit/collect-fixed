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
import android.util.TypedValue;
import android.view.View;
import androidx.annotation.NonNull;
import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.SelectMultiData;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.activities.FormFillingActivity;
import org.odk.collect.android.databinding.RankingWidgetBinding;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.fragments.dialogs.RankingWidgetDialog;
import org.odk.collect.android.utilities.HtmlUtils;
import org.odk.collect.android.widgets.QuestionWidget;
import org.odk.collect.android.widgets.interfaces.SelectChoiceLoader;
import org.odk.collect.android.widgets.interfaces.WidgetDataReceiver;
import org.odk.collect.android.widgets.utilities.WaitingForDataRegistry;
import org.odk.collect.android.widgets.warnings.SpacesInUnderlyingValuesWarning;
import java.util.ArrayList;
import java.util.List;

@SuppressLint("ViewConstructor")
public class RankingWidget extends QuestionWidget implements WidgetDataReceiver {

    private final WaitingForDataRegistry waitingForDataRegistry;
    private List<SelectChoice> savedItems;
    private final List<SelectChoice> items;
    RankingWidgetBinding binding;

    public RankingWidget(Context context, QuestionDetails prompt, WaitingForDataRegistry waitingForDataRegistry, SelectChoiceLoader selectChoiceLoader, Dependencies dependencies) {
        super(context, dependencies, prompt);
        this.waitingForDataRegistry = waitingForDataRegistry;
        items = ItemsWidgetUtils.loadItemsAndHandleErrors(this, questionDetails.getPrompt(), selectChoiceLoader);
        readSavedItems();
        render();
    }

    @Override
    protected View onCreateAnswerView(@NonNull Context context, @NonNull FormEntryPrompt prompt, int answerFontSize) {
        binding = RankingWidgetBinding.inflate(((Activity) context).getLayoutInflater());

        binding.rankItemsButton.setOnClickListener(v -> {
            waitingForDataRegistry.waitForData(getFormEntryPrompt().getIndex());
            RankingWidgetDialog rankingWidgetDialog = new RankingWidgetDialog(savedItems == null ? items : savedItems, getFormEntryPrompt());
            rankingWidgetDialog.show(((FormFillingActivity) getContext()).getSupportFragmentManager(), "RankingDialog");
        });
        binding.answer.setText(getAnswerText());
        binding.answer.setTextSize(TypedValue.COMPLEX_UNIT_DIP, answerFontSize);
        binding.answer.setVisibility(binding.answer.getText().toString().isBlank() ? GONE : VISIBLE);

        if (questionDetails.isReadOnly()) {
            binding.rankItemsButton.setVisibility(View.GONE);
        }

        SpacesInUnderlyingValuesWarning
                .forQuestionWidget(this)
                .renderWarningIfNecessary(savedItems == null ? items : savedItems);

        return binding.getRoot();
    }

    @Override
    public IAnswerData getAnswer() {
        List<Selection> orderedItems = new ArrayList<>();
        if (savedItems != null) {
            for (SelectChoice selectChoice : savedItems) {
                orderedItems.add(new Selection(selectChoice));
            }
        }

        return orderedItems.isEmpty() ? null : new SelectMultiData(orderedItems);
    }

    @Override
    public void clearAnswer() {
        savedItems = null;
        binding.answer.setText(null);
        binding.answer.setVisibility(GONE);
        widgetValueChanged();
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        binding.rankItemsButton.setOnLongClickListener(l);
        binding.answer.setOnLongClickListener(l);
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        binding.rankItemsButton.cancelLongPress();
        binding.answer.cancelLongPress();
    }

    @Override
    public void setData(Object values) {
        savedItems = (List<SelectChoice>) values;
        binding.answer.setText(getAnswerText());
        binding.answer.setVisibility(binding.answer.getText().toString().isBlank() ? GONE : VISIBLE);
        widgetValueChanged();
    }

    private void readSavedItems() {
        List<Selection> savedOrderedItems =
                getFormEntryPrompt().getAnswerValue() == null
                ? new ArrayList<>()
                : (List<Selection>) getFormEntryPrompt().getAnswerValue().getValue();

        if (!savedOrderedItems.isEmpty()) {
            savedItems = new ArrayList<>();
            for (Selection selection : savedOrderedItems) {
                for (SelectChoice selectChoice : items) {
                    if (selection.getValue().equals(selectChoice.getValue())) {
                        savedItems.add(selectChoice);
                        break;
                    }
                }
            }

            for (SelectChoice selectChoice : items) {
                if (!savedItems.contains(selectChoice)) {
                    savedItems.add(selectChoice);
                }
            }
        }
    }

    private CharSequence getAnswerText() {
        StringBuilder answerText = new StringBuilder();
        if (savedItems != null) {
            for (SelectChoice item : savedItems) {
                answerText
                        .append(savedItems.indexOf(item) + 1)
                        .append(". ")
                        .append(getFormEntryPrompt().getSelectChoiceText(item));
                if ((savedItems.size() - 1) > savedItems.indexOf(item)) {
                    answerText.append("<br>");
                }
            }
        }
        return HtmlUtils.textToHtml(answerText.toString());
    }
}
