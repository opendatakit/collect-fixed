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

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.SelectMultiData;
import org.javarosa.core.model.data.helper.Selection;
import org.odk.collect.android.adapters.AbstractSelectListAdapter;
import org.odk.collect.android.adapters.SelectMultipleListAdapter;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.utilities.Appearances;
import org.odk.collect.android.widgets.interfaces.SelectChoiceLoader;
import org.odk.collect.android.widgets.warnings.SpacesInUnderlyingValuesWarning;

import java.util.ArrayList;
import java.util.List;

/**
 * SelectMultiWidget handles multiple selection fields using checkboxes.
 *
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
@SuppressLint("ViewConstructor")
public class SelectMultiWidget extends BaseSelectListWidget {
    public SelectMultiWidget(Context context, QuestionDetails prompt, SelectChoiceLoader selectChoiceLoader, Dependencies dependencies) {
        super(context, prompt, selectChoiceLoader, dependencies);
        SpacesInUnderlyingValuesWarning
                .forQuestionWidget(this)
                .renderWarningIfNecessary(items);
    }

    @Override
    protected AbstractSelectListAdapter setUpAdapter() {
        int numColumns = Appearances.getNumberOfColumns(getFormEntryPrompt(), screenUtils);
        boolean noButtonsMode = Appearances.isCompactAppearance(getFormEntryPrompt()) || Appearances.isNoButtonsAppearance(getFormEntryPrompt());

        recyclerViewAdapter = new SelectMultipleListAdapter(getSelectedItems(), this, getContext(),
                items, getFormEntryPrompt(), getReferenceManager(), audioPlayer,
                getPlayColor(getFormEntryPrompt(), themeUtils), numColumns, noButtonsMode, mediaUtils);
        return recyclerViewAdapter;
    }

    @Override
    public IAnswerData getAnswer() {
        List<Selection> selectedItems = recyclerViewAdapter.getSelectedItems();
        return selectedItems.isEmpty()
                ? null
                : new SelectMultiData(selectedItems);
    }

    @Override
    public void setChoiceSelected(int choiceIndex, boolean isSelected) {
        if (isSelected) {
            ((SelectMultipleListAdapter) recyclerViewAdapter).addItem(items.get(choiceIndex).selection());
        } else {
            ((SelectMultipleListAdapter) recyclerViewAdapter).removeItem(items.get(choiceIndex).selection());
        }
    }

    private List<Selection> getSelectedItems() {
        return getFormEntryPrompt().getAnswerValue() == null
                ? new ArrayList<>() :
                (List<Selection>) getFormEntryPrompt().getAnswerValue().getValue();
    }

    @Override
    public void onItemClicked() {
        widgetValueChanged();
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
    }
}
