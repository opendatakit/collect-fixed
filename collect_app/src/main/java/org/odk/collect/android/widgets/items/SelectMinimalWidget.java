package org.odk.collect.android.widgets.items;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import org.javarosa.core.model.SelectChoice;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.databinding.SelectMinimalWidgetAnswerBinding;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.widgets.QuestionWidget;
import org.odk.collect.android.widgets.interfaces.MultiChoiceWidget;
import org.odk.collect.android.widgets.interfaces.SelectChoiceLoader;
import org.odk.collect.android.widgets.interfaces.WidgetDataReceiver;
import org.odk.collect.android.widgets.utilities.QuestionFontSizeUtils;
import org.odk.collect.android.widgets.utilities.WaitingForDataRegistry;

import java.util.List;

public abstract class SelectMinimalWidget extends QuestionWidget implements WidgetDataReceiver, MultiChoiceWidget {

    final List<SelectChoice> items;

    SelectMinimalWidgetAnswerBinding binding;
    private final WaitingForDataRegistry waitingForDataRegistry;

    public SelectMinimalWidget(Context context, QuestionDetails prompt, WaitingForDataRegistry waitingForDataRegistry, SelectChoiceLoader selectChoiceLoader, Dependencies dependencies) {
        super(context, dependencies, prompt);
        this.waitingForDataRegistry = waitingForDataRegistry;
        items = ItemsWidgetUtils.loadItemsAndHandleErrors(this, questionDetails.getPrompt(), selectChoiceLoader);
    }

    @Override
    protected View onCreateAnswerView(Context context, FormEntryPrompt prompt, int answerFontSize) {
        binding = SelectMinimalWidgetAnswerBinding.inflate(((Activity) context).getLayoutInflater());
        binding.answer.setTextSize(QuestionFontSizeUtils.getQuestionFontSize());
        if (prompt.isReadOnly()) {
            binding.answer.setEnabled(false);
        } else {
            binding.answer.setOnClickListener(v -> {
                waitingForDataRegistry.waitForData(prompt.getIndex());
                showDialog();
            });
        }
        return binding.getRoot();
    }

    @Override
    public void clearAnswer() {
        binding.answer.setText(org.odk.collect.strings.R.string.select_answer);
        widgetValueChanged();
    }

    @Override
    public int getChoiceCount() {
        return items.size();
    }

    protected abstract void showDialog();
}
