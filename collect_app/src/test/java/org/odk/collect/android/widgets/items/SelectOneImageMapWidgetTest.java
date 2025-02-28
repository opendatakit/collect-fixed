package org.odk.collect.android.widgets.items;

import androidx.annotation.NonNull;

import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.SelectOneData;
import org.javarosa.core.model.data.helper.Selection;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.widgets.support.FormEntryPromptSelectChoiceLoader;

import java.util.List;

public class SelectOneImageMapWidgetTest extends SelectImageMapWidgetTest<SelectOneImageMapWidget, SelectOneData> {
    @NonNull
    @Override
    public SelectOneImageMapWidget createWidget() {
        return new SelectOneImageMapWidget(activity, new QuestionDetails(formEntryPrompt), false, new FormEntryPromptSelectChoiceLoader(), dependencies);
    }

    @NonNull
    @Override
    public SelectOneData getNextAnswer() {
        List<SelectChoice> selectChoices = getSelectChoices();

        int selectedIndex = Math.abs(random.nextInt()) % selectChoices.size();
        SelectChoice selectChoice = selectChoices.get(selectedIndex);

        Selection selection = new Selection(selectChoice);
        return new SelectOneData(selection);
    }
}
