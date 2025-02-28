package org.odk.collect.android.widgets;

import androidx.annotation.NonNull;

import net.bytebuddy.utility.RandomString;

import org.javarosa.core.model.Constants;
import org.javarosa.core.model.data.StringData;
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.odk.collect.android.utilities.Appearances.MASKED;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.promptWithAppearance;

import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.View;

/**
 * @author James Knight
 */

public class ExStringWidgetTest extends GeneralExStringWidgetTest<ExStringWidget, StringData> {

    @Mock
    StringRequester stringRequester;

    @NonNull
    @Override
    public ExStringWidget createWidget() {
        when(formEntryPrompt.getDataType()).thenReturn(Constants.DATATYPE_TEXT);
        return new ExStringWidget(activity, new QuestionDetails(formEntryPrompt), new FakeWaitingForDataRegistry(), stringRequester);
    }

    @NonNull
    @Override
    public StringData getNextAnswer() {
        return new StringData(RandomString.make());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        when(formEntryPrompt.getAppearanceHint()).thenReturn("");
    }

    @Override
    @Test
    public void verifyInputType() {
        ExStringWidget widget = getWidget();
        assertThat(widget.binding.widgetAnswerText.getBinding().editText.getInputType(), equalTo(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE));
        assertThat(widget.binding.widgetAnswerText.getBinding().editText.getTransformationMethod(), equalTo(null));
        assertThat(widget.binding.widgetAnswerText.getBinding().textView.getTransformationMethod(), equalTo(null));
    }

    @Test
    public void verifyInputTypeWithMaskedAppearance() {
        when(formEntryPrompt.getAppearanceHint()).thenReturn(MASKED);
        ExStringWidget widget = getWidget();
        assertThat(widget.binding.widgetAnswerText.getBinding().editText.getInputType(), equalTo(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_VARIATION_PASSWORD));
        assertThat(widget.binding.widgetAnswerText.getBinding().editText.getTransformationMethod().getClass(), equalTo(PasswordTransformationMethod.class));
        assertThat(widget.binding.widgetAnswerText.getBinding().textView.getTransformationMethod().getClass(), equalTo(PasswordTransformationMethod.class));
    }

    @Test
    public void answersShouldNotBeMaskedIfMaskedAppearanceIsNotUsed() {
        assertThat(getSpyWidget().binding.widgetAnswerText.getBinding().editText.getTransformationMethod(), is(not(instanceOf(PasswordTransformationMethod.class))));
        assertThat(getSpyWidget().binding.widgetAnswerText.getBinding().textView.getTransformationMethod(), is(not(instanceOf(PasswordTransformationMethod.class))));
    }

    @Test
    public void answersShouldBeMaskedIfMaskedAppearanceIsUsed() {
        when(formEntryPrompt.getAppearanceHint()).thenReturn(Appearances.MASKED);

        assertThat(getSpyWidget().binding.widgetAnswerText.getBinding().editText.getTransformationMethod(), is(instanceOf(PasswordTransformationMethod.class)));
        assertThat(getSpyWidget().binding.widgetAnswerText.getBinding().textView.getTransformationMethod(), is(instanceOf(PasswordTransformationMethod.class)));
    }

    @Test
    public void whenNumberOfRowsSpecifiedEditTextShouldHaveProperNumberOfMinLines() {
        when(questionDef.getAdditionalAttribute(null, "rows")).thenReturn("5");
        assertThat(getWidget().binding.widgetAnswerText.getBinding().editText.getMinLines(), equalTo(5));
    }

    @Override
    @Test
    public void whenPromptHasHiddenAnswerAppearance_answerIsNotDisplayed() {
        FormEntryPrompt prompt = new MockFormEntryPromptBuilder(promptWithAppearance(Appearances.HIDDEN_ANSWER))
                .withAnswer(new StringData("original contents"))
                .build();

        WidgetTestActivity widgetTestActivity = QuestionWidgetHelpers.widgetTestActivity();
        ExStringWidget widget = new ExStringWidget(widgetTestActivity, new QuestionDetails(prompt),
                new FakeWaitingForDataRegistry(), stringRequester);

        // Check initial value is not shown
        assertThat(widget.binding.widgetAnswerText.getVisibility(), equalTo(View.GONE));
        assertThat(widget.getAnswer(), equalTo(new StringData("original contents")));

        // Check updates aren't shown
        widget.setData("updated contents");
        assertThat(widget.binding.widgetAnswerText.getVisibility(), equalTo(View.GONE));
        assertThat(widget.getAnswer(), equalTo(new StringData("updated contents")));
    }
}
