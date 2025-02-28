package org.odk.collect.android.widgets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.odk.collect.android.utilities.Appearances.MASKED;

import android.text.InputType;
import android.text.method.PasswordTransformationMethod;

import androidx.annotation.NonNull;

import net.bytebuddy.utility.RandomString;

import org.javarosa.core.model.Constants;
import org.javarosa.core.model.data.StringData;
import org.junit.Test;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.utilities.Appearances;
import org.odk.collect.android.widgets.base.GeneralStringWidgetTest;

/**
 * @author James Knight
 */
public class StringWidgetTest extends GeneralStringWidgetTest<StringWidget, StringData> {

    @NonNull
    @Override
    public StringWidget createWidget() {
        when(formEntryPrompt.getDataType()).thenReturn(Constants.DATATYPE_TEXT);
        return new StringWidget(activity, new QuestionDetails(formEntryPrompt, readOnlyOverride), dependencies);
    }

    @NonNull
    @Override
    public StringData getNextAnswer() {
        return new StringData(RandomString.make());
    }

    @Override
    @Test
    public void verifyInputType() {
        StringWidget widget = getWidget();
        assertThat(widget.widgetAnswerText.getBinding().editText.getInputType(), equalTo(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE));
        assertThat(widget.widgetAnswerText.getBinding().editText.getTransformationMethod(), equalTo(null));
    }

    @Test
    public void verifyInputTypeWithMaskedAppearance() {
        when(formEntryPrompt.getAppearanceHint()).thenReturn(MASKED);
        StringWidget widget = getWidget();
        assertThat(widget.widgetAnswerText.getBinding().editText.getInputType(), equalTo(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_VARIATION_PASSWORD));
        assertThat(widget.widgetAnswerText.getBinding().editText.getTransformationMethod().getClass(), equalTo(PasswordTransformationMethod.class));
    }

    @Test
    public void answersShouldNotBeMaskedIfMaskedAppearanceIsNotUsed() {
        assertThat(getSpyWidget().widgetAnswerText.getBinding().editText.getTransformationMethod(), is(not(instanceOf(PasswordTransformationMethod.class))));
        assertThat(getSpyWidget().widgetAnswerText.getBinding().textView.getTransformationMethod(), is(not(instanceOf(PasswordTransformationMethod.class))));
    }

    @Test
    public void answersShouldBeMaskedIfMaskedAppearanceIsUsed() {
        when(formEntryPrompt.getAppearanceHint()).thenReturn(Appearances.MASKED);

        assertThat(getSpyWidget().widgetAnswerText.getBinding().editText.getTransformationMethod(), is(instanceOf(PasswordTransformationMethod.class)));
        assertThat(getSpyWidget().widgetAnswerText.getBinding().textView.getTransformationMethod(), is(instanceOf(PasswordTransformationMethod.class)));
    }

    @Test
    public void whenNumberOfRowsSpecifiedEditTextShouldHaveProperNumberOfMinLines() {
        when(questionDef.getAdditionalAttribute(null, "rows")).thenReturn("5");
        assertThat(getWidget().widgetAnswerText.getBinding().editText.getMinLines(), equalTo(5));
    }
}
