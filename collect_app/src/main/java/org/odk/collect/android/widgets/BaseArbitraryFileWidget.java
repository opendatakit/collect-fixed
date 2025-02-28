package org.odk.collect.android.widgets;

import android.content.Context;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.utilities.QuestionMediaManager;
import org.odk.collect.android.widgets.interfaces.FileWidget;
import org.odk.collect.android.widgets.interfaces.WidgetDataReceiver;
import org.odk.collect.android.widgets.utilities.WaitingForDataRegistry;

import java.io.File;

import timber.log.Timber;

public abstract class BaseArbitraryFileWidget extends QuestionWidget implements FileWidget, WidgetDataReceiver  {
    private final QuestionMediaManager questionMediaManager;
    protected final WaitingForDataRegistry waitingForDataRegistry;

    protected File answerFile;

    public BaseArbitraryFileWidget(Context context, QuestionDetails questionDetails, QuestionMediaManager questionMediaManager,
                                   WaitingForDataRegistry waitingForDataRegistry, Dependencies dependencies) {
        super(context, dependencies, questionDetails);
        this.questionMediaManager = questionMediaManager;
        this.waitingForDataRegistry = waitingForDataRegistry;
        render();
    }

    @Override
    public IAnswerData getAnswer() {
        return answerFile != null ? new StringData(answerFile.getName()) : null;
    }

    @Override
    public void deleteFile() {
        questionMediaManager.deleteAnswerFile(getFormEntryPrompt().getIndex().toString(), answerFile.getAbsolutePath());
        answerFile = null;
        hideAnswerText();
    }

    @Override
    public void setData(Object object) {
        if (answerFile != null) {
            deleteFile();
        }

        if (object instanceof File) {
            answerFile = (File) object;
            if (answerFile.exists()) {
                questionMediaManager.replaceAnswerFile(getFormEntryPrompt().getIndex().toString(), answerFile.getAbsolutePath());
                showAnswerText();
                widgetValueChanged();
            } else {
                answerFile = null;
                Timber.e(new Error("Inserting Arbitrary file FAILED"));
            }
        } else if (object != null) {
            Timber.e(new Error("FileWidget's setBinaryData must receive a File but received: " + object.getClass()));
        }
    }

    protected void setupAnswerFile(String fileName) {
        if (fileName != null && !fileName.isEmpty()) {
            answerFile = questionMediaManager.getAnswerFile(fileName);
        }
    }

    protected abstract void showAnswerText();

    protected abstract void hideAnswerText();
}
