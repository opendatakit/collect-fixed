package org.odk.collect.android.widgets;

import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static org.odk.collect.android.utilities.Appearances.THOUSANDS_SEP;

import android.text.InputType;

import androidx.annotation.NonNull;

import org.javarosa.core.model.Constants;
import org.javarosa.core.model.data.IntegerData;
import org.junit.Test;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.widgets.base.GeneralStringWidgetTest;

/**
 * @author James Knight
 */
public class IntegerWidgetTest extends GeneralStringWidgetTest<IntegerWidget, IntegerData> {

    @NonNull
    @Override
    public IntegerWidget createWidget() {
        when(formEntryPrompt.getDataType()).thenReturn(Constants.DATATYPE_INTEGER);
        return new IntegerWidget(activity, new QuestionDetails(formEntryPrompt, readOnlyOverride), dependencies);
    }

    @NonNull
    @Override
    public IntegerData getNextAnswer() {
        return new IntegerData(randomInteger());
    }

    private int randomInteger() {
        return Math.abs(random.nextInt()) % 1_000_000_000;
    }

    @Test
    public void digitsAboveLimitOfNineShouldBeTruncatedFromRight() {
        getWidget().widgetAnswerText.setAnswer("123456789123");
        assertEquals("123456789", getWidget().getAnswerText());
    }

    @Test
    public void separatorsShouldBeAddedWhenEnabled() {
        when(formEntryPrompt.getAppearanceHint()).thenReturn(THOUSANDS_SEP);
        getWidget().widgetAnswerText.setAnswer("123456789");

        assertEquals("123,456,789", getWidget().widgetAnswerText.getAnswer());
        assertEquals("123,456,789", getWidget().widgetAnswerText.getBinding().editText.getText().toString());
        assertEquals("123,456,789", getWidget().widgetAnswerText.getBinding().textView.getText().toString());
    }

    @Override
    @Test
    public void verifyInputType() {
        IntegerWidget widget = getWidget();
        assertThat(widget.widgetAnswerText.getBinding().editText.getInputType(), equalTo(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED));
        assertThat(widget.widgetAnswerText.getBinding().editText.getTransformationMethod(), equalTo(null));
    }
}
