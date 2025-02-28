package org.odk.collect.android.widgets;

import android.view.View;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.javarosa.core.model.data.GeoPointData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.listeners.WidgetValueChangedListener;
import org.odk.collect.android.widgets.interfaces.GeoDataRequester;
import org.odk.collect.android.widgets.utilities.GeoWidgetUtils;
import org.odk.collect.android.widgets.utilities.WaitingForDataRegistry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.odk.collect.android.widgets.support.GeoWidgetHelpers.getRandomDoubleArray;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.mockValueChangedListener;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.promptWithAnswer;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.promptWithReadOnly;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.promptWithReadOnlyAndAnswer;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.widgetDependencies;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.widgetTestActivity;

@RunWith(AndroidJUnit4.class)
public class GeoPointMapWidgetTest {
    private final GeoPointData answer = new GeoPointData(getRandomDoubleArray());

    private GeoDataRequester geoDataRequester;
    private WaitingForDataRegistry waitingForDataRegistry;

    @Before
    public void setup() {
        geoDataRequester = mock(GeoDataRequester.class);
        waitingForDataRegistry = mock(WaitingForDataRegistry.class);
    }

    @Test
    public void getAnswer_whenPromptDoesNotHaveAnswer_returnsNull() {
        GeoPointMapWidget widget = createWidget(promptWithAnswer(null));
        assertNull(widget.getAnswer());
    }

    @Test
    public void getAnswer_whenPromptHasAnswer_returnsPromptAnswer() {
        GeoPointMapWidget widget = createWidget(promptWithAnswer(answer));
        assertEquals(widget.getAnswer().getDisplayText(),
                new GeoPointData(GeoWidgetUtils.parseGeometryPoint(answer.getDisplayText())).getDisplayText());
    }

    @Test
    public void getAnswer_whenPromptHasInvalidAnswer_returnsNull() {
        GeoPointMapWidget widget = createWidget(promptWithAnswer(new StringData("blah")));
        assertNull(widget.getAnswer());
    }

    @Test
    public void creatingWidgetWithInvalidValue_doesNotUpdateWidgetDisplayedAnswer() {
        GeoPointMapWidget widget = createWidget(promptWithAnswer(new StringData("blah")));
        assertEquals(widget.binding.geoAnswerText.getText(), "");
        assertEquals(widget.binding.simpleButton.getText(), widget.getContext().getString(org.odk.collect.strings.R.string.get_point));
    }

    @Test
    public void whenPromptHasAnswer_answerTextViewShowsCorrectAnswer() {
        GeoPointMapWidget widget = createWidget(promptWithAnswer(answer));
        assertEquals(widget.binding.geoAnswerText.getText(), GeoWidgetUtils.getGeoPointAnswerToDisplay(widget.getContext(), answer.getDisplayText()));
    }

    @Test
    public void whenPromptIsReadOnlyAndDoesNotHaveAnswer_geoButtonIsNotDisplayed() {
        GeoPointMapWidget widget = createWidget(promptWithReadOnly());
        assertEquals(widget.binding.simpleButton.getVisibility(), View.GONE);
    }

    @Test
    public void whenPromptIsReadOnlyAndHasAnswer_viewGeoPointButtonIsShown() {
        GeoPointMapWidget widget = createWidget(promptWithReadOnlyAndAnswer(answer));
        assertEquals(widget.binding.simpleButton.getText(), widget.getContext().getString(org.odk.collect.strings.R.string.view_point));
    }

    @Test
    public void whenPromptIsNotReadOnlyAndDoesNotHaveAnswer_startGeoPointButtonIsShown() {
        GeoPointMapWidget widget = createWidget(promptWithAnswer(null));
        assertEquals(widget.binding.simpleButton.getText(), widget.getContext().getString(org.odk.collect.strings.R.string.get_point));
    }

    @Test
    public void whenPromptIsNotReadOnlyAndHasAnswer_viewOrChangeLocationButtonIsShown() {
        GeoPointMapWidget widget = createWidget(promptWithAnswer(answer));
        assertEquals(widget.binding.simpleButton.getText(), widget.getContext().getString(org.odk.collect.strings.R.string.view_or_change_point));
    }

    @Test
    public void clearAnswer_clearsWidgetAnswer() {
        GeoPointMapWidget widget = createWidget(promptWithAnswer(answer));
        widget.clearAnswer();

        assertEquals(widget.binding.geoAnswerText.getText(), "");
        assertEquals(widget.binding.simpleButton.getText(), widget.getContext().getString(org.odk.collect.strings.R.string.get_point));
    }

    @Test
    public void clearAnswer_callsValueChangeListeners() {
        GeoPointMapWidget widget = createWidget(promptWithAnswer(null));
        WidgetValueChangedListener valueChangedListener = mockValueChangedListener(widget);
        widget.clearAnswer();

        verify(valueChangedListener).widgetValueChanged(widget);
    }

    @Test
    public void setData_updatesWidgetAnswer() {
        GeoPointMapWidget widget = createWidget(promptWithAnswer(null));
        widget.setData(answer.getDisplayText());
        assertEquals(widget.getAnswer().getDisplayText(), answer.getDisplayText());
    }

    @Test
    public void setDataWithInvalidValue_doesNotUpdateWidgetAnswer() {
        GeoPointMapWidget widget = createWidget(promptWithAnswer(null));
        widget.setData("blah");
        assertEquals(widget.getAnswer(), null);
    }

    @Test
    public void setData_updatesWidgetDisplayedAnswer() {
        GeoPointMapWidget widget = createWidget(promptWithAnswer(null));
        widget.setData(answer.getDisplayText());
        assertEquals(widget.binding.geoAnswerText.getText(), GeoWidgetUtils.getGeoPointAnswerToDisplay(widget.getContext(), answer.getDisplayText()));
    }

    @Test
    public void setDataWithInvalidValue_doesNotUpdateWidgetDisplayedAnswer() {
        GeoPointMapWidget widget = createWidget(promptWithAnswer(null));
        widget.setData("blah");
        assertEquals(widget.binding.geoAnswerText.getText(), "");
        assertEquals(widget.binding.simpleButton.getText(), widget.getContext().getString(org.odk.collect.strings.R.string.get_point));
    }

    @Test
    public void setData_whenDataIsNull_updatesButtonLabel() {
        GeoPointMapWidget widget = createWidget(promptWithAnswer(answer));
        widget.setData("");
        assertEquals(widget.binding.simpleButton.getText(), widget.getContext().getString(org.odk.collect.strings.R.string.get_point));
    }

    @Test
    public void setData_whenDataIsNotNull_updatesButtonLabel() {
        GeoPointMapWidget widget = createWidget(promptWithAnswer(null));
        widget.setData(answer.getDisplayText());
        assertEquals(widget.binding.simpleButton.getText(), widget.getContext().getString(org.odk.collect.strings.R.string.view_or_change_point));
    }

    @Test
    public void setData_callsValueChangeListener() {
        GeoPointMapWidget widget = createWidget(promptWithAnswer(null));
        WidgetValueChangedListener valueChangedListener = mockValueChangedListener(widget);
        widget.setData(answer.getDisplayText());

        verify(valueChangedListener).widgetValueChanged(widget);
    }

    @Test
    public void clickingButtonAndAnswerTextViewForLong_callsLongClickListener() {
        View.OnLongClickListener listener = mock(View.OnLongClickListener.class);
        GeoPointMapWidget widget = createWidget(promptWithAnswer(null));

        widget.setOnLongClickListener(listener);
        widget.binding.simpleButton.performLongClick();
        widget.binding.geoAnswerText.performLongClick();

        verify(listener).onLongClick(widget.binding.simpleButton);
        verify(listener).onLongClick(widget.binding.geoAnswerText);
    }

    @Test
    public void buttonClick_requestsGeoPoint() {
        FormEntryPrompt prompt = promptWithAnswer(answer);
        GeoPointMapWidget widget = createWidget(prompt);
        widget.binding.simpleButton.performClick();
        verify(geoDataRequester).requestGeoPoint(prompt, answer.getDisplayText(), waitingForDataRegistry);
    }

    @Test
    public void buttonClick_requestsGeoPoint_whenAnswerIsCleared() {
        FormEntryPrompt prompt = promptWithAnswer(answer);
        GeoPointMapWidget widget = createWidget(prompt);
        widget.clearAnswer();
        widget.binding.simpleButton.performClick();

        verify(geoDataRequester).requestGeoPoint(prompt, null, waitingForDataRegistry);
    }

    @Test
    public void buttonClick_requestsGeoPoint_whenAnswerIsUpdated() {
        FormEntryPrompt prompt = promptWithAnswer(null);
        GeoPointMapWidget widget = createWidget(prompt);
        widget.setData(answer);
        widget.binding.simpleButton.performClick();

        verify(geoDataRequester).requestGeoPoint(prompt, answer.getDisplayText(), waitingForDataRegistry);
    }

    private GeoPointMapWidget createWidget(FormEntryPrompt prompt) {
        return new GeoPointMapWidget(widgetTestActivity(), new QuestionDetails(prompt),
                waitingForDataRegistry, geoDataRequester, widgetDependencies());
    }
}
