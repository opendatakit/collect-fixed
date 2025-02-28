package org.odk.collect.android.widgets;

import android.content.ComponentName;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.view.View;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.odk.collect.android.activities.BearingActivity;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.listeners.WidgetValueChangedListener;
import org.odk.collect.android.support.WidgetTestActivity;
import org.odk.collect.android.utilities.ApplicationConstants;
import org.odk.collect.android.widgets.support.FakeWaitingForDataRegistry;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowToast;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.mockValueChangedListener;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.promptWithAnswer;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.promptWithReadOnly;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.widgetDependencies;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.widgetTestActivity;
import static org.robolectric.Shadows.shadowOf;

/**
 * @author James Knight
 */
@RunWith(AndroidJUnit4.class)
public class BearingWidgetTest {
    private final FakeWaitingForDataRegistry fakeWaitingForDataRegistry = new FakeWaitingForDataRegistry();

    private WidgetTestActivity widgetActivity;
    private ShadowActivity shadowActivity;
    private SensorManager sensorManager;
    private View.OnLongClickListener listener;

    @Before
    public void setUp() {
        widgetActivity = widgetTestActivity();
        shadowActivity = shadowOf(widgetActivity);

        sensorManager = mock(SensorManager.class);
        listener = mock(View.OnLongClickListener.class);

        Sensor sensor = mock(Sensor.class);
        when(sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)).thenReturn(sensor);
        when(sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)).thenReturn(sensor);
    }

    @Test
    public void usingReadOnlyOption_hidesBearingButton() {
        assertThat(createWidget(promptWithReadOnly()).binding.bearingButton.getVisibility(), is(View.GONE));
    }

    @Test
    public void whenPromptDoesNotHaveAnswer_getBearingButtonIsShown() {
        assertThat(createWidget(promptWithAnswer(null)).binding.bearingButton.getText(),
                is(widgetActivity.getString(org.odk.collect.strings.R.string.get_bearing)));
    }

    @Test
    public void whenPromptHasAnswer_replaceBearingButtonIsShown() {
        assertThat(createWidget(promptWithAnswer(new StringData("blah"))).binding.bearingButton.getText(),
                is(widgetActivity.getString(org.odk.collect.strings.R.string.replace_bearing)));
    }

    @Test
    public void whenPromptHasAnswer_answerTextViewShowsCorrectAnswer() {
        assertThat(createWidget(promptWithAnswer(new StringData("blah"))).binding.widgetAnswerText.getAnswer(), is("blah"));
    }

    @Test
    public void getAnswer_whenPromptAnswerDoesNotHaveAnswer_returnsNull() {
        assertThat(createWidget(promptWithAnswer(null)).getAnswer(), nullValue());
    }

    @Test
    public void getAnswer_whenPromptHasAnswer_returnsAnswer() {
        assertThat(createWidget(promptWithAnswer(new StringData("blah"))).getAnswer().getDisplayText(), is("blah"));
    }

    @Test
    public void clearAnswer_clearsWidgetAnswer() {
        BearingWidget widget = createWidget(promptWithAnswer(new StringData("blah")));
        widget.clearAnswer();
        assertThat(widget.binding.widgetAnswerText.getAnswer(), is(""));
    }

    @Test
    public void clearAnswer_updatesButtonLabel() {
        BearingWidget widget = createWidget(promptWithAnswer(new StringData("blah")));
        widget.clearAnswer();
        assertThat(widget.binding.bearingButton.getText(), is(widgetActivity.getString(org.odk.collect.strings.R.string.get_bearing)));
    }

    @Test
    public void clearAnswer_callsValueChangeListeners() {
        BearingWidget widget = createWidget(promptWithAnswer(new StringData("blah")));
        WidgetValueChangedListener valueChangedListener = mockValueChangedListener(widget);

        widget.clearAnswer();
        verify(valueChangedListener).widgetValueChanged(widget);
    }

    @Test
    public void setData_updatesWidgetAnswer() {
        BearingWidget widget = createWidget(promptWithAnswer(null));
        widget.setData("blah");
        assertThat(widget.binding.widgetAnswerText.getAnswer(), is("blah"));
    }

    @Test
    public void setData_updatesButtonLabel() {
        BearingWidget widget = createWidget(promptWithAnswer(null));
        widget.setData("blah");
        assertThat(widget.binding.bearingButton.getText(), is(widgetActivity.getString(org.odk.collect.strings.R.string.replace_bearing)));
    }

    @Test
    public void setData_callsValueChangeListeners() {
        BearingWidget widget = createWidget(promptWithAnswer(null));
        WidgetValueChangedListener valueChangedListener = mockValueChangedListener(widget);

        widget.setData("blah");
        verify(valueChangedListener).widgetValueChanged(widget);
    }

    @Test
    public void clickingAnswerTextForLong_callsOnLongClickListener() {
        BearingWidget widget = createWidget(promptWithAnswer(null));
        widget.setOnLongClickListener(listener);

        widget.binding.widgetAnswerText.performLongClick();
        verify(listener).onLongClick(widget.binding.widgetAnswerText);
    }

    @Test
    public void clickingBearingButtonForLong_callOnLongClickListener() {
        BearingWidget widget = createWidget(promptWithAnswer(null));
        widget.setOnLongClickListener(listener);

        widget.binding.bearingButton.performLongClick();
        verify(listener).onLongClick(widget.binding.bearingButton);
    }

    @Test
    public void clickingBearingButtonForLong_whenSensorIsAvailable_callsOnLongClickListener() {
        BearingWidget widget = createWidget(promptWithAnswer(null));
        widget.setOnLongClickListener(listener);

        widget.binding.bearingButton.performLongClick();
        verify(listener).onLongClick(widget.binding.bearingButton);
    }

    @Test
    public void clickingBearingButton_whenAccelerometerSensorIsNotAvailable_doesNotLaunchAnyIntent() {
        assertNoIntentLaunchedWhenSensorIsUnavailable(Sensor.TYPE_ACCELEROMETER);
    }

    @Test
    public void clickingBearingButton_whenAccelerometerSensorIsNotAvailable_disablesBearingButton() {
        assertBearingButtonIsDisabledWhenSensorIsUnavailable(Sensor.TYPE_ACCELEROMETER);
    }

    @Test
    public void clickingBearingButton_whenAccelerometerSensorIsNotAvailable_makesEditTextEditable() {
        assertAnswerTextIsEditableWhenSensorIsUnavailable(Sensor.TYPE_ACCELEROMETER);
    }

    @Test
    public void clickingBearingButton_whenMagneticSensorIsNotAvailable_doesNotLaunchAnyIntent() {
        assertNoIntentLaunchedWhenSensorIsUnavailable(Sensor.TYPE_MAGNETIC_FIELD);
    }

    @Test
    public void clickingBearingButton_whenMagneticSensorIsNotAvailable_disablesBearingButton() {
        assertBearingButtonIsDisabledWhenSensorIsUnavailable(Sensor.TYPE_MAGNETIC_FIELD);
    }

    @Test
    public void clickingBearingButton_whenMagneticSensorIsNotAvailable_makesEditTextEditable() {
        assertAnswerTextIsEditableWhenSensorIsUnavailable(Sensor.TYPE_MAGNETIC_FIELD);
    }

    @Test
    public void clickingBearingButton_whenSensorIsAvailable_setsWidgetWaitingForData() {
        FormEntryPrompt prompt = promptWithAnswer(null);
        FormIndex formIndex = mock(FormIndex.class);
        when(prompt.getIndex()).thenReturn(formIndex);

        BearingWidget widget = createWidget(prompt);
        widget.binding.bearingButton.performClick();
        assertThat(fakeWaitingForDataRegistry.waiting.contains(formIndex), is(true));
    }

    @Test
    public void clickingBearingButton_whenSensorIsAvailable_launchesCorrectIntent() {
        BearingWidget widget = createWidget(promptWithAnswer(null));
        widget.binding.bearingButton.performClick();

        assertThat(shadowActivity.getNextStartedActivity().getComponent(), is(new ComponentName(widgetActivity, BearingActivity.class)));
        assertThat(shadowActivity.getNextStartedActivityForResult().requestCode, is(ApplicationConstants.RequestCodes.BEARING_CAPTURE));
    }

    private BearingWidget createWidget(FormEntryPrompt prompt) {
        return new BearingWidget(widgetActivity, new QuestionDetails(prompt), fakeWaitingForDataRegistry, sensorManager, widgetDependencies());
    }

    private void assertNoIntentLaunchedWhenSensorIsUnavailable(int sensorType) {
        when(sensorManager.getDefaultSensor(sensorType)).thenReturn(null);
        BearingWidget widget = createWidget(promptWithAnswer(null));
        widget.binding.bearingButton.performClick();

        assertThat(shadowActivity.getNextStartedActivity(), nullValue());
        assertThat(ShadowToast.getTextOfLatestToast(), is(widgetActivity.getString(org.odk.collect.strings.R.string.bearing_lack_of_sensors)));
    }

    private void assertAnswerTextIsEditableWhenSensorIsUnavailable(int sensorType) {
        when(sensorManager.getDefaultSensor(sensorType)).thenReturn(null);
        BearingWidget widget = createWidget(promptWithAnswer(null));
        widget.binding.bearingButton.performClick();

        assertThat(widget.binding.widgetAnswerText.isEditableState(), is(true));
    }

    private void assertBearingButtonIsDisabledWhenSensorIsUnavailable(int sensorType) {
        when(sensorManager.getDefaultSensor(sensorType)).thenReturn(null);
        BearingWidget widget = createWidget(promptWithAnswer(null));
        widget.binding.bearingButton.performClick();

        assertThat(widget.binding.bearingButton.isEnabled(), is(false));
    }
}
