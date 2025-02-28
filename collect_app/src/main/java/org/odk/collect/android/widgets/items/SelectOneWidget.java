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

package org.odk.collect.android.widgets.items;

import static org.odk.collect.android.formentry.media.FormMediaUtils.getPlayColor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.RadioButton;

import androidx.annotation.Nullable;

import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.SelectOneData;
import org.javarosa.core.model.data.helper.Selection;
import org.odk.collect.android.adapters.AbstractSelectListAdapter;
import org.odk.collect.android.adapters.SelectOneListAdapter;
import org.odk.collect.android.exception.JavaRosaException;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.javarosawrapper.FormController;
import org.odk.collect.android.listeners.AdvanceToNextListener;
import org.odk.collect.android.utilities.Appearances;
import org.odk.collect.android.utilities.SelectOneWidgetUtils;
import org.odk.collect.android.widgets.interfaces.SelectChoiceLoader;

import timber.log.Timber;

/**
 * SelectOneWidgets handles select-one fields using radio buttons.
 *
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
@SuppressLint("ViewConstructor")
public class SelectOneWidget extends BaseSelectListWidget {

    @Nullable
    private AdvanceToNextListener listener;

    private final boolean autoAdvance;
    private final FormController formController;

    public SelectOneWidget(Context context, QuestionDetails questionDetails, boolean autoAdvance, FormController formController, SelectChoiceLoader selectChoiceLoader, Dependencies dependencies) {
        super(context, questionDetails, selectChoiceLoader, dependencies);
        this.autoAdvance = autoAdvance;
        this.formController = formController;
        if (context instanceof AdvanceToNextListener) {
            listener = (AdvanceToNextListener) context;
        }
    }

    @Override
    protected AbstractSelectListAdapter setUpAdapter() {
        int numColumns = Appearances.getNumberOfColumns(getFormEntryPrompt(), screenUtils);
        boolean noButtonsMode = Appearances.isCompactAppearance(getFormEntryPrompt()) || Appearances.isNoButtonsAppearance(getFormEntryPrompt());

        recyclerViewAdapter = new SelectOneListAdapter(getSelectedValue(), this, getContext(), items,
                getFormEntryPrompt(), getReferenceManager(), audioPlayer,
                getPlayColor(getFormEntryPrompt(), themeUtils), numColumns, noButtonsMode, mediaUtils);
        return recyclerViewAdapter;
    }

    @Override
    public IAnswerData getAnswer() {
        Selection selectedItem = ((SelectOneListAdapter) recyclerViewAdapter).getSelectedItem();
        return selectedItem == null
                ? null
                : new SelectOneData(selectedItem);
    }

    protected String getSelectedValue() {
        Selection selectedItem = SelectOneWidgetUtils.getSelectedItem(getQuestionDetails().getPrompt(), items);
        return selectedItem == null ? null : selectedItem.getValue();
    }

    @Override
    public void setChoiceSelected(int choiceIndex, boolean isSelected) {
        RadioButton button = new RadioButton(getContext());
        button.setTag(choiceIndex);
        button.setChecked(isSelected);

        ((SelectOneListAdapter) recyclerViewAdapter).onCheckedChanged(button, isSelected);
    }

    @Override
    public void onItemClicked() {
        if (autoAdvance && listener != null) {
            listener.advance();
        }

        clearFollowingItemsetWidgets();
        widgetValueChanged();
    }

    @Override
    public void clearAnswer() {
        clearFollowingItemsetWidgets();
        super.clearAnswer();
    }

    /**
     * If there are "fast external itemset" selects right after this select, assume that they are linked to the current question and clear them.
     */
    private void clearFollowingItemsetWidgets() {
        if (formController == null) {
            return;
        }

        if (formController.currentCaptionPromptIsQuestion()) {
            try {
                FormIndex startFormIndex = formController.getQuestionPrompt().getIndex();
                formController.stepToNextScreenEvent();
                while (formController.currentCaptionPromptIsQuestion()
                        && formController.getQuestionPrompt().getFormElement().getAdditionalAttribute(null, "query") != null) {
                    formController.saveAnswer(formController.getQuestionPrompt().getIndex(), null);
                    formController.stepToNextScreenEvent();
                }
                formController.jumpToIndex(startFormIndex);
            } catch (JavaRosaException e) {
                Timber.d(e);
            }
        }
    }

    public void setListener(AdvanceToNextListener listener) {
        this.listener = listener;
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
    }
}
