package org.odk.collect.android.widgets.items;

import android.text.InputType;
import android.view.View;

import androidx.annotation.NonNull;

import org.javarosa.core.model.data.SelectOneData;
import org.javarosa.core.model.data.helper.Selection;
import org.junit.Test;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.listeners.WidgetValueChangedListener;
import org.odk.collect.android.widgets.base.GeneralSelectOneWidgetTest;
import org.odk.collect.android.widgets.support.FakeWaitingForDataRegistry;
import org.odk.collect.android.widgets.support.FormEntryPromptSelectChoiceLoader;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.mockValueChangedListener;

public class SelectOneMinimalWidgetTest extends GeneralSelectOneWidgetTest<SelectOneMinimalWidget> {
    @NonNull
    @Override
    public SelectOneMinimalWidget createWidget() {
        return new SelectOneMinimalWidget(activity, new QuestionDetails(formEntryPrompt), false, new FakeWaitingForDataRegistry(), new FormEntryPromptSelectChoiceLoader(), dependencies);
    }

    @Test
    public void answerView_shouldBeMultilineTextNoSuggestion() {
        assertThat(getSpyWidget().binding.answer.getInputType(), is(InputType.TYPE_CLASS_TEXT + InputType.TYPE_TEXT_FLAG_MULTI_LINE + InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS));
    }

    @Test
    public void answerView_shouldHaveSavingStateDisabled() {
        assertThat(getSpyWidget().binding.answer.isSaveEnabled(), is(false));
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
        SelectOneData answer = getInitialAnswer();
        Selection selectedChoice = (Selection) answer.getValue();
        when(formEntryPrompt.getAnswerValue()).thenReturn(answer);
        when(formEntryPrompt.getSelectItemText(selectedChoice)).thenReturn(selectedChoice.getValue());

        assertThat(getSpyWidget().binding.answer.getText().toString(), is(selectedChoice.getValue()));
    }

    @Test
    public void whenAnswerChanges_shouldAnswerLabelBeUpdated() {
        assertThat(getSpyWidget().binding.answer.getText().toString(), is("Select Answer"));

        SelectOneData answer = getInitialAnswer();
        Selection selectedChoice = (Selection) answer.getValue();
        when(formEntryPrompt.getSelectItemText(selectedChoice)).thenReturn(selectedChoice.getValue());
        getSpyWidget().setData(Collections.singletonList(selectedChoice));

        assertThat(getSpyWidget().binding.answer.getText().toString(), is(selectedChoice.getValue()));
        getSpyWidget().clearAnswer();
        assertThat(getSpyWidget().binding.answer.getText().toString(), is("Select Answer"));
    }

    @Test
    public void whenAnswerChanges_shouldValueChangeListenersBeCalled() {
        WidgetValueChangedListener valueChangedListener = mockValueChangedListener(getSpyWidget());

        SelectOneData answer = getInitialAnswer();
        Selection selectedChoice = (Selection) answer.getValue();
        getSpyWidget().setData(Collections.singletonList(selectedChoice));

        verify(valueChangedListener).widgetValueChanged(getSpyWidget());
    }
}
