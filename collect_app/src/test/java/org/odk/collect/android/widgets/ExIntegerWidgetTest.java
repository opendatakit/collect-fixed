package org.odk.collect.android.widgets;

import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static org.odk.collect.android.utilities.Appearances.THOUSANDS_SEP;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.promptWithAppearance;

import android.text.InputType;
import android.view.View;

import androidx.annotation.NonNull;

import org.hamcrest.Matchers;
import org.javarosa.core.model.Constants;
import org.javarosa.core.model.data.IntegerData;
import org.javarosa.form.api.FormEntryPrompt;
import org.junit.Test;
import org.mockito.Mock;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.support.MockFormEntryPromptBuilder;
import org.odk.collect.android.support.WidgetTestActivity;
import org.odk.collect.android.utilities.Appearances;
import org.odk.collect.android.widgets.base.GeneralExStringWidgetTest;
import org.odk.collect.android.widgets.support.FakeWaitingForDataRegistry;
import org.odk.collect.android.widgets.support.QuestionWidgetHelpers;
import org.odk.collect.android.widgets.utilities.StringRequester;

/**
 * @author James Knight
 */

public class ExIntegerWidgetTest extends GeneralExStringWidgetTest<ExIntegerWidget, IntegerData> {

    @Mock
    StringRequester stringRequester;

    @NonNull
    @Override
    public ExIntegerWidget createWidget() {
        when(formEntryPrompt.getDataType()).thenReturn(Constants.DATATYPE_INTEGER);
        return new ExIntegerWidget(activity, new QuestionDetails(formEntryPrompt), new FakeWaitingForDataRegistry(), stringRequester);
    }

    @NonNull
    @Override
    public IntegerData getNextAnswer() {
        return new IntegerData(randomInteger());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        when(formEntryPrompt.getAppearanceHint()).thenReturn("");
    }

    private int randomInteger() {
        return Math.abs(random.nextInt()) % 1_000_000_000;
    }

    @Test
    public void digitsAboveLimitOfNineShouldBeTruncatedFromRight() {
        getWidget().binding.widgetAnswerText.setAnswer("123456789123");
        assertEquals("123456789", getWidget().binding.widgetAnswerText.getAnswer());
    }

    @Test
    public void separatorsShouldBeAddedWhenEnabled() {
        when(formEntryPrompt.getAppearanceHint()).thenReturn(THOUSANDS_SEP);
        getWidget().binding.widgetAnswerText.setAnswer("123456789");

        assertEquals("123,456,789", getWidget().binding.widgetAnswerText.getAnswer());
        assertEquals("123,456,789", getWidget().binding.widgetAnswerText.getBinding().editText.getText().toString());
        assertEquals("123,456,789", getWidget().binding.widgetAnswerText.getBinding().textView.getText().toString());
    }

    @Override
    @Test
    public void verifyInputType() {
        ExIntegerWidget widget = getWidget();
        assertThat(widget.binding.widgetAnswerText.getBinding().editText.getInputType(), equalTo(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED));
        assertThat(widget.binding.widgetAnswerText.getBinding().editText.getTransformationMethod(), equalTo(null));
        assertThat(widget.binding.widgetAnswerText.getBinding().textView.getTransformationMethod(), equalTo(null));
    }

    @Override
    @Test
    public void whenPromptHasHiddenAnswerAppearance_answerIsNotDisplayed() {
        FormEntryPrompt prompt = new MockFormEntryPromptBuilder(promptWithAppearance(Appearances.HIDDEN_ANSWER))
                .withAnswer(new IntegerData(10))
                .build();

        WidgetTestActivity widgetTestActivity = QuestionWidgetHelpers.widgetTestActivity();
        ExIntegerWidget widget = new ExIntegerWidget(widgetTestActivity, new QuestionDetails(prompt),
                new FakeWaitingForDataRegistry(), stringRequester);

        // Check initial value is not shown
        assertThat(widget.binding.widgetAnswerText.getVisibility(), Matchers.equalTo(View.GONE));
        assertThat(widget.getAnswer(), equalTo(new IntegerData(10)));

        // Check updates aren't shown
        widget.setData(15);
        assertThat(widget.binding.widgetAnswerText.getVisibility(), Matchers.equalTo(View.GONE));
        assertThat(widget.getAnswer(), equalTo(new IntegerData(15)));
    }
}
