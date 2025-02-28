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
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.databinding.VideoWidgetBinding;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.utilities.Appearances;
import org.odk.collect.android.utilities.QuestionMediaManager;
import org.odk.collect.android.widgets.interfaces.FileWidget;
import org.odk.collect.android.widgets.interfaces.WidgetDataReceiver;
import org.odk.collect.android.widgets.utilities.WaitingForDataRegistry;
import org.odk.collect.settings.keys.ProjectKeys;

import java.io.File;
import java.util.Locale;

import timber.log.Timber;

/**
 * Widget that allows user to take pictures, sounds or video and add them to the
 * form.
 *
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
@SuppressLint("ViewConstructor")
public class VideoWidget extends QuestionWidget implements FileWidget, WidgetDataReceiver {
    private final WaitingForDataRegistry waitingForDataRegistry;
    private final QuestionMediaManager questionMediaManager;
    private String binaryName;
    VideoWidgetBinding binding;

    public VideoWidget(Context context, QuestionDetails prompt, QuestionMediaManager questionMediaManager, WaitingForDataRegistry waitingForDataRegistry, Dependencies dependencies) {
        this(context, prompt, waitingForDataRegistry, questionMediaManager, dependencies);
    }

    public VideoWidget(Context context, QuestionDetails questionDetails, WaitingForDataRegistry waitingForDataRegistry, QuestionMediaManager questionMediaManager, Dependencies dependencies) {
        super(context, dependencies, questionDetails);
        this.waitingForDataRegistry = waitingForDataRegistry;
        this.questionMediaManager = questionMediaManager;
        binaryName = questionDetails.getPrompt().getAnswerText();
        render();
    }

    @Override
    protected View onCreateAnswerView(@NonNull Context context, @NonNull FormEntryPrompt prompt, int answerFontSize) {
        binding = VideoWidgetBinding.inflate(((Activity) context).getLayoutInflater());

        binding.recordVideoButton.setOnClickListener(v -> getPermissionsProvider().requestCameraPermission((Activity) getContext(), this::captureVideo));
        binding.chooseVideoButton.setOnClickListener(v -> chooseVideo());
        binding.playVideoButton.setEnabled(binaryName != null);
        binding.playVideoButton.setOnClickListener(v -> playVideoFile());

        if (questionDetails.isReadOnly()) {
            binding.recordVideoButton.setVisibility(View.GONE);
            binding.chooseVideoButton.setVisibility(View.GONE);
        }

        if (getFormEntryPrompt().getAppearanceHint() != null
                && getFormEntryPrompt().getAppearanceHint().toLowerCase(Locale.ENGLISH).contains(Appearances.NEW)) {
            binding.chooseVideoButton.setVisibility(View.GONE);
        }

        return binding.getRoot();
    }

    @Override
    public void deleteFile() {
        questionMediaManager.deleteAnswerFile(getFormEntryPrompt().getIndex().toString(),
        questionMediaManager.getAnswerFile(binaryName).getAbsolutePath());
        binaryName = null;
    }

    @Override
    public void clearAnswer() {
        // remove the file
        deleteFile();
        binding.playVideoButton.setEnabled(false);
        widgetValueChanged();
    }

    @Override
    public IAnswerData getAnswer() {
        if (binaryName != null) {
            return new StringData(binaryName);
        } else {
            return null;
        }
    }

    @Override
    public void setData(Object object) {
        if (binaryName != null) {
            deleteFile();
        }

        if (object instanceof File) {
            File newVideo = (File) object;
            if (newVideo.exists()) {
                questionMediaManager.replaceAnswerFile(getFormEntryPrompt().getIndex().toString(), newVideo.getAbsolutePath());
                binaryName = newVideo.getName();
                widgetValueChanged();
                binding.playVideoButton.setEnabled(binaryName != null);
            } else {
                Timber.e(new Error("Inserting Video file FAILED"));
            }
        } else {
            Timber.e(new Error("VideoWidget's setBinaryData must receive a File or Uri object."));
        }
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        binding.recordVideoButton.setOnLongClickListener(l);
        binding.chooseVideoButton.setOnLongClickListener(l);
        binding.playVideoButton.setOnLongClickListener(l);
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        binding.recordVideoButton.cancelLongPress();
        binding.chooseVideoButton.cancelLongPress();
        binding.playVideoButton.cancelLongPress();
    }

    private void captureVideo() {
        Intent i = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        int requestCode = RequestCodes.VIDEO_CAPTURE;

        // request high resolution if configured for that...
        boolean highResolution = settingsProvider.getUnprotectedSettings().getBoolean(ProjectKeys.KEY_HIGH_RESOLUTION);
        if (highResolution) {
            i.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
        }

        try {
            waitingForDataRegistry.waitForData(getFormEntryPrompt().getIndex());
            ((Activity) getContext()).startActivityForResult(i, requestCode);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(
                    getContext(),
                    getContext().getString(org.odk.collect.strings.R.string.activity_not_found,
                            getContext().getString(org.odk.collect.strings.R.string.capture_video)), Toast.LENGTH_SHORT)
                    .show();
            waitingForDataRegistry.cancelWaitingForData();
        }
    }

    private void chooseVideo() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("video/*");
        try {
            waitingForDataRegistry.waitForData(getFormEntryPrompt().getIndex());
            ((Activity) getContext()).startActivityForResult(i,
                    RequestCodes.VIDEO_CHOOSER);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(
                    getContext(),
                    getContext().getString(org.odk.collect.strings.R.string.activity_not_found,
                            getContext().getString(org.odk.collect.strings.R.string.choose_video)), Toast.LENGTH_SHORT)
                    .show();

            waitingForDataRegistry.cancelWaitingForData();
        }
    }

    private void playVideoFile() {
        File file = questionMediaManager.getAnswerFile(binaryName);
        mediaUtils.openFile(getContext(), file, "video/*");
    }
}
