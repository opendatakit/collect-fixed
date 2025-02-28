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

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
import static org.odk.collect.android.utilities.ApplicationConstants.RequestCodes;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.databinding.AnnotateWidgetBinding;
import org.odk.collect.draw.DrawActivity;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.utilities.Appearances;
import org.odk.collect.android.utilities.FileUtils;
import org.odk.collect.android.utilities.QuestionMediaManager;
import org.odk.collect.android.widgets.utilities.ImageCaptureIntentCreator;
import org.odk.collect.android.widgets.utilities.WaitingForDataRegistry;
import org.odk.collect.androidshared.ui.ToastUtils;
import java.io.File;
import java.util.Locale;

/**
 * Image widget that supports annotations on the image.
 *
 * @author BehrAtherton@gmail.com
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
@SuppressLint("ViewConstructor")
public class AnnotateWidget extends BaseImageWidget {
    AnnotateWidgetBinding binding;

    public AnnotateWidget(Context context, QuestionDetails prompt, QuestionMediaManager questionMediaManager, WaitingForDataRegistry waitingForDataRegistry, String tmpImageFilePath, Dependencies dependencies) {
        super(context, prompt, questionMediaManager, waitingForDataRegistry, tmpImageFilePath, dependencies);
        imageClickHandler = new DrawImageClickHandler(DrawActivity.OPTION_ANNOTATE, RequestCodes.ANNOTATE_IMAGE, org.odk.collect.strings.R.string.annotate_image);
        imageCaptureHandler = new ImageCaptureHandler();

        render();
    }

    @Override
    protected View onCreateAnswerView(Context context, FormEntryPrompt prompt, int answerFontSize) {
        binding = AnnotateWidgetBinding.inflate(((Activity) context).getLayoutInflater());
        errorTextView = binding.errorMessage;
        imageView = binding.image;
        updateAnswer();

        if (getFormEntryPrompt().getAppearanceHint() != null && getFormEntryPrompt().getAppearanceHint().toLowerCase(Locale.ENGLISH).contains(Appearances.NEW)) {
            binding.chooseButton.setVisibility(View.GONE);
        }

        if (binaryName == null || binding.image.getVisibility() == GONE) {
            binding.annotateButton.setEnabled(false);
        }

        binding.captureButton.setOnClickListener(v -> getPermissionsProvider().requestCameraPermission((Activity) getContext(), () -> {
            Intent intent = ImageCaptureIntentCreator.imageCaptureIntent(getFormEntryPrompt(), getContext(), tmpImageFilePath);
            imageCaptureHandler.captureImage(intent, RequestCodes.IMAGE_CAPTURE, org.odk.collect.strings.R.string.annotate_image);
        }));
        binding.chooseButton.setOnClickListener(v -> imageCaptureHandler.chooseImage(org.odk.collect.strings.R.string.annotate_image));
        binding.annotateButton.setOnClickListener(v -> imageClickHandler.clickImage("annotateButton"));
        binding.image.setOnClickListener(v -> imageClickHandler.clickImage("viewImage"));

        if (questionDetails.isReadOnly()) {
            binding.captureButton.setVisibility(View.GONE);
            binding.chooseButton.setVisibility(View.GONE);
            binding.annotateButton.setVisibility(View.GONE);
        }

        return binding.getRoot();
    }

    @Override
    public Intent addExtrasToIntent(Intent intent) {
        Bitmap bmp = null;
        if (binding.image.getDrawable() != null) {
            bmp = ((BitmapDrawable) binding.image.getDrawable()).getBitmap();
        }

        int screenOrientation =  bmp != null && bmp.getHeight() > bmp.getWidth() ?
                SCREEN_ORIENTATION_PORTRAIT : SCREEN_ORIENTATION_LANDSCAPE;

        intent.putExtra(DrawActivity.SCREEN_ORIENTATION, screenOrientation);
        return intent;
    }

    @Override
    protected boolean doesSupportDefaultValues() {
        return true;
    }

    @Override
    public void clearAnswer() {
        super.clearAnswer();
        binding.annotateButton.setEnabled(false);
        binding.captureButton.setText(getContext().getString(org.odk.collect.strings.R.string.capture_image));
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        binding.captureButton.setOnLongClickListener(l);
        binding.chooseButton.setOnLongClickListener(l);
        binding.annotateButton.setOnLongClickListener(l);
        super.setOnLongClickListener(l);
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        binding.captureButton.cancelLongPress();
        binding.chooseButton.cancelLongPress();
        binding.annotateButton.cancelLongPress();
    }

    @Override
    public void setData(Object newImageObj) {
        if (newImageObj instanceof File) {
            String mimeType = FileUtils.getMimeType((File) newImageObj);
            if ("image/gif".equals(mimeType)) {
                ToastUtils.showLongToast(org.odk.collect.strings.R.string.gif_not_supported);
            } else {
                super.setData(newImageObj);
                binding.annotateButton.setEnabled(binaryName != null);
            }
        }
    }
}
