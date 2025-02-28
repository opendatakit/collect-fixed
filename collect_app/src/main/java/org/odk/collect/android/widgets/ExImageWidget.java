package org.odk.collect.android.widgets;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.ImageView;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.databinding.ExImageWidgetAnswerBinding;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.utilities.ApplicationConstants;
import org.odk.collect.android.utilities.FileUtils;
import org.odk.collect.android.utilities.QuestionMediaManager;
import org.odk.collect.android.widgets.interfaces.FileWidget;
import org.odk.collect.android.widgets.interfaces.WidgetDataReceiver;
import org.odk.collect.android.widgets.utilities.FileRequester;
import org.odk.collect.android.widgets.utilities.WaitingForDataRegistry;
import org.odk.collect.androidshared.ui.ToastUtils;

import java.io.File;

import timber.log.Timber;

@SuppressLint("ViewConstructor")
public class ExImageWidget extends QuestionWidget implements FileWidget, WidgetDataReceiver {
    ExImageWidgetAnswerBinding binding;

    private final WaitingForDataRegistry waitingForDataRegistry;
    private final QuestionMediaManager questionMediaManager;
    private final FileRequester fileRequester;

    File answerFile;

    public ExImageWidget(Context context, QuestionDetails questionDetails, QuestionMediaManager questionMediaManager,
                         WaitingForDataRegistry waitingForDataRegistry, FileRequester fileRequester, Dependencies dependencies) {
        super(context, dependencies, questionDetails);

        this.waitingForDataRegistry = waitingForDataRegistry;
        this.questionMediaManager = questionMediaManager;
        this.fileRequester = fileRequester;

        render();
    }

    @Override
    protected View onCreateAnswerView(Context context, FormEntryPrompt prompt, int answerFontSize) {
        setupAnswerFile(prompt.getAnswerText());

        binding = ExImageWidgetAnswerBinding.inflate(((Activity) context).getLayoutInflater());

        binding.launchExternalAppButton.setVisibility(questionDetails.isReadOnly() ? GONE : VISIBLE);
        binding.launchExternalAppButton.setOnClickListener(view -> launchExternalApp());
        binding.imageView.setOnClickListener(view -> mediaUtils.openFile(getContext(), answerFile, "image/*"));
        if (answerFile != null) {
            displayImage();
        } else {
            binding.imageView.setVisibility(GONE);
        }

        return binding.getRoot();
    }

    @Override
    public void deleteFile() {
        questionMediaManager.deleteAnswerFile(getFormEntryPrompt().getIndex().toString(), answerFile.getAbsolutePath());
        answerFile = null;
    }

    @Override
    public void clearAnswer() {
        deleteFile();
        binding.imageView.setVisibility(GONE);
        widgetValueChanged();
    }

    @Override
    public IAnswerData getAnswer() {
        return answerFile != null ? new StringData(answerFile.getName()) : null;
    }

    @Override
    public void setData(Object object) {
        if (answerFile != null) {
            clearAnswer();
        }

        if (object instanceof File && mediaUtils.isImageFile((File) object)) {
            answerFile = (File) object;
            if (answerFile.exists()) {
                questionMediaManager.replaceAnswerFile(getFormEntryPrompt().getIndex().toString(), answerFile.getAbsolutePath());
                displayImage();
                widgetValueChanged();
            } else {
                Timber.e(new Error("Inserting Image file FAILED"));
            }
        } else if (object != null) {
            if (object instanceof File) {
                ToastUtils.showLongToast(org.odk.collect.strings.R.string.invalid_file_type);
                mediaUtils.deleteMediaFile(((File) object).getAbsolutePath());
                Timber.e(new Error("ExImageWidget's setBinaryData must receive an image file but received: " + FileUtils.getMimeType((File) object)));
            } else {
                Timber.e(new Error("ExImageWidget's setBinaryData must receive an image file but received: " + object.getClass()));
            }
        }
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        binding.launchExternalAppButton.setOnLongClickListener(l);
        binding.imageView.setOnLongClickListener(l);
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        binding.launchExternalAppButton.cancelLongPress();
        binding.imageView.cancelLongPress();
    }

    private void launchExternalApp() {
        waitingForDataRegistry.waitForData(getFormEntryPrompt().getIndex());
        fileRequester.launch((Activity) getContext(), ApplicationConstants.RequestCodes.EX_IMAGE_CHOOSER, getFormEntryPrompt());
    }

    private void setupAnswerFile(String fileName) {
        if (fileName != null && !fileName.isEmpty()) {
            answerFile = questionMediaManager.getAnswerFile(fileName);
        }
    }

    private void displayImage() {
        imageLoader.loadImage(binding.imageView, answerFile, ImageView.ScaleType.FIT_CENTER, null);
        binding.imageView.setVisibility(VISIBLE);
    }
}
