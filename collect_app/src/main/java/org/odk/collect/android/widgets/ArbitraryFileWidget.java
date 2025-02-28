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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;

import android.util.TypedValue;
import android.view.View;

import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.databinding.ArbitraryFileWidgetAnswerBinding;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.utilities.ApplicationConstants;
import org.odk.collect.android.utilities.QuestionMediaManager;
import org.odk.collect.android.widgets.interfaces.WidgetDataReceiver;
import org.odk.collect.android.widgets.interfaces.FileWidget;
import org.odk.collect.android.widgets.utilities.WaitingForDataRegistry;

@SuppressLint("ViewConstructor")
public class ArbitraryFileWidget extends BaseArbitraryFileWidget implements FileWidget, WidgetDataReceiver {
    ArbitraryFileWidgetAnswerBinding binding;

    private final WaitingForDataRegistry waitingForDataRegistry;

    ArbitraryFileWidget(Context context, QuestionDetails questionDetails,
                        QuestionMediaManager questionMediaManager, WaitingForDataRegistry waitingForDataRegistry, Dependencies dependencies) {
        super(context, questionDetails, questionMediaManager, waitingForDataRegistry, dependencies);
        this.waitingForDataRegistry = waitingForDataRegistry;
    }

    @Override
    protected View onCreateAnswerView(Context context, FormEntryPrompt prompt, int answerFontSize) {
        binding = ArbitraryFileWidgetAnswerBinding.inflate(((Activity) context).getLayoutInflater());
        setupAnswerFile(prompt.getAnswerText());

        binding.arbitraryFileAnswerText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, answerFontSize);

        binding.arbitraryFileButton.setVisibility(questionDetails.isReadOnly() ? GONE : VISIBLE);
        binding.arbitraryFileButton.setOnClickListener(v -> onButtonClick());
        binding.arbitraryFileAnswerText.setOnClickListener(v -> mediaUtils.openFile(getContext(), answerFile, null));

        if (answerFile != null) {
            binding.arbitraryFileAnswerText.setText(answerFile.getName());
            binding.arbitraryFileAnswerText.setVisibility(VISIBLE);
        }

        return binding.getRoot();
    }

    @Override
    public void clearAnswer() {
        binding.arbitraryFileAnswerText.setVisibility(GONE);
        deleteFile();
        widgetValueChanged();
    }

    private void onButtonClick() {
        waitingForDataRegistry.waitForData(getFormEntryPrompt().getIndex());
        mediaUtils.pickFile((Activity) getContext(), "*/*", ApplicationConstants.RequestCodes.ARBITRARY_FILE_CHOOSER);
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener listener) {
        binding.arbitraryFileButton.setOnLongClickListener(listener);
        binding.arbitraryFileAnswerText.setOnLongClickListener(listener);
    }

    @Override
    protected void showAnswerText() {
        binding.arbitraryFileAnswerText.setText(answerFile.getName());
        binding.arbitraryFileAnswerText.setVisibility(VISIBLE);
    }

    @Override
    protected void hideAnswerText() {
        binding.arbitraryFileAnswerText.setVisibility(GONE);
    }
}
