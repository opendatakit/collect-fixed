/*
 * Copyright (C) 2018 Smap Consulting
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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.method.TextKeyListener;
import android.text.method.TextKeyListener.Capitalize;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.Toast;

import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.odk.collect.android.R;
import org.odk.collect.android.activities.FormEntryActivity;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.external.ExternalAppsUtils;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.taskModel.FormLaunchDetail;
import org.odk.collect.android.utilities.ActivityAvailability;
import org.odk.collect.android.utilities.ManageForm;
import org.odk.collect.android.utilities.SoftKeyboardUtils;
import org.odk.collect.android.utilities.ViewIds;
import org.odk.collect.android.widgets.interfaces.BinaryWidget;

import timber.log.Timber;

/**
 * Launch another form
 *
 * @author neilpenman@smap.com.au
 */
@SuppressLint("ViewConstructor")
public class SmapFormWidget extends QuestionWidget implements BinaryWidget {
    // If an extra with this key is specified, it will be parsed as a URI and used as intent data
    private static final String URI_KEY = "uri_data";

    protected EditText answer;
    private boolean hasExApp = true;
    public final Button launchIntentButton;
    public EditText launching;
    private final Drawable textBackground;

    private ManageForm mf;
    private ManageForm.ManageFormDetails mfd;

    private ActivityAvailability activityAvailability;

    private long formId;

    public SmapFormWidget(Context context, QuestionDetails questionDetails, String appearance, boolean readOnlyOverride) {

        super(context, questionDetails);

        TableLayout.LayoutParams params = new TableLayout.LayoutParams();
        params.setMargins(7, 5, 7, 5);

        /*
         * Get the details on the form to be launched
         */
        boolean validForm = true;
        mf = new ManageForm();

        String formIdent = questionDetails.getPrompt().getQuestion().getAdditionalAttribute(null, "form_identifier");
        String key_question = questionDetails.getPrompt().getQuestion().getAdditionalAttribute(null, "key_question");

        if(formIdent == null) {
            validForm = false;
            Toast.makeText(getContext(),
                    Collect.getInstance().getString(R.string.smap_form_not_specified),
                    Toast.LENGTH_SHORT)
                    .show();
        } else {
            mfd = mf.getFormDetailsNoVersion(formIdent);
            validForm = mfd.exists;
            if(!validForm) {
                Toast.makeText(getContext(),
                        Collect.getInstance().getString(R.string.smap_form_not_found).replace("%s", formIdent),
                        Toast.LENGTH_SHORT)
                        .show();
            }
        }

        // set text formatting
        answer = new EditText(context);
        answer.setId(ViewIds.generateViewId());
        answer.setTextSize(TypedValue.COMPLEX_UNIT_DIP, getAnswerFontSize());
        answer.setLayoutParams(params);
        textBackground = answer.getBackground();
        answer.setBackground(null);
        answer.setTextColor(themeUtils.getColorOnSurface());

        answer.getText();
        // capitalize nothing
        answer.setKeyListener(new TextKeyListener(Capitalize.NONE, false));

        // needed to make long read only text scroll
        answer.setHorizontallyScrolling(false);
        answer.setSingleLine(false);

        String s = questionDetails.getPrompt().getAnswerText();
        if(s != null && s.startsWith("::")) {
            validForm = false;
            Toast.makeText(getContext(),
                    Collect.getInstance().getString(R.string.smap_form_completed, mfd.formName),
                    Toast.LENGTH_SHORT)
                    .show();
            answer.setText(s);
        }


        if (getFormEntryPrompt().isReadOnly() || readOnlyOverride || !validForm) {
            answer.setFocusable(false);
            answer.setEnabled(false);
        }

        String v = getFormEntryPrompt().getSpecialFormQuestionText("buttonText");
        String buttonText = (v != null) ? v : context.getString(R.string.launch_app);

        if(validForm) {
            buttonText += " " + mfd.formName;
        }

        launchIntentButton = getSimpleButton(buttonText);
        launchIntentButton.setEnabled(validForm && !readOnlyOverride);

        // set text formatting
        launching = new EditText(context);
        launching.setTextSize(TypedValue.COMPLEX_UNIT_DIP, getAnswerFontSize());
        launching.setLayoutParams(params);
        launching.setBackground(null);
        launching.setTextColor(themeUtils.getColorOnSurface());
        launching.setGravity(Gravity.CENTER);
        launching.setVisibility(GONE);
        String launchingText = context.getString(R.string.smap_starting_form).replace("%s", mfd.formName);
        launching.setText(launchingText);

        // finish complex layout
        LinearLayout answerLayout = new LinearLayout(getContext());
        answerLayout.setOrientation(LinearLayout.VERTICAL);
        answerLayout.addView(launchIntentButton);
        answerLayout.addView(answer);
        answerLayout.addView(launching);
        addAnswerView(answerLayout);

        /*
        if(appearance != null && appearance.contains("auto")) {
            launchIntentButton.performClick();
            launchIntentButton.setVisibility(GONE);
            launching.setVisibility(VISIBLE);
        }
        */

    }

    @Override
    public void clearAnswer() {
        answer.setText(null);
    }

    @Override
    public IAnswerData getAnswer() {
        String s = answer.getText().toString();
        return !s.isEmpty() ? new StringData(s) : null;
    }

    /**
     * Allows answer to be set externally in {@link FormEntryActivity}.
     */
    @Override
    public void setBinaryData(Object answer) {
        StringData stringData = ExternalAppsUtils.asStringData(answer);
        this.answer.setText(stringData == null ? null : stringData.getValue().toString());
    }

    @Override
    public void setFocus(Context context) {
        if (hasExApp) {
            SoftKeyboardUtils.hideSoftKeyboard(answer);
            // focus on launch button
            launchIntentButton.requestFocus();
        } else {
            if (!getFormEntryPrompt().isReadOnly()) {
                SoftKeyboardUtils.showSoftKeyboard(answer);
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
                SoftKeyboardUtils.hideSoftKeyboard(answer);
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return !event.isAltPressed() && super.onKeyDown(keyCode, event);
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        answer.setOnLongClickListener(l);
        launchIntentButton.setOnLongClickListener(l);
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        answer.cancelLongPress();
        launchIntentButton.cancelLongPress();
    }

    @Override
    public void onButtonClick(int buttonId) {

        // 1. Save restore information in collect app
        String instancePath = Collect.getInstance().getFormController().getInstanceFile().getAbsolutePath();
        FormIndex formIndex = Collect.getInstance().getFormController().getFormIndex();

        Collect.getInstance().pushToFormStack(new FormLaunchDetail(instancePath, formIndex, (String) Collect.getInstance().getFormEntryActivity().getTitle()));

        // 2. Set form details to be launched in collect app
        Collect.getInstance().pushToFormStack(new FormLaunchDetail(mfd.id, mfd.formName));

        // 3. Save and exit current form
        Collect.getInstance().getFormEntryActivity().saveDataToDisk(true, false,
                null, false, false);
    }

    private void focusAnswer() {
        SoftKeyboardUtils.showSoftKeyboard(answer);
    }

    private void onException(String toastText) {
        hasExApp = false;
        if (!getFormEntryPrompt().isReadOnly()) {
            answer.setBackground(textBackground);
            answer.setFocusable(true);
            answer.setFocusableInTouchMode(true);
            answer.setEnabled(true);
        }
        launchIntentButton.setEnabled(false);
        launchIntentButton.setFocusable(false);
        cancelWaitingForData();

        Toast.makeText(getContext(),
                toastText, Toast.LENGTH_SHORT)
                .show();
        Timber.d(toastText);
        focusAnswer();
    }
}
