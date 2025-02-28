/*
 * Copyright (C) 2009 University of Washington
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

import static org.odk.collect.android.utilities.ApplicationConstants.RequestCodes;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.databinding.ImageWidgetBinding;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.storage.StoragePathProvider;
import org.odk.collect.android.storage.StorageSubdirectory;
import org.odk.collect.android.utilities.Appearances;
import org.odk.collect.android.utilities.QuestionMediaManager;
import org.odk.collect.android.widgets.utilities.ImageCaptureIntentCreator;
import org.odk.collect.android.widgets.utilities.WaitingForDataRegistry;
import org.odk.collect.androidshared.system.CameraUtils;
import org.odk.collect.selfiecamera.CaptureSelfieActivity;
import java.util.Locale;

/**
 * Widget that allows user to take pictures, sounds or video and add them to the form.
 *
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */

@SuppressLint("ViewConstructor")
public class ImageWidget extends BaseImageWidget {
    ImageWidgetBinding binding;

    private boolean selfie;

    public ImageWidget(Context context, final QuestionDetails prompt, QuestionMediaManager questionMediaManager, WaitingForDataRegistry waitingForDataRegistry, String tmpImageFilePath, Dependencies dependencies) {
        super(context, prompt, questionMediaManager, waitingForDataRegistry, tmpImageFilePath, dependencies);
        imageClickHandler = new ViewImageClickHandler();
        imageCaptureHandler = new ImageCaptureHandler();

        render();
        updateAnswer();
    }

    @Override
    protected View onCreateAnswerView(Context context, FormEntryPrompt prompt, int answerFontSize) {
        binding = ImageWidgetBinding.inflate(((Activity) context).getLayoutInflater());

        String appearance = prompt.getAppearanceHint();
        selfie = Appearances.isFrontCameraAppearance(prompt);
        if (selfie || ((appearance != null && appearance.toLowerCase(Locale.ENGLISH).contains(Appearances.NEW)))) {
            binding.chooseButton.setVisibility(View.GONE);
        }

        binding.captureButton.setOnClickListener(v -> getPermissionsProvider().requestCameraPermission((Activity) getContext(), this::captureImage));
        binding.chooseButton.setOnClickListener(v -> imageCaptureHandler.chooseImage(org.odk.collect.strings.R.string.choose_image));
        binding.image.setOnClickListener(v -> imageClickHandler.clickImage("viewImage"));

        if (questionDetails.isReadOnly()) {
            binding.captureButton.setVisibility(View.GONE);
            binding.chooseButton.setVisibility(View.GONE);
        }

        errorTextView = binding.errorMessage;
        imageView = binding.image;

        return binding.getRoot();
    }

    @Override
    public Intent addExtrasToIntent(Intent intent) {
        return intent;
    }

    @Override
    protected boolean doesSupportDefaultValues() {
        return false;
    }

    @Override
    public void clearAnswer() {
        super.clearAnswer();
        binding.captureButton.setText(getContext().getString(org.odk.collect.strings.R.string.capture_image));
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        binding.captureButton.setOnLongClickListener(l);
        binding.chooseButton.setOnLongClickListener(l);
        super.setOnLongClickListener(l);
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        binding.captureButton.cancelLongPress();
        binding.chooseButton.cancelLongPress();
    }

    private void captureImage() {
        if (selfie && new CameraUtils().isFrontCameraAvailable(getContext())) {
            Intent intent = new Intent(getContext(), CaptureSelfieActivity.class);
            intent.putExtra(CaptureSelfieActivity.EXTRA_TMP_PATH, new StoragePathProvider().getOdkDirPath(StorageSubdirectory.CACHE));
            imageCaptureHandler.captureImage(intent, RequestCodes.MEDIA_FILE_PATH, org.odk.collect.strings.R.string.capture_image);
        } else {
            Intent intent = ImageCaptureIntentCreator.imageCaptureIntent(getFormEntryPrompt(), getContext(), tmpImageFilePath);
            imageCaptureHandler.captureImage(intent, RequestCodes.IMAGE_CAPTURE, org.odk.collect.strings.R.string.capture_image);
        }
    }
}
