package org.odk.collect.android.widgets;

import android.view.View;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.maps.MapConfigurator;
import org.odk.collect.android.listeners.WidgetValueChangedListener;
import org.odk.collect.android.widgets.interfaces.GeoDataRequester;
import org.odk.collect.android.widgets.utilities.WaitingForDataRegistry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.odk.collect.android.widgets.support.GeoWidgetHelpers.stringFromDoubleList;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.mockValueChangedListener;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.promptWithAnswer;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.promptWithReadOnly;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.promptWithReadOnlyAndAnswer;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.widgetDependencies;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.widgetTestActivity;

@RunWith(AndroidJUnit4.class)
public class GeoTraceWidgetTest {
    private final String answer = stringFromDoubleList();

    private WaitingForDataRegistry waitingForDataRegistry;
    private GeoDataRequester geoDataRequester;
    private MapConfigurator mapConfigurator;

    @Before
    public void setup() {
        waitingForDataRegistry = mock(WaitingForDataRegistry.class);
        geoDataRequester = mock(GeoDataRequester.class);
        mapConfigurator = mock(MapConfigurator.class);
        when(mapConfigurator.isAvailable(any())).thenReturn(true);
    }

    @Test
    public void getAnswer_whenPromptDoesNotHaveAnswer_returnsNull() {
        GeoTraceWidget widget = createWidget(promptWithAnswer(null));
        assertNull(widget.getAnswer());
    }

    @Test
    public void getAnswer_whenPromptHasAnswer_returnsAnswer() {
        GeoTraceWidget widget = createWidget(promptWithAnswer(new StringData(answer)));
        assertEquals(widget.getAnswer().getDisplayText(), answer);
    }

    @Test
    public void whenPromptDoesNotHaveAnswer_textViewDisplaysEmptyString() {
        GeoTraceWidget widget = createWidget(promptWithAnswer(null));
        assertEquals(widget.binding.geoAnswerText.getText().toString(), "");
    }

    @Test
    public void whenPromptHasAnswer_textViewDisplaysAnswer() {
        GeoTraceWidget widget = createWidget(promptWithAnswer(new StringData(answer)));
        assertEquals(widget.binding.geoAnswerText.getText().toString(), answer);
    }

    @Test
    public void whenPromptIsReadOnlyAndDoesNotHaveAnswer_geoButtonIsNotDisplayed() {
        GeoTraceWidget widget = createWidget(promptWithReadOnly());
        assertEquals(widget.binding.simpleButton.getVisibility(), View.GONE);
    }

    @Test
    public void whenPromptIsReadOnlyAndHasAnswer_viewGeoShapeButtonIsShown() {
        GeoTraceWidget widget = createWidget(promptWithReadOnlyAndAnswer(new StringData(answer)));
        assertEquals(widget.binding.simpleButton.getText(), widget.getContext().getString(org.odk.collect.strings.R.string.view_line));
    }

    @Test
    public void whenPromptIsNotReadOnlyAndDoesNotHaveAnswer_startGeoShapeButtonIsShown() {
        GeoTraceWidget widget = createWidget(promptWithAnswer(null));
        assertEquals(widget.binding.simpleButton.getText(), widget.getContext().getString(org.odk.collect.strings.R.string.get_line));
    }

    @Test
    public void whenPromptIsNotReadOnlyAndHasAnswer_viewOrChangeGeoShapeButtonIsShown() {
        GeoTraceWidget widget = createWidget(promptWithAnswer(new StringData(answer)));
        assertEquals(widget.binding.simpleButton.getText(), widget.getContext().getString(org.odk.collect.strings.R.string.view_or_change_line));
    }

    @Test
    public void clearAnswer_clearsWidgetAnswer() {
        GeoTraceWidget widget = createWidget(promptWithAnswer(new StringData(answer)));
        widget.clearAnswer();

        assertEquals(widget.binding.geoAnswerText.getText(), "");
        assertEquals(widget.binding.simpleButton.getText(), widget.getContext().getString(org.odk.collect.strings.R.string.get_line));
    }

    @Test
    public void clearAnswer_callsValueChangeListeners() {
        GeoTraceWidget widget = createWidget(promptWithAnswer(null));
        WidgetValueChangedListener valueChangedListener = mockValueChangedListener(widget);
        widget.clearAnswer();

        verify(valueChangedListener).widgetValueChanged(widget);
    }

    @Test
    public void clickingButtonAndAnswerTextViewForLong_callsLongClickListener() {
        View.OnLongClickListener listener = mock(View.OnLongClickListener.class);
        GeoTraceWidget widget = createWidget(promptWithAnswer(null));

        widget.setOnLongClickListener(listener);
        widget.binding.simpleButton.performLongClick();
        widget.binding.geoAnswerText.performLongClick();

        verify(listener).onLongClick(widget.binding.simpleButton);
        verify(listener).onLongClick(widget.binding.geoAnswerText);
    }

    @Test
    public void setData_updatesWidgetAnswer() {
        GeoTraceWidget widget = createWidget(promptWithAnswer(null));
        widget.setData(answer);
        assertEquals(widget.getAnswer().getDisplayText(), answer);
    }

    @Test
    public void setData_setsCorrectAnswerInAnswerTextView() {
        GeoTraceWidget widget = createWidget(promptWithAnswer(null));
        widget.setData(answer);
        assertEquals(widget.binding.geoAnswerText.getText().toString(), answer);
    }

    @Test
    public void setData_updatesWidgetDisplayedAnswer() {
        GeoTraceWidget widget = createWidget(promptWithAnswer(null));
        widget.setData(answer);
        assertEquals(widget.binding.geoAnswerText.getText().toString(), answer);
    }

    @Test
    public void setData_whenDataIsNull_updatesButtonLabel() {
        GeoTraceWidget widget = createWidget(promptWithAnswer(new StringData(answer)));
        widget.setData("");
        assertEquals(widget.binding.simpleButton.getText(), widget.getContext().getString(org.odk.collect.strings.R.string.get_line));
    }

    @Test
    public void setData_whenDataIsNotNull_updatesButtonLabel() {
        GeoTraceWidget widget = createWidget(promptWithAnswer(null));
        widget.setData(answer);
        assertEquals(widget.binding.simpleButton.getText(), widget.getContext().getString(org.odk.collect.strings.R.string.view_or_change_line));
    }

    @Test
    public void setData_callsValueChangeListener() {
        GeoTraceWidget widget = createWidget(promptWithAnswer(null));
        WidgetValueChangedListener valueChangedListener = mockValueChangedListener(widget);
        widget.setData(answer);

        verify(valueChangedListener).widgetValueChanged(widget);
    }

    @Test
    public void buttonClick_whenMapConfiguratorIsUnavailable_doesNotRequestGeoTrace() {
        FormEntryPrompt prompt = promptWithAnswer(null);
        GeoTraceWidget widget = createWidget(prompt);

        when(mapConfigurator.isAvailable(widget.getContext())).thenReturn(false);
        widget.binding.simpleButton.performClick();

        verify(geoDataRequester, never()).requestGeoTrace(prompt, "",  waitingForDataRegistry);
        verify(mapConfigurator).showUnavailableMessage(widget.getContext());
    }

    @Test
    public void buttonClick_whenMapConfiguratorIsAvailable_requestsGeoTrace() {
        FormEntryPrompt prompt = promptWithAnswer(null);
        GeoTraceWidget widget = createWidget(prompt);
        widget.binding.simpleButton.performClick();

        verify(geoDataRequester).requestGeoTrace(prompt, "", waitingForDataRegistry);
    }

    @Test
    public void buttonClick_requestsGeoTrace_whenAnswerIsCleared() {
        FormEntryPrompt prompt = promptWithAnswer(new StringData(answer));
        GeoTraceWidget widget = createWidget(prompt);
        widget.clearAnswer();
        widget.binding.simpleButton.performClick();

        verify(geoDataRequester).requestGeoTrace(prompt, "", waitingForDataRegistry);
    }

    @Test
    public void buttonClick_requestsGeoTrace_whenAnswerIsUpdated() {
        FormEntryPrompt prompt = promptWithAnswer(null);
        GeoTraceWidget widget = createWidget(prompt);
        widget.setData(answer);
        widget.binding.simpleButton.performClick();

        verify(geoDataRequester).requestGeoTrace(prompt, answer, waitingForDataRegistry);
    }

    private GeoTraceWidget createWidget(FormEntryPrompt prompt) {
        return new GeoTraceWidget(widgetTestActivity(), new QuestionDetails(prompt),
                waitingForDataRegistry, mapConfigurator, geoDataRequester, widgetDependencies());
    }
}
