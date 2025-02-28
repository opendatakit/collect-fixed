/*
 * Copyright 2018 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.odk.collect.android.widgets;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.core.reference.InvalidReferenceException;
import org.odk.collect.draw.DrawActivity;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.utilities.ApplicationConstants;
import org.odk.collect.android.utilities.QuestionMediaManager;
import org.odk.collect.android.widgets.interfaces.FileWidget;
import org.odk.collect.android.widgets.interfaces.WidgetDataReceiver;
import org.odk.collect.android.widgets.utilities.WaitingForDataRegistry;
import org.odk.collect.androidshared.ui.multiclicksafe.MultiClickGuard;
import org.odk.collect.imageloader.GlideImageLoader;

import java.io.File;

import timber.log.Timber;

public abstract class BaseImageWidget extends QuestionWidget implements FileWidget, WidgetDataReceiver {

    protected ImageView imageView;
    protected String binaryName;
    protected TextView errorTextView;
    protected LinearLayout answerLayout;

    protected ImageClickHandler imageClickHandler;
    protected ExternalImageCaptureHandler imageCaptureHandler;

    private final WaitingForDataRegistry waitingForDataRegistry;
    private final QuestionMediaManager questionMediaManager;
    protected final String tmpImageFilePath;

    public BaseImageWidget(Context context, QuestionDetails prompt, QuestionMediaManager questionMediaManager,
                           WaitingForDataRegistry waitingForDataRegistry, String tmpImageFilePath, Dependencies dependencies) {
        super(context, dependencies, prompt);
        this.questionMediaManager = questionMediaManager;
        this.waitingForDataRegistry = waitingForDataRegistry;
        this.tmpImageFilePath = tmpImageFilePath;

        binaryName = getFormEntryPrompt().getAnswerText();
    }

    @Override
    public IAnswerData getAnswer() {
        return binaryName == null ? null : new StringData(binaryName);
    }

    @Override
    public void clearAnswer() {
        deleteFile();
        imageView.setImageDrawable(null);
        imageView.setVisibility(View.GONE);
        errorTextView.setVisibility(View.GONE);
        widgetValueChanged();
    }

    @Override
    public void deleteFile() {
        questionMediaManager.deleteAnswerFile(getFormEntryPrompt().getIndex().toString(), binaryName);
        binaryName = null;
    }

    @Override
    public void setData(Object object) {
        if (binaryName != null) {
            deleteFile();
        }

        if (object instanceof File) {
            File newImage = (File) object;
            if (newImage.exists()) {
                questionMediaManager.replaceAnswerFile(getFormEntryPrompt().getIndex().toString(), newImage.getAbsolutePath());
                binaryName = newImage.getName();
                updateAnswer();
                widgetValueChanged();
            } else {
                Timber.e(new Error("NO IMAGE EXISTS at: " + newImage.getAbsolutePath()));
            }
        } else {
            Timber.e(new Error("ImageWidget's setBinaryData must receive a File object."));
        }
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        if (imageView != null) {
            imageView.setOnLongClickListener(l);
        }
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        if (imageView != null) {
            imageView.cancelLongPress();
        }
    }

    protected void updateAnswer() {
        imageView.setVisibility(View.GONE);
        errorTextView.setVisibility(View.GONE);

        if (binaryName != null) {
            File f = getFile();
            if (f != null && f.exists()) {
                imageView.setVisibility(View.VISIBLE);
                imageLoader.loadImage(imageView, f, ImageView.ScaleType.FIT_CENTER, new GlideImageLoader.ImageLoaderCallback() {
                    @Override
                    public void onLoadFailed() {
                        imageView.setVisibility(View.GONE);
                        errorTextView.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onLoadSucceeded() {
                    }
                });
            }
        }
    }

    /**
     * Enables a subclass to add extras to the intent before launching the draw activity.
     *
     * @param intent to add extras
     * @return intent with added extras
     */
    public abstract Intent addExtrasToIntent(Intent intent);

    /**
     * Interface for Clicking on Images
     */
    protected interface ImageClickHandler {
        void clickImage(String context);
    }

    /**
     * Class to implement launching of viewing an image Activity
     */
    protected class ViewImageClickHandler implements ImageClickHandler {

        @Override
        public void clickImage(String context) {
            mediaUtils.openFile(getContext(), questionMediaManager.getAnswerFile(binaryName),
                    "image/*");
        }
    }

    /**
     * Class to implement launching of drawing image Activity when clicked
     */
    protected class DrawImageClickHandler implements ImageClickHandler {

        private final String drawOption;
        private final int requestCode;
        private final int stringResourceId;

        public DrawImageClickHandler(String option, final int code, final int resourceId) {
            drawOption = option;
            requestCode = code;
            stringResourceId = resourceId;
        }

        @Override
        public void clickImage(String context) {
            if (MultiClickGuard.allowClick(getClass().getName())) {
                launchDrawActivity();
            }
        }

        private void launchDrawActivity() {
            Intent i = new Intent(getContext(), DrawActivity.class);
            i.putExtra(DrawActivity.OPTION, drawOption);
            File file = getFile();
            if (file != null && file.exists()) {
                i.putExtra(DrawActivity.REF_IMAGE, Uri.fromFile(file));
            }

            i.putExtra(DrawActivity.EXTRA_OUTPUT, Uri.fromFile(new File(tmpImageFilePath)));
            i = addExtrasToIntent(i);
            launchActivityForResult(i, requestCode, stringResourceId);
        }
    }

    /**
     * Interface for choosing or capturing a new image
     */
    protected interface ExternalImageCaptureHandler {
        void captureImage(Intent intent, int requestCode, int stringResource);

        void chooseImage(int stringResource);
    }

    /**
     * Class for launching the image capture or choose image activities
     */
    protected class ImageCaptureHandler implements ExternalImageCaptureHandler {

        @Override
        public void captureImage(Intent intent, final int requestCode, int stringResource) {
            launchActivityForResult(intent, requestCode, stringResource);
        }

        @Override
        public void chooseImage(@IdRes final int stringResource) {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("image/*");
            launchActivityForResult(i, ApplicationConstants.RequestCodes.IMAGE_CHOOSER, stringResource);
        }
    }

    /**
     * Standard method for launching an Activity.
     *
     * @param intent              - The Intent to start
     * @param resourceCode        - Code to return when Activity exits
     * @param errorStringResource - String resource for error toast
     */
    protected void launchActivityForResult(Intent intent, final int resourceCode, final int errorStringResource) {
        try {
            waitingForDataRegistry.waitForData(getFormEntryPrompt().getIndex());
            ((Activity) getContext()).startActivityForResult(intent, resourceCode);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(),
                    getContext().getString(org.odk.collect.strings.R.string.activity_not_found, getContext().getString(errorStringResource)),
                    Toast.LENGTH_SHORT).show();
            waitingForDataRegistry.cancelWaitingForData();
        }
    }

    @Nullable
    private File getFile() {
        if (binaryName == null) {
            return null;
        }

        File file = questionMediaManager.getAnswerFile(binaryName);
        if ((file == null || !file.exists()) && doesSupportDefaultValues()) {
            String filePath = getDefaultFilePath();
            if (filePath != null) {
                return new File(getDefaultFilePath());
            } else {
                return null;
            }
        }

        return file;
    }

    private String getDefaultFilePath() {
        try {
            return referenceManager.deriveReference(binaryName).getLocalURI();
        } catch (InvalidReferenceException e) {
            Timber.w(e);
        }

        return null;
    }

    protected abstract boolean doesSupportDefaultValues();

    public ImageView getImageView() {
        return imageView;
    }

    public TextView getErrorTextView() {
        return errorTextView;
    }
}
