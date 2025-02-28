package org.odk.collect.android.widgets.items;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import androidx.annotation.NonNull;

import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.SelectOneData;
import org.javarosa.core.model.data.helper.Selection;
import org.junit.Before;
import org.junit.Test;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.support.MockFormEntryPromptBuilder;
import org.odk.collect.android.widgets.base.QuestionWidgetTest;
import org.odk.collect.android.widgets.support.FormEntryPromptSelectChoiceLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

public class LikertWidgetTest extends QuestionWidgetTest<LikertWidget, SelectOneData> {
    private List<SelectChoice> options = new ArrayList<>();

    @Before
    public void setup() {
        options = asList(
                new SelectChoice("1", "1"),
                new SelectChoice("2", "2"),
                new SelectChoice("3", "3"),
                new SelectChoice("4", "4"),
                new SelectChoice("5", "5"));

        formEntryPrompt = new MockFormEntryPromptBuilder()
                .withIndex("i am index")
                .withSelectChoices(options)
                .build();
    }

    @NonNull
    @Override
    public LikertWidget createWidget() {
        return new LikertWidget(activity, new QuestionDetails(formEntryPrompt), new FormEntryPromptSelectChoiceLoader(), dependencies);
    }

    @NonNull
    @Override
    public SelectOneData getNextAnswer() {
        return new SelectOneData(new Selection(options.get(0)));
    }

    @Test
    public void usingReadOnlyOptionShouldMakeAllClickableElementsDisabled() {
        when(formEntryPrompt.isReadOnly()).thenReturn(true);

        for (int i = 0; i < getSpyWidget().view.getChildCount(); i++) {
            LinearLayout optionView = (LinearLayout) getSpyWidget().view.getChildAt(0);
            assertThat(optionView.getVisibility(), is(View.VISIBLE));
            assertThat(optionView.isEnabled(), is(Boolean.FALSE));
        }

        for (Map.Entry<RadioButton, String> radioButtonStringEntry : getSpyWidget().buttonsToName.entrySet()) {
            assertThat(((RadioButton) ((Map.Entry) radioButtonStringEntry).getKey()).getVisibility(), is(View.VISIBLE));
            assertThat(((RadioButton) ((Map.Entry) radioButtonStringEntry).getKey()).isEnabled(), is(Boolean.FALSE));
        }
    }
}
