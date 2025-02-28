package org.odk.collect.android.widgets.items;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.common.collect.ImmutableList;

import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.SelectMultiData;
import org.javarosa.core.model.data.helper.Selection;
import org.junit.Test;
import org.odk.collect.android.R;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.support.MockFormEntryPromptBuilder;
import org.odk.collect.android.widgets.support.FormEntryPromptSelectChoiceLoader;

import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class SelectMultiImageMapWidgetTest extends SelectImageMapWidgetTest<SelectMultiImageMapWidget, SelectMultiData> {
    @NonNull
    @Override
    public SelectMultiImageMapWidget createWidget() {
        return new SelectMultiImageMapWidget(activity, new QuestionDetails(formEntryPrompt), new FormEntryPromptSelectChoiceLoader(), dependencies);
    }

    @NonNull
    @Override
    public SelectMultiData getNextAnswer() {
        List<SelectChoice> selectChoices = getSelectChoices();

        int selectedIndex = Math.abs(random.nextInt()) % selectChoices.size();
        SelectChoice selectChoice = selectChoices.get(selectedIndex);

        Selection selection = new Selection(selectChoice);
        return new SelectMultiData(ImmutableList.of(selection));
    }

    @Test
    public void whenSpacesInUnderlyingValuesExist_shouldAppropriateWarningBeDisplayed() {
        formEntryPrompt = new MockFormEntryPromptBuilder()
                .withSelectChoices(asList(
                        new SelectChoice("a", "a a"),
                        new SelectChoice("a", "b b")
                ))
                .withImageURI("jr://images/body.svg")
                .build();

        TextView warningTv = getWidget().findViewById(R.id.warning_text);
        assertThat(warningTv.getVisibility(), is(View.VISIBLE));
        assertThat(warningTv.getText(), is("Warning: underlying values a a, b b have spaces"));
    }
}
