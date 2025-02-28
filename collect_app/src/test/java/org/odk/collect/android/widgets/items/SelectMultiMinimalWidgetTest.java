package org.odk.collect.android.widgets.items;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.SelectMultiData;
import org.javarosa.core.model.data.helper.Selection;
import org.junit.Test;
import org.odk.collect.android.R;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.listeners.WidgetValueChangedListener;
import org.odk.collect.android.support.MockFormEntryPromptBuilder;
import org.odk.collect.android.widgets.base.GeneralSelectMultiWidgetTest;
import org.odk.collect.android.widgets.support.FakeWaitingForDataRegistry;
import org.odk.collect.android.widgets.support.FormEntryPromptSelectChoiceLoader;

import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.mockValueChangedListener;

public class SelectMultiMinimalWidgetTest extends GeneralSelectMultiWidgetTest<SelectMultiMinimalWidget> {

    @NonNull
    @Override
    public SelectMultiMinimalWidget createWidget() {
        return new SelectMultiMinimalWidget(activity, new QuestionDetails(formEntryPrompt), new FakeWaitingForDataRegistry(), new FormEntryPromptSelectChoiceLoader(), dependencies);
    }

    @Test
    public void usingReadOnlyOptionShouldMakeAllClickableElementsDisabled() {
        when(formEntryPrompt.isReadOnly()).thenReturn(true);
        assertThat(getSpyWidget().binding.answer.getVisibility(), is(View.VISIBLE));
        assertThat(getSpyWidget().binding.answer.isEnabled(), is(Boolean.FALSE));
    }

    @Test
    public void whenThereIsNoAnswer_shouldDefaultTextBeDisplayed() {
        assertThat(getSpyWidget().binding.answer.getText().toString(), is("Select Answer"));
    }

    @Test
    public void whenThereIsAnswer_shouldSelectedChoicesBeDisplayed() {
        SelectMultiData answer = getInitialAnswer();
        Selection selectedChoice = ((List<Selection>) answer.getValue()).get(0);
        when(formEntryPrompt.getAnswerValue()).thenReturn(answer);
        when(formEntryPrompt.getSelectItemText(selectedChoice)).thenReturn(selectedChoice.getValue());

        assertThat(getSpyWidget().binding.answer.getText().toString(), is(selectedChoice.getValue()));
    }

    @Test
    public void whenAnswerChanges_shouldAnswerLabelBeUpdated() {
        assertThat(getSpyWidget().binding.answer.getText().toString(), is("Select Answer"));

        SelectMultiData answer = getInitialAnswer();
        Selection selectedChoice = ((List<Selection>) answer.getValue()).get(0);
        when(formEntryPrompt.getSelectItemText(selectedChoice)).thenReturn(selectedChoice.getValue());
        getSpyWidget().setData(Collections.singletonList(selectedChoice));

        assertThat(getSpyWidget().binding.answer.getText().toString(), is(selectedChoice.getValue()));
        getSpyWidget().clearAnswer();
        assertThat(getSpyWidget().binding.answer.getText().toString(), is("Select Answer"));
    }

    @Test
    public void whenAnswerChanges_shouldValueChangeListenersBeCalled() {
        WidgetValueChangedListener valueChangedListener = mockValueChangedListener(getSpyWidget());

        SelectMultiData answer = getInitialAnswer();
        Selection selectedChoice = ((List<Selection>) answer.getValue()).get(0);
        getSpyWidget().setData(Collections.singletonList(selectedChoice));

        verify(valueChangedListener).widgetValueChanged(getSpyWidget());
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
}
