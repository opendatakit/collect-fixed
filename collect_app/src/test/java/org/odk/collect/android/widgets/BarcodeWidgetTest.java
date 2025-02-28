package org.odk.collect.android.widgets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.mockValueChangedListener;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.promptWithAnswer;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.promptWithAppearance;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.promptWithReadOnly;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.widgetDependencies;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.widgetTestActivity;
import static org.robolectric.Shadows.shadowOf;

import android.view.View;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.odk.collect.android.fakes.FakePermissionsProvider;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.listeners.WidgetValueChangedListener;
import org.odk.collect.android.support.MockFormEntryPromptBuilder;
import org.odk.collect.android.support.WidgetTestActivity;
import org.odk.collect.android.utilities.Appearances;
import org.odk.collect.android.widgets.support.FakeWaitingForDataRegistry;
import org.odk.collect.androidshared.system.CameraUtils;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowToast;

/**
 * @author James Knight
 */
@RunWith(AndroidJUnit4.class)
public class BarcodeWidgetTest {
    private final FakeWaitingForDataRegistry waitingForDataRegistry = new FakeWaitingForDataRegistry();
    private final FakePermissionsProvider permissionsProvider = new FakePermissionsProvider();

    private WidgetTestActivity widgetTestActivity;
    private ShadowActivity shadowActivity;
    private CameraUtils cameraUtils;
    private View.OnLongClickListener listener;
    private FormIndex formIndex;

    @Before
    public void setUp() {
        widgetTestActivity = widgetTestActivity();
        shadowActivity = shadowOf(widgetTestActivity);

        cameraUtils = mock(CameraUtils.class);
        listener = mock(View.OnLongClickListener.class);
        formIndex = mock(FormIndex.class);
        permissionsProvider.setPermissionGranted(true);
    }

    @Test
    public void usingReaDOnly_shouldHideBarcodeButton() {
        assertThat(createWidget(promptWithReadOnly()).binding.barcodeButton.getVisibility(), equalTo(View.GONE));
    }

    @Test
    public void whenPromptHasAnswer_replaceBarcodeButtonIsDisplayed() {
        BarcodeWidget widget = createWidget(promptWithAnswer(new StringData("blah")));
        assertThat(widget.binding.barcodeButton.getText().toString(), equalTo(widgetTestActivity.getString(org.odk.collect.strings.R.string.replace_barcode)));
    }

    @Test
    public void whenPromptHasAnswer_answerTextViewShowsCorrectAnswer() {
        BarcodeWidget widget = createWidget(promptWithAnswer(new StringData("blah")));
        assertThat(widget.binding.barcodeAnswerText.getText().toString(), equalTo("blah"));
    }

    @Test
    public void getAnswer_whenPromptDoesNotHaveAnswer_returnsNull() {
        BarcodeWidget widget = createWidget(promptWithAnswer(null));
        assertThat(widget.getAnswer(), nullValue());
    }

    @Test
    public void getAnswer_whenPromptHasAnswer_returnsCorrectAnswer() {
        BarcodeWidget widget = createWidget(promptWithAnswer(new StringData("blah")));
        assertThat(widget.getAnswer().getDisplayText(), equalTo("blah"));
    }

    @Test
    public void clearAnswer_clearsWidgetAnswer() {
        BarcodeWidget widget = createWidget(promptWithAnswer(new StringData("blah")));
        widget.clearAnswer();

        assertThat(widget.binding.barcodeAnswerText.getText().toString(), equalTo(""));
        assertThat(widget.binding.barcodeButton.getText().toString(), equalTo(widgetTestActivity.getString(org.odk.collect.strings.R.string.get_barcode)));
    }

    @Test
    public void clearAnswer_callsValueChangeListener() {
        BarcodeWidget widget = createWidget(promptWithAnswer(new StringData("blah")));
        WidgetValueChangedListener valueChangedListener = mockValueChangedListener(widget);
        widget.clearAnswer();

        verify(valueChangedListener).widgetValueChanged(widget);
    }

    @Test
    public void setData_updatesWidgetAnswer_afterStrippingInvalidCharacters() {
        BarcodeWidget widget = createWidget(promptWithAnswer(null));
        widget.setData("\ud800blah\b");
        assertThat(widget.binding.barcodeAnswerText.getText().toString(), equalTo("blah"));
    }

    @Test
    public void setData_updatesButtonLabel() {
        BarcodeWidget widget = createWidget(promptWithAnswer(null));
        widget.setData("\ud800blah\b");
        assertThat(widget.binding.barcodeButton.getText(), equalTo(widgetTestActivity.getString(org.odk.collect.strings.R.string.replace_barcode)));
    }

    @Test
    public void setData_callsValueChangeListener() {
        BarcodeWidget widget = createWidget(promptWithAnswer(null));
        WidgetValueChangedListener valueChangedListener = mockValueChangedListener(widget);
        widget.setData("blah");

        verify(valueChangedListener).widgetValueChanged(widget);
    }

    @Test
    public void clickingButtonAndAnswerTextViewForLong_callsLongClickListener() {
        BarcodeWidget widget = createWidget(promptWithAnswer(null));
        widget.setOnLongClickListener(listener);
        widget.binding.barcodeButton.performLongClick();
        widget.binding.barcodeAnswerText.performLongClick();

        verify(listener).onLongClick(widget.binding.barcodeButton);
        verify(listener).onLongClick(widget.binding.barcodeAnswerText);
    }

    @Test
    public void clickingBarcodeButton_whenPermissionIsNotGranted_doesNotLaunchAnyIntent() {
        BarcodeWidget widget = createWidget(promptWithAnswer(null));
        permissionsProvider.setPermissionGranted(false);
        widget.setPermissionsProvider(permissionsProvider);
        widget.binding.barcodeButton.performClick();

        assertThat(shadowActivity.getNextStartedActivity(), nullValue());
        assertThat(waitingForDataRegistry.waiting.isEmpty(), equalTo(true));
    }

    @Test
    public void clickingBarcodeButton_whenPermissionIsGranted_setsWidgetWaitingForData() {
        FormEntryPrompt prompt = promptWithAnswer(null);
        when(prompt.getIndex()).thenReturn(formIndex);

        BarcodeWidget widget = createWidget(prompt);
        widget.setPermissionsProvider(permissionsProvider);
        widget.binding.barcodeButton.performClick();

        assertThat(waitingForDataRegistry.waiting.contains(formIndex), equalTo(true));
    }

    @Test
    public void clickingBarcodeButton_whenFrontCameraIsNotAvailable_showsFrontCameraNotAvailableToast() {
        when(cameraUtils.isFrontCameraAvailable(any())).thenReturn(false);
        BarcodeWidget widget = createWidget(promptWithAppearance(Appearances.FRONT));
        widget.setPermissionsProvider(permissionsProvider);
        widget.binding.barcodeButton.performClick();

        assertThat(ShadowToast.getTextOfLatestToast(), equalTo(widgetTestActivity.getString(org.odk.collect.strings.R.string.error_front_camera_unavailable)));
    }

    @Test
    public void clickingBarcodeButton_whenFrontCameraIsAvailable_launchesCorrectIntent() {
        when(cameraUtils.isFrontCameraAvailable(any())).thenReturn(true);
        BarcodeWidget widget = createWidget(promptWithAppearance(Appearances.FRONT));
        widget.setPermissionsProvider(permissionsProvider);
        widget.binding.barcodeButton.performClick();

        assertThat(shadowActivity.getNextStartedActivity().getBooleanExtra(Appearances.FRONT, false), equalTo(true));
    }

    @Test
    public void whenPromptHasHiddenAnswerAppearance_answerIsNotDisplayed() {
        FormEntryPrompt prompt = new MockFormEntryPromptBuilder(promptWithAppearance(Appearances.HIDDEN_ANSWER))
                .withAnswer(new StringData("original contents"))
                .build();

        BarcodeWidget widget = createWidget(prompt);

        // Check initial value is not shown
        assertThat(widget.binding.barcodeAnswerText.getVisibility(), equalTo(View.GONE));
        assertThat(widget.binding.barcodeButton.getText(), equalTo(widgetTestActivity.getString(org.odk.collect.strings.R.string.replace_barcode)));
        assertThat(widget.getAnswer(), equalTo(new StringData("original contents")));

        // Check updates aren't shown
        widget.setData("updated contents");
        assertThat(widget.binding.barcodeAnswerText.getVisibility(), equalTo(View.GONE));
        assertThat(widget.binding.barcodeButton.getText(), equalTo(widgetTestActivity.getString(org.odk.collect.strings.R.string.replace_barcode)));
        assertThat(widget.getAnswer(), equalTo(new StringData("updated contents")));
    }

    public BarcodeWidget createWidget(FormEntryPrompt prompt) {
        return new BarcodeWidget(widgetTestActivity, new QuestionDetails(prompt),
                waitingForDataRegistry, cameraUtils, widgetDependencies());
    }
}
