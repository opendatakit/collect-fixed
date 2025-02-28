package org.odk.collect.android.widgets.base;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.mockValueChangedListener;

import android.app.Activity;
import android.view.View;

import androidx.annotation.NonNull;

import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.junit.Test;
import org.mockito.Mock;
import org.odk.collect.android.R;
import org.odk.collect.android.javarosawrapper.FormController;
import org.odk.collect.android.listeners.WidgetValueChangedListener;
import org.odk.collect.android.support.WidgetTestActivity;
import org.odk.collect.android.widgets.QuestionWidget;
import org.odk.collect.android.widgets.interfaces.Widget;
import org.odk.collect.android.widgets.support.QuestionWidgetHelpers;

import java.util.List;
import java.util.Random;

public abstract class QuestionWidgetTest<W extends Widget, A extends IAnswerData>
        extends WidgetTest {

    protected Random random = new Random();
    protected Activity activity = QuestionWidgetHelpers.widgetTestActivity();
    protected QuestionWidget.Dependencies dependencies = QuestionWidgetHelpers.widgetDependencies();

    private W widget;
    private W actualWidget;

    @Mock
    public FormIndex formIndex;

    @Mock
    public FormController formController;

    @NonNull
    public abstract W createWidget();

    public abstract A getNextAnswer();

    public A getInitialAnswer() {
        return getNextAnswer();
    }

    /**
     * @return Real {@link Widget} object if present otherwise creates one
     * <p>
     * This should be used for mutating the {@link org.odk.collect.android.widgets.QuestionWidget}
     */
    public W getWidget() {
        if (actualWidget == null) {
            actualWidget = createWidget();
        }

        return actualWidget;
    }

    /**
     * @return {@link org.mockito.Spy} of the {@link #actualWidget}
     * <p>
     * This should be unless we want to mutate {@link org.odk.collect.android.widgets.QuestionWidget}
     * This is because a spy is not the real object and changing it won't have any effect on the real object
     */
    public W getSpyWidget() {
        if (widget == null) {
            widget = spy(getWidget());
        }

        return widget;
    }

    public void resetWidget() {
        actualWidget = null;
        widget = null;
    }

    public void setUp() throws Exception {
        super.setUp();

        when(formEntryPrompt.getIndex()).thenReturn(formIndex);

        widget = null;
    }

    @Test
    public void getAnswerShouldReturnNullIfPromptDoesNotHaveExistingAnswer() {
        W widget = getSpyWidget();
        assertNull(widget.getAnswer());
    }

    @Test
    public void getAnswerShouldReturnExistingAnswerIfPromptHasExistingAnswer() {
        A answer = getInitialAnswer();
        if (answer instanceof StringData) {
            when(formEntryPrompt.getAnswerText()).thenReturn((String) answer.getValue());
        } else {
            when(formEntryPrompt.getAnswerValue()).thenReturn(answer);
        }

        W widget = getSpyWidget();
        IAnswerData newAnswer = widget.getAnswer();

        assertNotNull(newAnswer);
        assertEquals(newAnswer.getDisplayText(), answer.getDisplayText());
    }

    @Test
    public void callingClearShouldRemoveTheExistingAnswer() {
        getAnswerShouldReturnExistingAnswerIfPromptHasExistingAnswer();
        widget.clearAnswer();

        assertNull(widget.getAnswer());
    }

    @Test
    public void callingClearShouldCallValueChangeListeners() {
        when(formEntryPrompt.getAnswerText()).thenReturn(getInitialAnswer().getDisplayText());

        QuestionWidget widget = (QuestionWidget) getWidget();
        WidgetValueChangedListener valueChangedListener = mockValueChangedListener(widget);
        widget.clearAnswer();
        verify(valueChangedListener).widgetValueChanged(widget);
    }

    @Test
    public void whenReadOnlyQuestionHasNoAnswer_answerContainerShouldNotBeDisplayed() {
        when(formEntryPrompt.isReadOnly()).thenReturn(true);
        QuestionWidget widget = (QuestionWidget) getWidget();
        assertThat(widget.findViewById(R.id.answer_container).getVisibility(), is(View.GONE));
    }

    @Test
    public void whenReadOnlyQuestionHasAnswer_answerContainerShouldBeDisplayed() {
        when(formEntryPrompt.isReadOnly()).thenReturn(true);
        when(formEntryPrompt.getAnswerValue()).thenReturn(getInitialAnswer());
        QuestionWidget widget = (QuestionWidget) getWidget();
        assertThat(widget.findViewById(R.id.answer_container).getVisibility(), is(View.VISIBLE));
    }

    // The whole widget should be registered for context menu to support removing answers/groups
    @Test
    public void widgetShouldBeRegisteredForContextMenu() {
        QuestionWidget widget = (QuestionWidget) createWidget();
        List<View> viewsRegisterForContextMenu = ((WidgetTestActivity) activity).viewsRegisterForContextMenu;

        assertThat(viewsRegisterForContextMenu.size(), is(1));
        assertThat(viewsRegisterForContextMenu.get(0), is(widget));
    }
}
