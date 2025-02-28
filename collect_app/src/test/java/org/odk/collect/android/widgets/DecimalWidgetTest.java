package org.odk.collect.android.widgets;

import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.odk.collect.android.utilities.Appearances.THOUSANDS_SEP;

import android.text.InputType;

import androidx.annotation.NonNull;

import org.javarosa.core.model.Constants;
import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.model.data.DecimalData;
import org.javarosa.core.model.data.IAnswerData;
import org.junit.Test;
import org.mockito.Mock;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.widgets.base.GeneralStringWidgetTest;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Random;

public class DecimalWidgetTest extends GeneralStringWidgetTest<DecimalWidget, DecimalData> {

    private final Random random = new Random();

    @Mock
    IAnswerData answerData;

    @Mock
    QuestionDef questionDef;

    @NonNull
    @Override
    public DecimalWidget createWidget() {
        when(formEntryPrompt.getDataType()).thenReturn(Constants.DATATYPE_DECIMAL);
        return new DecimalWidget(activity, new QuestionDetails(formEntryPrompt, readOnlyOverride), dependencies);
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

        when(formEntryPrompt.getLongText()).thenReturn("A decimal questionDef");
        when(formElement.getAdditionalAttribute(null, "playColor")).thenReturn(null);

        when(formEntryPrompt.getQuestion()).thenReturn(questionDef);
        when(questionDef.getAdditionalAttribute(null, "rows")).thenReturn(null);
    }

    @Test
    // Fails when double is turned to string with toString or String.format(Locale.US, "%f", d))
    public void integerValueShouldDisplayNoDecimalPoint() {
        Double integerDouble = 0.;
        String integerString = "0";
        when(formEntryPrompt.getAnswerValue()).thenReturn(answerData);
        when(answerData.getValue()).thenReturn(integerDouble);

        DecimalWidget decimalWidget = new DecimalWidget(activity, new QuestionDetails(formEntryPrompt), dependencies);

        assertThat(decimalWidget.getAnswerText(), is(equalTo(integerString)));

        decimalWidget = new DecimalWidget(activity, new QuestionDetails(formEntryPrompt), dependencies);

        assertThat(decimalWidget.getAnswerText(), is(equalTo(integerString)));
    }

    @Test
    // Fails when double is turned to string using String.format(Locale.US, "%f", d)
    // because default precision for %f is 6
    // NOTE: in the case of a decimal value with trailing 0s, it's probably not possible to maintain
    // that precision. For example, 9.00 becomes 9
    public void decimalValueShouldNotAddPrecision() {
        Double twoDecimalDouble = 9.99;
        String twoDecimalString = "9.99";
        when(formEntryPrompt.getAnswerValue()).thenReturn(answerData);
        when(answerData.getValue()).thenReturn(twoDecimalDouble);

        DecimalWidget decimalWidget = new DecimalWidget(activity, new QuestionDetails(formEntryPrompt), dependencies);

        assertThat(decimalWidget.getAnswerText(), is(equalTo(twoDecimalString)));

        decimalWidget = new DecimalWidget(activity, new QuestionDetails(formEntryPrompt), dependencies);

        assertThat(decimalWidget.getAnswerText(), is(equalTo(twoDecimalString)));
    }

    @Test
    // Fails when double is turned to string with toString or String.format(Locale.US, "%f", d)
    public void negativeIntegerShouldDisplayNegativeWithNoDecimalPoint() {
        Double negativeIntegerDouble = -999.;
        String negativeIntegerString = "-999";
        when(formEntryPrompt.getAnswerValue()).thenReturn(answerData);
        when(answerData.getValue()).thenReturn(negativeIntegerDouble);

        DecimalWidget decimalWidget = new DecimalWidget(activity, new QuestionDetails(formEntryPrompt), dependencies);

        assertThat(decimalWidget.getAnswerText(), is(equalTo(negativeIntegerString)));

        decimalWidget = new DecimalWidget(activity, new QuestionDetails(formEntryPrompt), dependencies);

        assertThat(decimalWidget.getAnswerText(), is(equalTo(negativeIntegerString)));
    }

    @Test
    // Fails when double is turned to string with toString because of scientific notation
    // https://docs.oracle.com/javase/7/docs/api/java/lang/Double.html#toString(double)
    public void fifteenDigitValueShouldDisplayAllDigits() {
        Double fifteenDigitDouble = 999999999999999.;
        String fifteenDigitString = "999999999999999";
        assertSame(15, fifteenDigitString.length());

        when(formEntryPrompt.getAnswerValue()).thenReturn(answerData);
        when(answerData.getValue()).thenReturn(fifteenDigitDouble);

        DecimalWidget decimalWidget = new DecimalWidget(activity, new QuestionDetails(formEntryPrompt), dependencies);

        assertThat(decimalWidget.getAnswerText(), is(equalTo(fifteenDigitString)));

        decimalWidget = new DecimalWidget(activity, new QuestionDetails(formEntryPrompt), dependencies);

        assertThat(decimalWidget.getAnswerText(), is(equalTo(fifteenDigitString)));
    }

    @Test
    // Fails when double is turned to string with toString because of scientific notation
    // https://docs.oracle.com/javase/7/docs/api/java/lang/Double.html#toString(double)
    public void fifteenDigitNegativeValueShouldDisplayAllDigits() {
        Double fifteenDigitNegativeDouble = -99999999999999.;
        String fifteenDigitNegativeString = "-99999999999999";
        assertSame(15, fifteenDigitNegativeString.length());

        when(formEntryPrompt.getAnswerValue()).thenReturn(answerData);
        when(answerData.getValue()).thenReturn(fifteenDigitNegativeDouble);

        DecimalWidget decimalWidget = new DecimalWidget(activity, new QuestionDetails(formEntryPrompt), dependencies);

        assertThat(decimalWidget.getAnswerText(), is(equalTo(fifteenDigitNegativeString)));

        decimalWidget = new DecimalWidget(activity, new QuestionDetails(formEntryPrompt), dependencies);

        assertThat(decimalWidget.getAnswerText(), is(equalTo(fifteenDigitNegativeString)));
    }

    @Test
    // Fails when double is turned to string using String.format(Locale.US, "%f", d) because default
    // precision for %f is 6
    public void fifteenDigitDecimalValueShouldDisplayAllDigits() {
        Double fifteenDigitDecimalDouble = 0.9999999999999;
        String fifteenDigitDecimalString = "0.9999999999999";
        assertSame(15, fifteenDigitDecimalString.length());

        when(formEntryPrompt.getAnswerValue()).thenReturn(answerData);
        when(answerData.getValue()).thenReturn(fifteenDigitDecimalDouble);

        DecimalWidget decimalWidget = new DecimalWidget(activity, new QuestionDetails(formEntryPrompt), dependencies);

        assertThat(decimalWidget.getAnswerText(), is(equalTo(fifteenDigitDecimalString)));

        decimalWidget = new DecimalWidget(activity, new QuestionDetails(formEntryPrompt), dependencies);

        assertThat(decimalWidget.getAnswerText(), is(equalTo(fifteenDigitDecimalString)));
    }

    @Test
    // This should never be possible because the EditText has a limit on it
    public void digitsAboveLimitOfFifteenShouldBeTruncatedFromRight() {
        Double eighteenDigitDouble = 9999999999999994.;
        String fifteenDigitString = "999999999999994";
        assertSame(15, fifteenDigitString.length());

        when(formEntryPrompt.getAnswerValue()).thenReturn(answerData);
        when(answerData.getValue()).thenReturn(eighteenDigitDouble);

        DecimalWidget decimalWidget = new DecimalWidget(activity, new QuestionDetails(formEntryPrompt), dependencies);

        assertThat(decimalWidget.getAnswerText(), is(equalTo(fifteenDigitString)));

        decimalWidget = new DecimalWidget(activity, new QuestionDetails(formEntryPrompt), dependencies);

        assertThat(decimalWidget.getAnswerText(), is(equalTo(fifteenDigitString)));
    }

    @Test
    public void separatorsShouldBeAddedWhenEnabled() {
        when(formEntryPrompt.getAppearanceHint()).thenReturn(THOUSANDS_SEP);
        getWidget().widgetAnswerText.setAnswer("123456789.54");

        assertEquals("123,456,789.54", getWidget().widgetAnswerText.getAnswer());
        assertEquals("123,456,789.54", getWidget().widgetAnswerText.getBinding().editText.getText().toString());
        assertEquals("123,456,789.54", getWidget().widgetAnswerText.getBinding().textView.getText().toString());
    }

    @Override
    @Test
    public void verifyInputType() {
        DecimalWidget widget = getWidget();
        assertThat(widget.widgetAnswerText.getBinding().editText.getInputType(), equalTo(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_FLAG_DECIMAL));
        assertThat(widget.widgetAnswerText.getBinding().editText.getTransformationMethod(), equalTo(null));
    }
}
