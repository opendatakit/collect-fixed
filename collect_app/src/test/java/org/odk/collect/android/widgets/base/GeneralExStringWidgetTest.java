package org.odk.collect.android.widgets.base;

import static junit.framework.Assert.assertTrue;

import android.view.View;
import android.widget.TextView;

import org.javarosa.core.model.data.IAnswerData;
import org.junit.Test;
import org.odk.collect.android.R;
import org.odk.collect.android.support.WidgetTestActivity;
import org.odk.collect.android.widgets.ExStringWidget;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.util.List;

/**
 * @author James Knight
 */

public abstract class GeneralExStringWidgetTest<W extends ExStringWidget, A extends IAnswerData> extends BinaryWidgetTest<W, A> {

    @Override
    public Object createBinaryData(A answerData) {
        return answerData.getDisplayText();
    }

    // TODO we should have such tests for every widget like we have to confirm readOnly option
    @Test
    public void testElementsVisibilityAndAvailability() {
        assertThat(getSpyWidget().binding.launchAppButton.getVisibility(), is(View.VISIBLE));
        assertThat(getSpyWidget().binding.launchAppButton.isEnabled(), is(Boolean.TRUE));
        assertThat(getSpyWidget().binding.widgetAnswerText.isEditableState(), is(false));
    }

    @Test
    public void usingReadOnlyOptionShouldMakeAllClickableElementsDisabled() {
        when(formEntryPrompt.isReadOnly()).thenReturn(true);

        assertThat(getSpyWidget().binding.launchAppButton.getVisibility(), is(View.GONE));
        assertThat(getSpyWidget().binding.widgetAnswerText.isEditableState(), is(false));
    }

    /**
     * Unlike other widgets, String widgets that contain EditText should not be registered to
     * context menu as a whole because the Clipboard menu would be broken.
     *
     * https://github.com/getodk/collect/pull/4860
     */
    @Test
    public void widgetShouldBeRegisteredForContextMenu() {
        ExStringWidget widget = createWidget();
        List<View> viewsRegisterForContextMenu = ((WidgetTestActivity) activity).viewsRegisterForContextMenu;

        assertThat(viewsRegisterForContextMenu.size(), is(3));

        assertTrue(viewsRegisterForContextMenu.contains(widget.findViewWithTag(R.id.question_label)));
        assertTrue(viewsRegisterForContextMenu.contains(widget.findViewWithTag(R.id.help_text)));
        assertTrue(viewsRegisterForContextMenu.contains(widget.findViewWithTag(R.id.error_message_container)));

        assertThat(viewsRegisterForContextMenu.get(0).getId(), is(widget.getId()));
        assertThat(viewsRegisterForContextMenu.get(1).getId(), is(widget.getId()));
    }

    @Test
    public void errorDisappearsOnSetData() {
        ExStringWidget widget = getWidget();
        widget.displayError("blah");
        widget.setData("answer");

        assertThat(widget.errorLayout.getVisibility(), equalTo(TextView.GONE));
        assertThat(widget.getBackground(), equalTo(null));
    }

    @Test
    public void errorDisappearsOnAddingAnswerManuallyViaTheTextField() {
        ExStringWidget widget = getWidget();
        widget.displayError("blah");
        widget.binding.widgetAnswerText.getBinding().editText.setText("answer");

        assertThat(widget.errorLayout.getVisibility(), equalTo(TextView.GONE));
        assertThat(widget.getBackground(), equalTo(null));
    }

    @Test
    public abstract void verifyInputType();

    @Test
    public abstract void whenPromptHasHiddenAnswerAppearance_answerIsNotDisplayed();
}
