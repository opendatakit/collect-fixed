package org.odk.collect.android.widgets;

import android.view.View;
import android.widget.CheckBox;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.listeners.WidgetValueChangedListener;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.mockValueChangedListener;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.promptWithAnswer;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.promptWithReadOnly;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.widgetDependencies;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.widgetTestActivity;

@RunWith(AndroidJUnit4.class)
public class TriggerWidgetTest {

    @Test
    public void getAnswer_whenPromptAnswerDoesNotHaveAnswer_returnsNull() {
        assertThat(createWidget(promptWithAnswer(null)).getAnswer(), nullValue());
    }

    @Test
    public void getAnswer_whenPromptHasAnswer_returnsAnswer() {
        TriggerWidget widget = createWidget(promptWithAnswer(new StringData("OK")));
        assertThat(widget.getAnswer().getDisplayText(), equalTo("OK"));
    }

    @Test
    public void clearAnswer_clearsWidgetAnswer() {
        TriggerWidget widget = createWidget(promptWithAnswer(new StringData("OK")));

        widget.clearAnswer();
        assertThat(widget.getAnswer(), nullValue());
    }

    @Test
    public void usingReadOnlyOption_makesAllClickableElementsDisabled() {
        TriggerWidget widget = createWidget(promptWithReadOnly());
        assertThat(widget.getCheckBox().getVisibility(), equalTo(View.VISIBLE));
        assertThat(widget.getCheckBox().isEnabled(), equalTo(Boolean.FALSE));
    }

    @Test
    public void whenPromptAnswerDoesNotHaveAnswer_checkboxIsUnchecked() {
        TriggerWidget widget = createWidget(promptWithAnswer(null));
        assertThat(widget.getCheckBox().isChecked(), equalTo(false));
    }

    @Test
    public void whenPromptHasAnswer_checkboxIsChecked() {
        TriggerWidget widget = createWidget(promptWithAnswer(new StringData("OK")));
        assertThat(widget.getCheckBox().isChecked(), equalTo(true));
    }

    @Test
    public void checkingCheckbox_setsAnswer() {
        TriggerWidget widget = createWidget(promptWithAnswer(null));
        CheckBox triggerButton = widget.getCheckBox();

        triggerButton.setChecked(true);
        assertThat(widget.getAnswer().getDisplayText(), equalTo("OK"));

        triggerButton.setChecked(false);
        assertThat(widget.getAnswer(), nullValue());
    }

    @Test
    public void checkingCheckbox_callsValueChangeListeners() {
        TriggerWidget widget = createWidget(promptWithAnswer(null));
        WidgetValueChangedListener valueChangedListener = mockValueChangedListener(widget);
        CheckBox triggerButton = widget.getCheckBox();

        triggerButton.setChecked(true);
        verify(valueChangedListener).widgetValueChanged(widget);
    }

    @Test // https://github.com/getodk/collect/issues/5523
    public void everyTriggerWidgetShouldHaveCheckboxWithUniqueID() {
        TriggerWidget widget1 = createWidget(promptWithAnswer(new StringData("OK")));
        TriggerWidget widget2 = createWidget(promptWithAnswer(new StringData("OK")));

        assertThat(widget1.getCheckBox().getId(), not(equalTo(widget2.getCheckBox().getId())));
    }

    private TriggerWidget createWidget(FormEntryPrompt prompt) {
        return new TriggerWidget(widgetTestActivity(), new QuestionDetails(prompt), widgetDependencies());
    }
}
