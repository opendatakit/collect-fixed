package org.odk.collect.android.widgets;

import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;
import static org.odk.collect.android.utilities.Appearances.THOUSANDS_SEP;

import android.text.InputType;

import androidx.annotation.NonNull;

import net.bytebuddy.utility.RandomString;

import org.javarosa.core.model.Constants;
import org.javarosa.core.model.data.StringData;
import org.junit.Test;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.widgets.base.GeneralStringWidgetTest;

/**
 * @author James Knight
 */
public class StringNumberWidgetTest extends GeneralStringWidgetTest<StringNumberWidget, StringData> {

    @NonNull
    @Override
    public StringNumberWidget createWidget() {
        when(formEntryPrompt.getDataType()).thenReturn(Constants.DATATYPE_TEXT);
        return new StringNumberWidget(activity, new QuestionDetails(formEntryPrompt, readOnlyOverride), dependencies);
    }

    @NonNull
    @Override
    public StringData getNextAnswer() {
        return new StringData(RandomString.make());
    }

    @Test
    public void digitsNumberShouldNotBeLimited() {
        getWidget().widgetAnswerText.setAnswer("123456789123456789123456789123456789");
        assertEquals("123456789123456789123456789123456789", getWidget().getAnswerText());
    }

    @Test
    public void separatorsShouldBeAddedWhenEnabled() {
        when(formEntryPrompt.getAppearanceHint()).thenReturn(THOUSANDS_SEP);
        getWidget().widgetAnswerText.setAnswer("123456789123456789123456789123456789");

        assertEquals("123,456,789,123,456,789,123,456,789,123,456,789", getWidget().widgetAnswerText.getAnswer());
        assertEquals("123,456,789,123,456,789,123,456,789,123,456,789", getWidget().widgetAnswerText.getBinding().editText.getText().toString());
        assertEquals("123,456,789,123,456,789,123,456,789,123,456,789", getWidget().widgetAnswerText.getBinding().textView.getText().toString());
    }

    @Override
    @Test
    public void verifyInputType() {
        StringNumberWidget widget = getWidget();
        assertThat(widget.widgetAnswerText.getBinding().editText.getInputType(), equalTo(InputType.TYPE_CLASS_NUMBER));
        assertThat(widget.widgetAnswerText.getBinding().editText.getTransformationMethod(), equalTo(null));
    }
}
