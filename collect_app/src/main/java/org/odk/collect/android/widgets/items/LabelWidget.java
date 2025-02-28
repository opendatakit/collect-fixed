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
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.IAnswerData;
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
import org.odk.collect.android.widgets.interfaces.SelectChoiceLoader;
import org.odk.collect.android.widgets.warnings.SpacesInUnderlyingValuesWarning;

import java.io.File;
import java.util.List;

import timber.log.Timber;

/**
 * The Label Widget does not return an answer. The purpose of this widget is to be the top entry in
 * a field-list with a bunch of list widgets below. This widget provides the labels, so that the
 * list widgets can hide their labels and reduce the screen clutter. This class is essentially
 * ListWidget with all the answer generating code removed.
 *
 * @author Jeff Beorse
 */
@SuppressLint("ViewConstructor")
public class LabelWidget extends QuestionWidget {

    private final List<SelectChoice> items;

    public LabelWidget(Context context, QuestionDetails questionDetails, SelectChoiceLoader selectChoiceLoader, Dependencies dependencies) {
        super(context, dependencies, questionDetails);
        render();

        items = ItemsWidgetUtils.loadItemsAndHandleErrors(this, questionDetails.getPrompt(), selectChoiceLoader);
        addItems(context, questionDetails);
        SpacesInUnderlyingValuesWarning.forQuestionWidget(this).renderWarningIfNecessary(items);
    }

    private void addItems(Context context, QuestionDetails questionDetails) {
        // Layout holds the horizontal list of buttons
        LinearLayout listItems = findViewById(R.id.answer_container);

        if (items != null) {
            for (int i = 0; i < items.size(); i++) {

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
                                Timber.e(e);
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
                        Timber.e(e, "Invalid image reference");
                    }

                }

                // build text label. Don't assign the text to the built in label to he
                // button because it aligns horizontally, and we want the label on top
                TextView label = new TextView(getContext());
                label.setText(HtmlUtils.textToHtml(questionDetails.getPrompt().getSelectChoiceText(items.get(i))));
                label.setTextSize(TypedValue.COMPLEX_UNIT_DIP, QuestionFontSizeUtils.getFontSize(settings, QuestionFontSizeUtils.FontSize.HEADLINE_6));
                label.setGravity(Gravity.CENTER_HORIZONTAL);

                // answer layout holds the label text/image on top and the radio button on bottom
                LinearLayout answer = new LinearLayout(getContext());
                answer.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams headerParams =
                        new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                                LayoutParams.WRAP_CONTENT);
                headerParams.gravity = Gravity.CENTER_HORIZONTAL;

                LinearLayout.LayoutParams buttonParams =
                        new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                                LayoutParams.WRAP_CONTENT);
                buttonParams.gravity = Gravity.CENTER_HORIZONTAL;

                if (imageView != null) {
                    imageView.setScaleType(ScaleType.CENTER);
                    answer.addView(imageView, headerParams);
                } else if (missingImage != null) {
                    answer.addView(missingImage, headerParams);
                } else {
                    label.setId(labelId);
                    answer.addView(label, headerParams);
                }
                answer.setPadding(4, 0, 4, 0);

                // Each button gets equal weight
                LinearLayout.LayoutParams answerParams =
                        new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                                LayoutParams.MATCH_PARENT);
                answerParams.weight = 1;
                listItems.addView(answer, answerParams);
            }
        }
    }

    @Override
    protected int getLayout() {
        return R.layout.label_widget;
    }

    @Override
    public void clearAnswer() {
        // Do nothing, no answers to clear
    }

    @Override
    public IAnswerData getAnswer() {
        return null;
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
    }
}
