package org.odk.collect.android.widgets;

import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.odk.collect.android.utilities.Appearances.THOUSANDS_SEP;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.promptWithAppearance;

import android.text.InputType;
import android.view.View;

import androidx.annotation.NonNull;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.javarosa.core.model.Constants;
import org.javarosa.core.model.data.DecimalData;
import org.javarosa.core.model.data.IAnswerData;
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

import java.text.NumberFormat;
import java.util.Locale;

/**
 * @author James Knight
 */

public class ExDecimalWidgetTest extends GeneralExStringWidgetTest<ExDecimalWidget, DecimalData> {

    @Mock
    IAnswerData answerData;

    @Mock
    StringRequester stringRequester;

    @NonNull
    @Override
    public ExDecimalWidget createWidget() {
        when(formEntryPrompt.getDataType()).thenReturn(Constants.DATATYPE_DECIMAL);
        return new ExDecimalWidget(activity, new QuestionDetails(formEntryPrompt), new FakeWaitingForDataRegistry(), stringRequester);
    }

    @NonNull
    @Override
    public DecimalData getNextAnswer() {
        // Need to keep under 15 digits:
        double d = random.nextDouble();
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setMaximumFractionDigits(13); // The Widget internally truncatest this further.
        nf.setMaximumIntegerDigits(13);
        nf.setGroupingUsed(false);

        String formattedValue = nf.format(d);
        return new DecimalData(Double.parseDouble(formattedValue));
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        when(formEntryPrompt.getAppearanceHint()).thenReturn("");
    }

    @Test
    // This should never be possible because the EditText has a limit on it
    public void digitsAboveLimitOfFifteenShouldBeTruncatedFromRight() {
        Double eighteenDigitDouble = 9999999999999994.;
        String fifteenDigitString = "999999999999994";
        assertSame(15, fifteenDigitString.length());

        when(formEntryPrompt.getAnswerValue()).thenReturn(answerData);
        when(answerData.getValue()).thenReturn(eighteenDigitDouble);

        ExDecimalWidget exDecimalWidget = new ExDecimalWidget(activity, new QuestionDetails(formEntryPrompt), new FakeWaitingForDataRegistry(), stringRequester);

        assertThat(exDecimalWidget.binding.widgetAnswerText.getAnswer(), is(equalTo(fifteenDigitString)));

        exDecimalWidget = new ExDecimalWidget(activity, new QuestionDetails(formEntryPrompt), new FakeWaitingForDataRegistry(), stringRequester);

        assertThat(exDecimalWidget.binding.widgetAnswerText.getAnswer(), is(equalTo(fifteenDigitString)));
    }

    @Test
    public void separatorsShouldBeAddedWhenEnabled() {
        when(formEntryPrompt.getAppearanceHint()).thenReturn(THOUSANDS_SEP);
        getWidget().binding.widgetAnswerText.setAnswer("123456789.54");

        assertEquals("123,456,789.54", getWidget().binding.widgetAnswerText.getAnswer());
        assertEquals("123,456,789.54", getWidget().binding.widgetAnswerText.getBinding().editText.getText().toString());
        assertEquals("123,456,789.54", getWidget().binding.widgetAnswerText.getBinding().textView.getText().toString());
    }

    @Override
    @Test
    public void verifyInputType() {
        ExDecimalWidget widget = getWidget();
        assertThat(widget.binding.widgetAnswerText.getBinding().editText.getInputType(), equalTo(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_FLAG_DECIMAL));
        assertThat(widget.binding.widgetAnswerText.getBinding().editText.getTransformationMethod(), equalTo(null));
        assertThat(widget.binding.widgetAnswerText.getBinding().textView.getTransformationMethod(), equalTo(null));
    }

    @Override
    @Test
    public void whenPromptHasHiddenAnswerAppearance_answerIsNotDisplayed() {
        FormEntryPrompt prompt = new MockFormEntryPromptBuilder(promptWithAppearance(Appearances.HIDDEN_ANSWER))
                .withAnswer(new DecimalData(10.5))
                .build();

        WidgetTestActivity widgetTestActivity = QuestionWidgetHelpers.widgetTestActivity();
        ExDecimalWidget widget = new ExDecimalWidget(widgetTestActivity, new QuestionDetails(prompt),
                new FakeWaitingForDataRegistry(), stringRequester);

        // Check initial value is not shown
        assertThat(widget.binding.widgetAnswerText.getVisibility(), Matchers.equalTo(View.GONE));
        assertThat(widget.getAnswer(), CoreMatchers.equalTo(new DecimalData(10.5)));

        // Check updates aren't shown
        widget.setData(15.5);
        assertThat(widget.binding.widgetAnswerText.getVisibility(), Matchers.equalTo(View.GONE));
        assertThat(widget.getAnswer(), CoreMatchers.equalTo(new DecimalData(15.5)));
    }
}
