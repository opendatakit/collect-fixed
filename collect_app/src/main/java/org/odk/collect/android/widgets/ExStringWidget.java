/*
 * Copyright (C) 2012 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.odk.collect.android.widgets;

import static org.odk.collect.android.utilities.Appearances.hasAppearance;
import static org.odk.collect.android.utilities.ApplicationConstants.RequestCodes;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.databinding.ExStringQuestionTypeBinding;
import org.odk.collect.android.dynamicpreload.ExternalAppsUtils;
import org.odk.collect.android.R;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.utilities.Appearances;
import org.odk.collect.android.widgets.interfaces.WidgetDataReceiver;
import org.odk.collect.android.widgets.utilities.QuestionFontSizeUtils;
import org.odk.collect.android.widgets.utilities.StringRequester;
import org.odk.collect.android.widgets.utilities.StringWidgetUtils;
import org.odk.collect.android.widgets.utilities.WaitingForDataRegistry;

import java.io.Serializable;

import timber.log.Timber;

/**
 * <p>Launch an external app to supply a string value. If the app
 * does not launch, enable the text area for regular data entry.</p>
 * <p>
 * <p>The default button text is "Launch"
 * <p>
 * <p>You may override the button text and the error text that is
 * displayed when the app is missing by using jr:itext() values.
 * <p>
 * <p>To use this widget, define an appearance on the &lt;input/&gt;
 * tag that begins "ex:" and then contains the intent action to lauch.
 * <p>
 * <p>e.g.,
 * <p>
 * <pre>
 * &lt;input appearance="ex:change.uw.android.TEXTANSWER" ref="/form/passPhrase" &gt;
 * </pre>
 * <p>or, to customize the button text and error strings with itext:
 * <pre>
 *      ...
 *      &lt;bind nodeset="/form/passPhrase" type="string" /&gt;
 *      ...
 *      &lt;itext&gt;
 *        &lt;translation lang="English"&gt;
 *          &lt;text id="textAnswer"&gt;
 *            &lt;value form="short"&gt;Text question&lt;/value&gt;
 *            &lt;value form="long"&gt;Enter your pass phrase&lt;/value&gt;
 *            &lt;value form="buttonText"&gt;Get Pass Phrase&lt;/value&gt;
 *            &lt;value form="noAppErrorString"&gt;Pass Phrase Tool is not installed!
 *             Please proceed to manually enter pass phrase.&lt;/value&gt;
 *          &lt;/text&gt;
 *        &lt;/translation&gt;
 *      &lt;/itext&gt;
 *    ...
 *    &lt;input appearance="ex:change.uw.android.TEXTANSWER" ref="/form/passPhrase"&gt;
 *      &lt;label ref="jr:itext('textAnswer')"/&gt;
 *    &lt;/input&gt;
 * </pre>
 */
@SuppressLint("ViewConstructor")
public class ExStringWidget extends QuestionWidget implements WidgetDataReceiver {
    public ExStringQuestionTypeBinding binding;
    private final WaitingForDataRegistry waitingForDataRegistry;

    private boolean hasExApp = true;
    private final StringRequester stringRequester;

    public ExStringWidget(Context context, QuestionDetails questionDetails, WaitingForDataRegistry waitingForDataRegistry, StringRequester stringRequester) {
        super(context, questionDetails);
        render();

        this.waitingForDataRegistry = waitingForDataRegistry;
        this.stringRequester = stringRequester;
    }

    @Override
    protected View onCreateAnswerView(@NonNull Context context, @NonNull FormEntryPrompt prompt, int answerFontSize) {
        binding = ExStringQuestionTypeBinding.inflate(LayoutInflater.from(context));
        binding.launchAppButton.setText(getButtonText());
        binding.launchAppButton.setOnClickListener(v -> {
            waitingForDataRegistry.waitForData(getFormEntryPrompt().getIndex());
            stringRequester.launch((Activity) getContext(), getRequestCode(), getFormEntryPrompt(), getAnswerForIntent(), (String errorMsg) -> {
                onException(errorMsg);
                return null;
            });
        });
        if (questionDetails.isReadOnly()) {
            binding.launchAppButton.setVisibility(GONE);
        }
        binding.widgetAnswerText.init(
                QuestionFontSizeUtils.getFontSize(settings, QuestionFontSizeUtils.FontSize.HEADLINE_6),
                true,
                StringWidgetUtils.getNumberOfRows(questionDetails.getPrompt()),
                Appearances.isMasked(prompt),
                this::widgetValueChanged
        );
        binding.widgetAnswerText.setAnswer(getFormEntryPrompt().getAnswerText());
        if (hasAppearance(getFormEntryPrompt(), Appearances.HIDDEN_ANSWER)) {
            binding.widgetAnswerText.setVisibility(GONE);
        }

        return binding.getRoot();
    }

    private String getButtonText() {
        String v = getFormEntryPrompt().getSpecialFormQuestionText("buttonText");
        return v != null ? v : getContext().getString(org.odk.collect.strings.R.string.launch_app);
    }

    protected Serializable getAnswerForIntent() {
        return getFormEntryPrompt().getAnswerText();
    }

    protected int getRequestCode() {
        return RequestCodes.EX_STRING_CAPTURE;
    }

    @Nullable
    @Override
    public IAnswerData getAnswer() {
        String answer = binding.widgetAnswerText.getAnswer();
        return !answer.isEmpty() ? new StringData(answer) : null;
    }

    @Override
    public void clearAnswer() {
        binding.widgetAnswerText.clearAnswer();
    }

    @Override
    public void setData(Object answer) {
        StringData stringData = ExternalAppsUtils.asStringData(answer);
        binding.widgetAnswerText.setAnswer(stringData == null ? null : stringData.getValue().toString());
    }

    @Override
    public void setFocus(Context context) {
        if (hasExApp) {
            binding.widgetAnswerText.setFocus(false);
            // focus on launch button
            binding.launchAppButton.requestFocus();
        } else {
            if (!getFormEntryPrompt().isReadOnly()) {
                binding.widgetAnswerText.setFocus(true);
            /*
             * If you do a multi-question screen after a "add another group" dialog, this won't
             * automatically pop up. It's an Android issue.
             *
             * That is, if I have an edit text in an activity, and pop a dialog, and in that
             * dialog's button's OnClick() I call edittext.requestFocus() and
             * showSoftInput(edittext, 0), showSoftinput() returns false. However, if the
             * edittext
             * is focused before the dialog pops up, everything works fine. great.
             */
            } else {
                binding.widgetAnswerText.setFocus(false);
            }
        }
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        binding.widgetAnswerText.setOnLongClickListener(l);
        binding.launchAppButton.setOnLongClickListener(l);
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        binding.widgetAnswerText.cancelLongPress();
        binding.launchAppButton.cancelLongPress();
    }

    /**
     * Registers all subviews except for the answer_container (which contains the EditText) to clear on long press.
     * This makes it possible to long-press to paste or perform other text editing functions.
     */
    @Override
    protected void registerToClearAnswerOnLongPress(Activity activity, ViewGroup viewGroup) {
        ViewGroup view = findViewById(R.id.question_widget_container);
        for (int i = 0; i < view.getChildCount(); i++) {
            View childView = view.getChildAt(i);
            if (childView.getId() != R.id.answer_container) {
                childView.setTag(childView.getId());
                childView.setId(getId());
                activity.registerForContextMenu(childView);
            }
        }
    }

    private void focusAnswer() {
        binding.widgetAnswerText.setFocus(true);
    }

    private void onException(String toastText) {
        hasExApp = false;
        if (!getFormEntryPrompt().isReadOnly()) {
            binding.widgetAnswerText.updateState(false);
        }
        binding.launchAppButton.setEnabled(false);
        binding.launchAppButton.setFocusable(false);
        waitingForDataRegistry.cancelWaitingForData();

        Toast.makeText(getContext(),
                toastText, Toast.LENGTH_SHORT)
                .show();
        Timber.d(toastText);
        focusAnswer();
    }

    @Override
    public void hideError() {
        super.hideError();
        binding.widgetAnswerText.setError(null);
    }

    @Override
    public void displayError(String errorMessage) {
        hideError();

        if (binding.widgetAnswerText.isEditableState()) {
            binding.widgetAnswerText.setError(errorMessage);
            setBackground(ContextCompat.getDrawable(getContext(), R.drawable.question_with_error_border));
        } else {
            ((TextView) errorLayout.findViewById(R.id.error_message)).setText(errorMessage);
            errorLayout.setVisibility(VISIBLE);
            setBackground(ContextCompat.getDrawable(getContext(), R.drawable.question_with_error_border));
        }
    }
}
