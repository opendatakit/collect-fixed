package org.odk.collect.android.widgets.items;

import static android.widget.RelativeLayout.CENTER_HORIZONTAL;
import static android.widget.RelativeLayout.CENTER_IN_PARENT;
import static android.widget.RelativeLayout.TRUE;
import static org.odk.collect.android.utilities.ViewUtils.pxFromDp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.radiobutton.MaterialRadioButton;

import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.SelectOneData;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.dynamicpreload.ExternalSelectChoice;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.utilities.HtmlUtils;
import org.odk.collect.android.widgets.QuestionWidget;
import org.odk.collect.android.widgets.interfaces.SelectChoiceLoader;
import org.odk.collect.androidshared.bitmap.ImageFileUtils;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import timber.log.Timber;

@SuppressLint("ViewConstructor")
public class LikertWidget extends QuestionWidget {
    LinearLayout view;
    private RadioButton checkedButton;
    private final LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1);
    private final RelativeLayout.LayoutParams textViewParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    private final RelativeLayout.LayoutParams imageViewParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    private final RelativeLayout.LayoutParams radioButtonsParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    private final RelativeLayout.LayoutParams buttonViewParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    private final List<SelectChoice> items;

    HashMap<RadioButton, String> buttonsToName;

    public LikertWidget(Context context, QuestionDetails questionDetails, SelectChoiceLoader selectChoiceLoader, Dependencies dependencies) {
        super(context, dependencies, questionDetails);
        items = ItemsWidgetUtils.loadItemsAndHandleErrors(this, questionDetails.getPrompt(), selectChoiceLoader);

        setMainViewLayoutParameters();
        setStructures();
        setButtonListener();
        setSavedButton();
        render();
    }

    @Override
    protected View onCreateAnswerView(@NonNull Context context, @NonNull FormEntryPrompt prompt, int answerFontSize) {
        return view;
    }

    public void setMainViewLayoutParameters() {
        view = new LinearLayout(getContext());
        view.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(params);
    }

    // Inserts the selected button from a saved state
    public void setSavedButton() {
        if (getFormEntryPrompt().getAnswerValue() != null) {
            String name = ((Selection) getFormEntryPrompt().getAnswerValue()
                    .getValue()).getValue();
            for (RadioButton bu : buttonsToName.keySet()) {
                if (buttonsToName.get(bu).equals(name)) {
                    checkedButton = bu;
                    checkedButton.setChecked(true);
                }
            }
        }
    }

    @Override
    public IAnswerData getAnswer() {
        if (checkedButton == null) {
            return null;
        } else {
            int selectedIndex = -1;
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).getValue().equals(buttonsToName.get(checkedButton))) {
                    selectedIndex = i;
                }
            }
            if (selectedIndex == -1) {
                return null;
            }
            SelectChoice sc = items.get(selectedIndex);
            return new SelectOneData(new Selection(sc));
        }
    }

    public void setStructures() {
        buttonsToName = new HashMap<>();
        for (int i = 0; i < items.size(); i++) {
            RelativeLayout buttonView = new RelativeLayout(this.getContext());
            buttonViewParams.addRule(CENTER_IN_PARENT, TRUE);
            buttonView.setLayoutParams(buttonViewParams);
            RadioButton button = getRadioButton(i);

            buttonsToName.put(button, items.get(i).getValue());
            buttonView.addView(button);

            LinearLayout optionView = getLinearLayout();
            optionView.addView(buttonView);

            ImageView imgView = getImageAsImageView(i);
            // checks if image is set or valid
            if (imgView != null) {
                optionView.addView(imgView);
            }
            TextView choice = getTextView();
            choice.setText(HtmlUtils.textToHtml(getFormEntryPrompt().getSelectChoiceText(items.get(i))));

            optionView.addView(choice);

            optionView.setEnabled(!getFormEntryPrompt().isReadOnly());
            optionView.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    RadioButton r = button;
                    if (checkedButton != null) {
                        checkedButton.setChecked(false);
                    }
                    checkedButton = r;
                    checkedButton.setChecked(true);
                    widgetValueChanged();
                }
            });
            view.addView(optionView);
        }
    }

    // Creates image view for choice
    public ImageView getImageView() {
        ImageView view = new ImageView(getContext());
        view.setLayoutParams(imageViewParams);
        return view;
    }

    public RadioButton getRadioButton(int i) {
        MaterialRadioButton button = new MaterialRadioButton(getContext());
        button.setId(View.generateViewId());
        button.setEnabled(!getFormEntryPrompt().isReadOnly());
        button.setFocusable(!getFormEntryPrompt().isReadOnly());
        radioButtonsParams.addRule(CENTER_HORIZONTAL, TRUE);
        radioButtonsParams.setMargins(0, pxFromDp(getContext(), 16), 0, pxFromDp(getContext(), 8));
        button.setLayoutParams(radioButtonsParams);

        // Remove radio button padding - this needs minHeight/Width rather than padding
        button.setMinHeight(0);
        button.setMinWidth(0);
        button.setMinimumHeight(0);
        button.setMinimumWidth(0);

        button.setGravity(Gravity.CENTER);
        return button;
    }

    // Creates text view for choice
    public TextView getTextView() {
        TextView view = new TextView(getContext());
        view.setGravity(Gravity.CENTER);
        view.setPadding(2, 2, 2, 2);
        view.setLayoutParams(textViewParams);
        return view;
    }

    // Linear Layout for new choice
    public LinearLayout getLinearLayout() {
        LinearLayout optionView = new LinearLayout(getContext());
        optionView.setGravity(Gravity.CENTER);
        optionView.setLayoutParams(linearLayoutParams);
        linearLayoutParams.setMargins(-1, 0, -1, 0);
        optionView.setOrientation(LinearLayout.VERTICAL);
        return optionView;
    }

    public void setButtonListener() {
        for (RadioButton button : buttonsToName.keySet()) {
            button.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    RadioButton r = (RadioButton) v;
                    if (checkedButton != null) {
                        checkedButton.setChecked(false);
                    }
                    checkedButton = r;
                    checkedButton.setChecked(true);
                    widgetValueChanged();
                }
            });
        }
    }

    public ImageView getImageAsImageView(int index) {
        ImageView view = getImageView();
        String imageURI;
        if (items.get(index) instanceof ExternalSelectChoice) {
            imageURI = ((ExternalSelectChoice) items.get(index)).getImage();
        } else {
            imageURI = getFormEntryPrompt().getSpecialFormSelectChoiceText(items.get(index),
                    FormEntryCaption.TEXT_FORM_IMAGE);
        }
        if (imageURI != null) {
            String error = setImageFromOtherSource(imageURI, view);
            if (error != null) {
                return null;
            }
            return view;
        } else {
            return null;
        }
    }

    public String setImageFromOtherSource(String imageURI, ImageView imageView) {
        String errorMsg = null;
        try {
            String imageFilename =
                    ReferenceManager.instance().deriveReference(imageURI).getLocalURI();
            final File imageFile = new File(imageFilename);
            if (imageFile.exists()) {
                Bitmap b = null;
                try {
                    DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
                    int screenWidth = metrics.widthPixels;
                    int screenHeight = metrics.heightPixels;
                    b = ImageFileUtils.getBitmapScaledToDisplay(imageFile, screenHeight, screenWidth);
                } catch (OutOfMemoryError e) {
                    errorMsg = "ERROR: " + e.getMessage();
                }

                if (b != null) {
                    imageView.setAdjustViewBounds(true);
                    imageView.setImageBitmap(b);
                } else if (errorMsg == null) {
                    // Loading the image failed. The image work when in .jpg format
                    errorMsg = getContext().getString(org.odk.collect.strings.R.string.file_invalid, imageFile);

                }
            } else {
                errorMsg = getContext().getString(org.odk.collect.strings.R.string.file_missing, imageFile);
            }
            if (errorMsg != null) {
                Timber.e(new Error(errorMsg));
            }
        } catch (InvalidReferenceException e) {
            Timber.d(e, "Invalid image reference due to %s ", e.getMessage());
        }
        return errorMsg;
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        for (RadioButton r : buttonsToName.keySet()) {
            r.setOnLongClickListener(l);
        }
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        for (RadioButton r : buttonsToName.keySet()) {
            r.cancelLongPress();
        }
    }

    @Override
    public void clearAnswer() {
        if (checkedButton != null) {
            checkedButton.setChecked(false);
        }
        checkedButton = null;
        widgetValueChanged();
    }
}
