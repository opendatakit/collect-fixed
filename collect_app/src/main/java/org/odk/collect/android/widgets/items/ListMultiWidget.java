/*
 * Copyright (C) 2011 University of Washington
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

package org.odk.collect.android.widgets.items;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatCheckBox;

import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.SelectMultiData;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.form.api.FormEntryCaption;
import org.odk.collect.android.R;
import org.odk.collect.android.dynamicpreload.ExternalSelectChoice;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.utilities.HtmlUtils;
import org.odk.collect.android.widgets.utilities.QuestionFontSizeUtils;
import org.odk.collect.androidshared.bitmap.ImageFileUtils;
import org.odk.collect.android.widgets.QuestionWidget;
import org.odk.collect.android.widgets.interfaces.MultiChoiceWidget;
import org.odk.collect.android.widgets.interfaces.SelectChoiceLoader;
import org.odk.collect.android.widgets.warnings.SpacesInUnderlyingValuesWarning;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * ListMultiWidget handles multiple selection fields using check boxes. The check boxes are aligned
 * horizontally. They are typically meant to be used in a field list, where multiple questions with
 * the same multiple choice answers can sit on top of each other and make a grid of buttons that is
 * easy to navigate quickly. Optionally, you can turn off the labels. This would be done if a label
 * widget was at the top of your field list to provide the labels. If audio or video are specified
 * in the select answers they are ignored. This class is almost identical to ListWidget, except it
 * uses checkboxes. It also did not require a custom clickListener class.
 *
 * @author Jeff Beorse (jeff@beorse.net)
 */
@SuppressLint("ViewConstructor")
public class ListMultiWidget extends QuestionWidget implements MultiChoiceWidget {

    final ArrayList<CheckBox> checkBoxes;
    private final boolean displayLabel;
    private final List<SelectChoice> items;

    @SuppressWarnings("unchecked")
    public ListMultiWidget(Context context, QuestionDetails questionDetails, boolean displayLabel, SelectChoiceLoader selectChoiceLoader, Dependencies dependencies) {
        super(context, dependencies, questionDetails);
        render();

        items = ItemsWidgetUtils.loadItemsAndHandleErrors(this, questionDetails.getPrompt(), selectChoiceLoader);

        checkBoxes = new ArrayList<>();
        this.displayLabel = displayLabel;

        // Layout holds the horizontal list of buttons
        LinearLayout buttonLayout = findViewById(R.id.answer_container);

        List<Selection> ve = new ArrayList<>();
        if (questionDetails.getPrompt().getAnswerValue() != null) {
            ve = (List<Selection>) questionDetails.getPrompt().getAnswerValue().getValue();
        }

        if (items != null) {
            for (int i = 0; i < items.size(); i++) {

                AppCompatCheckBox c = new AppCompatCheckBox(getContext());
                c.setTag(i);
                c.setId(View.generateViewId());
                c.setFocusable(!questionDetails.getPrompt().isReadOnly());
                c.setEnabled(!questionDetails.getPrompt().isReadOnly());

                for (int vi = 0; vi < ve.size(); vi++) {
                    // match based on value, not key
                    if (items.get(i).getValue().equals(ve.get(vi).getValue())) {
                        c.setChecked(true);
                        break;
                    }

                }
                checkBoxes.add(c);

                // when clicked, check for readonly before toggling
                c.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (getFormEntryPrompt().isReadOnly()) {
                            if (buttonView.isChecked()) {
                                buttonView.setChecked(false);
                            } else {
                                buttonView.setChecked(true);
                            }
                        }

                        widgetValueChanged();
                    }
                });

                String imageURI;
                if (items.get(i) instanceof ExternalSelectChoice) {
                    imageURI = ((ExternalSelectChoice) items.get(i)).getImage();
                } else {
                    imageURI = questionDetails.getPrompt().getSpecialFormSelectChoiceText(items.get(i),
                            FormEntryCaption.TEXT_FORM_IMAGE);
                }

                // build image view (if an image is provided)
                ImageView imageView = null;
                TextView missingImage = null;

                final int labelId = View.generateViewId();

                // Now set up the image view
                String errorMsg = null;
                if (imageURI != null) {
                    try {
                        String imageFilename =
                                ReferenceManager.instance().deriveReference(imageURI).getLocalURI();
                        final File imageFile = new File(imageFilename);
                        if (imageFile.exists()) {
                            Bitmap b = null;
                            try {
                                DisplayMetrics metrics = context.getResources().getDisplayMetrics();
                                int screenWidth = metrics.widthPixels;
                                int screenHeight = metrics.heightPixels;
                                b = ImageFileUtils.getBitmapScaledToDisplay(imageFile, screenHeight, screenWidth);
                            } catch (OutOfMemoryError e) {
                                errorMsg = "ERROR: " + e.getMessage();
                            }

                            if (b != null) {
                                imageView = new ImageView(getContext());
                                imageView.setPadding(2, 2, 2, 2);
                                imageView.setAdjustViewBounds(true);
                                imageView.setImageBitmap(b);
                                imageView.setId(labelId);
                            } else if (errorMsg == null) {
                                // An error hasn't been logged and loading the image failed, so it's
                                // likely
                                // a bad file.
                                errorMsg = getContext().getString(org.odk.collect.strings.R.string.file_invalid, imageFile);

                            }
                        } else {
                            // An error hasn't been logged. We should have an image, but the file
                            // doesn't
                            // exist.
                            errorMsg = getContext().getString(org.odk.collect.strings.R.string.file_missing, imageFile);
                        }

                        if (errorMsg != null) {
                            // errorMsg is only set when an error has occured
                            Timber.e(new Error(errorMsg));
                            missingImage = new TextView(getContext());
                            missingImage.setText(errorMsg);

                            missingImage.setPadding(2, 2, 2, 2);
                            missingImage.setId(labelId);
                        }

                    } catch (InvalidReferenceException e) {
                        Timber.d(e, "Invalid image reference due to %s ", e.getMessage());
                    }
                }

                // build text label. Don't assign the text to the built in label to he
                // button because it aligns horizontally, and we want the label on top
                TextView label = new TextView(getContext());
                label.setText(HtmlUtils.textToHtml(questionDetails.getPrompt().getSelectChoiceText(items.get(i))));
                label.setTextSize(TypedValue.COMPLEX_UNIT_DIP, QuestionFontSizeUtils.getFontSize(settings, QuestionFontSizeUtils.FontSize.HEADLINE_6));
                label.setGravity(Gravity.CENTER_HORIZONTAL);
                if (!displayLabel) {
                    label.setVisibility(View.GONE);
                }

                // answer layout holds the label text/image on top and the radio button on bottom
                RelativeLayout answer = new RelativeLayout(getContext());
                RelativeLayout.LayoutParams headerParams =
                        new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                                LayoutParams.WRAP_CONTENT);
                headerParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                headerParams.addRule(RelativeLayout.CENTER_HORIZONTAL);

                RelativeLayout.LayoutParams buttonParams =
                        new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                                LayoutParams.WRAP_CONTENT);
                buttonParams.addRule(RelativeLayout.CENTER_HORIZONTAL);

                if (imageView != null) {
                    imageView.setScaleType(ScaleType.CENTER);
                    if (!displayLabel) {
                        imageView.setVisibility(View.GONE);
                    }
                    answer.addView(imageView, headerParams);
                } else if (missingImage != null) {
                    answer.addView(missingImage, headerParams);
                } else {
                    if (displayLabel) {
                        label.setId(labelId);
                        answer.addView(label, headerParams);
                    }

                }
                if (displayLabel) {
                    buttonParams.addRule(RelativeLayout.BELOW, labelId);
                }
                answer.addView(c, buttonParams);
                answer.setPadding(4, 0, 4, 0);

                // /Each button gets equal weight
                LinearLayout.LayoutParams answerParams =
                        new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                                LayoutParams.MATCH_PARENT);
                answerParams.weight = 1;

                buttonLayout.addView(answer, answerParams);
            }

            SpacesInUnderlyingValuesWarning
                    .forQuestionWidget(this)
                    .renderWarningIfNecessary(items);
        }
    }

    @Override
    public void clearAnswer() {
        for (int i = 0; i < checkBoxes.size(); i++) {
            CheckBox c = checkBoxes.get(i);
            if (c.isChecked()) {
                c.setChecked(false);
            }
        }
        widgetValueChanged();
    }

    @Override
    public IAnswerData getAnswer() {
        List<Selection> vc = new ArrayList<>();
        for (int i = 0; i < checkBoxes.size(); i++) {
            CheckBox c = checkBoxes.get(i);
            if (c.isChecked()) {
                vc.add(new Selection(items.get(i)));
            }
        }

        if (vc.isEmpty()) {
            return null;
        } else {
            return new SelectMultiData(vc);
        }

    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        for (CheckBox c : checkBoxes) {
            c.setOnLongClickListener(l);
        }
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        for (CheckBox c : checkBoxes) {
            c.cancelLongPress();
        }
    }

    @Override
    public int getChoiceCount() {
        return checkBoxes.size();
    }

    @Override
    public void setChoiceSelected(int choiceIndex, boolean isSelected) {
        checkBoxes.get(choiceIndex).setChecked(isSelected);
    }

    @Override
    protected int getLayout() {
        return R.layout.label_widget;
    }

    public boolean shouldDisplayLabel() {
        return displayLabel;
    }
}
