package org.odk.collect.android.widgets;

import android.view.View;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.listeners.WidgetValueChangedListener;
import org.odk.collect.android.widgets.interfaces.GeoDataRequester;
import org.odk.collect.android.widgets.utilities.WaitingForDataRegistry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.odk.collect.android.widgets.support.GeoWidgetHelpers.stringFromDoubleList;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.mockValueChangedListener;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.promptWithAnswer;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.promptWithReadOnly;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.promptWithReadOnlyAndAnswer;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.widgetDependencies;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.widgetTestActivity;

@RunWith(AndroidJUnit4.class)
public class GeoShapeWidgetTest {
    private final String answer = stringFromDoubleList();

    private GeoDataRequester geoDataRequester;
    private WaitingForDataRegistry waitingForDataRegistry;

    @Before
    public void setup() {
        geoDataRequester = mock(GeoDataRequester.class);
        waitingForDataRegistry = mock(WaitingForDataRegistry.class);
    }

    @Test
    public void getAnswer_whenPromptDoesNotHaveAnswer_returnsNull() {
        GeoShapeWidget widget = createWidget(promptWithAnswer(null));
        assertNull(widget.getAnswer());
    }

    @Test
    public void getAnswer_whenPromptHasAnswer_returnsAnswer() {
        GeoShapeWidget widget = createWidget(promptWithAnswer(new StringData(answer)));
        assertEquals(widget.getAnswer().getDisplayText(), answer);
    }

    @Test
    public void whenPromptDoesNotHaveAnswer_textViewDisplaysEmptyString() {
        GeoShapeWidget widget = createWidget(promptWithAnswer(null));
        assertEquals(widget.binding.geoAnswerText.getText().toString(), "");
    }

    @Test
    public void whenPromptHasAnswer_textViewDisplaysAnswer() {
        GeoShapeWidget widget = createWidget(promptWithAnswer(new StringData(answer)));
        assertEquals(widget.binding.geoAnswerText.getText().toString(), answer);
    }

    @Test
    public void whenPromptIsReadOnlyAndDoesNotHaveAnswer_geoButtonIsNotDisplayed() {
        GeoShapeWidget widget = createWidget(promptWithReadOnly());
        assertEquals(widget.binding.simpleButton.getVisibility(), View.GONE);
    }

    @Test
    public void whenPromptIsReadOnlyAndHasAnswer_viewGeoShapeButtonIsShown() {
        GeoShapeWidget widget = createWidget(promptWithReadOnlyAndAnswer(new StringData(answer)));
        assertEquals(widget.binding.simpleButton.getText(), widget.getContext().getString(org.odk.collect.strings.R.string.view_polygon));
    }

    @Test
    public void whenPromptIsNotReadOnlyAndDoesNotHaveAnswer_startGeoShapeButtonIsShown() {
        GeoShapeWidget widget = createWidget(promptWithAnswer(null));
        assertEquals(widget.binding.simpleButton.getText(), widget.getContext().getString(org.odk.collect.strings.R.string.get_polygon));
    }

    @Test
    public void whenPromptIsNotReadOnlyAndHasAnswer_viewOrChangeGeoShapeButtonIsShown() {
        GeoShapeWidget widget = createWidget(promptWithAnswer(new StringData(answer)));
        assertEquals(widget.binding.simpleButton.getText(), widget.getContext().getString(org.odk.collect.strings.R.string.view_or_change_polygon));
    }

    @Test
    public void clearAnswer_clearsWidgetAnswer() {
        GeoShapeWidget widget = createWidget(promptWithAnswer(new StringData(answer)));
        widget.clearAnswer();

        assertEquals(widget.binding.geoAnswerText.getText(), "");
        assertEquals(widget.binding.simpleButton.getText(), widget.getContext().getString(org.odk.collect.strings.R.string.get_polygon));
    }

    @Test
    public void clearAnswer_callsValueChangeListeners() {
        GeoShapeWidget widget = createWidget(promptWithAnswer(null));
        WidgetValueChangedListener valueChangedListener = mockValueChangedListener(widget);
        widget.clearAnswer();

        verify(valueChangedListener).widgetValueChanged(widget);
    }

    @Test
    public void clickingButtonAndAnswerTextViewForLong_callsLongClickListeners() {
        View.OnLongClickListener listener = mock(View.OnLongClickListener.class);
        GeoShapeWidget widget = createWidget(promptWithAnswer(null));

        widget.setOnLongClickListener(listener);
        widget.binding.simpleButton.performLongClick();
        widget.binding.geoAnswerText.performLongClick();

        verify(listener).onLongClick(widget.binding.simpleButton);
        verify(listener).onLongClick(widget.binding.geoAnswerText);
    }

    @Test
    public void setData_updatesWidgetAnswer() {
        GeoShapeWidget widget = createWidget(promptWithAnswer(null));
        widget.setData(answer);
        assertEquals(widget.getAnswer().getDisplayText(), answer);
    }

    @Test
    public void setData_updatesWidgetDisplayedAnswer() {
        GeoShapeWidget widget = createWidget(promptWithAnswer(null));
        widget.setData(answer);
        assertEquals(widget.binding.geoAnswerText.getText().toString(), answer);
    }

    @Test
    public void setData_whenDataIsNull_updatesButtonLabel() {
        GeoShapeWidget widget = createWidget(promptWithAnswer(new StringData(answer)));
        widget.setData("");
        assertEquals(widget.binding.simpleButton.getText(), widget.getContext().getString(org.odk.collect.strings.R.string.get_polygon));
    }

    @Test
    public void setData_whenDataIsNotNull_updatesButtonLabel() {
        GeoShapeWidget widget = createWidget(promptWithAnswer(null));
        widget.setData(answer);
        assertEquals(widget.binding.simpleButton.getText(), widget.getContext().getString(org.odk.collect.strings.R.string.view_or_change_polygon));
    }

    @Test
    public void setData_callsValueChangeListener() {
        GeoShapeWidget widget = createWidget(promptWithAnswer(null));
        WidgetValueChangedListener valueChangedListener = mockValueChangedListener(widget);
        widget.setData(answer);

        verify(valueChangedListener).widgetValueChanged(widget);
    }

    @Test
    public void buttonClick_requestsGeoShape() {
        FormEntryPrompt prompt = promptWithAnswer(new StringData(answer));
        GeoShapeWidget widget = createWidget(prompt);
        widget.binding.simpleButton.performClick();
        verify(geoDataRequester).requestGeoShape(prompt, answer, waitingForDataRegistry);
    }

    @Test
    public void buttonClick_requestsGeoShape_whenAnswerIsCleared() {
        FormEntryPrompt prompt = promptWithAnswer(new StringData(answer));
        GeoShapeWidget widget = createWidget(prompt);
        widget.clearAnswer();
        widget.binding.simpleButton.performClick();

        verify(geoDataRequester).requestGeoShape(prompt, "", waitingForDataRegistry);
    }

    @Test
    public void buttonClick_requestsGeoShape_whenAnswerIsUpdated() {
        FormEntryPrompt prompt = promptWithAnswer(null);
        GeoShapeWidget widget = createWidget(prompt);
        widget.setData(answer);
        widget.binding.simpleButton.performClick();

        verify(geoDataRequester).requestGeoShape(prompt, answer, waitingForDataRegistry);
    }

    private GeoShapeWidget createWidget(FormEntryPrompt prompt) {
        return new GeoShapeWidget(widgetTestActivity(), new QuestionDetails(prompt),
                waitingForDataRegistry, geoDataRequester, widgetDependencies());
    }
}
