package org.odk.collect.android.widgets.items;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.MultipleItemsData;
import org.javarosa.core.model.data.SelectMultiData;
import org.javarosa.core.model.data.helper.Selection;
import org.junit.Test;
import org.odk.collect.android.R;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.listeners.WidgetValueChangedListener;
import org.odk.collect.android.support.MockFormEntryPromptBuilder;
import org.odk.collect.android.widgets.base.SelectWidgetTest;
import org.odk.collect.android.widgets.support.FormEntryPromptSelectChoiceLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.mockValueChangedListener;

public class RankingWidgetTest extends SelectWidgetTest<RankingWidget, MultipleItemsData> {

    @NonNull
    @Override
    public RankingWidget createWidget() {
        return new RankingWidget(activity, new QuestionDetails(formEntryPrompt), null, new FormEntryPromptSelectChoiceLoader(), dependencies);
    }

    @NonNull
    @Override
    public MultipleItemsData getNextAnswer() {
        List<SelectChoice> selectChoices = getSelectChoices();

        List<Selection> selections = new ArrayList<>();
        for (SelectChoice selectChoice : selectChoices) {
            selections.add(new Selection(selectChoice));
        }

        return new SelectMultiData(selections);
    }

    @Test
    public void usingReadOnlyOptionShouldMakeAllClickableElementsDisabled() {
        when(formEntryPrompt.isReadOnly()).thenReturn(true);

        assertThat(getSpyWidget().binding.rankItemsButton.getVisibility(), is(View.GONE));
    }

    @Test
    public void whenSpacesInUnderlyingValuesExist_shouldAppropriateWarningBeDisplayed() {
        formEntryPrompt = new MockFormEntryPromptBuilder()
                .withSelectChoices(asList(
                        new SelectChoice("a", "a a"),
                        new SelectChoice("a", "b b")
                ))
                .build();

        TextView warningTv = getWidget().findViewById(R.id.warning_text);
        assertThat(warningTv.getVisibility(), is(View.VISIBLE));
        assertThat(warningTv.getText(), is("Warning: underlying values a a, b b have spaces"));
    }

    @Test
    public void setData_callsValueChangeListener() {
        RankingWidget widget = createWidget();
        WidgetValueChangedListener valueChangedListener = mockValueChangedListener(widget);
        widget.setValueChangedListener(valueChangedListener);
        widget.setData(
                Arrays.asList(
                        new SelectChoice("a", "a a"),
                        new SelectChoice("a", "b b")
                )
        );

        verify(valueChangedListener).widgetValueChanged(widget);
    }
}
