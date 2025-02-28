package org.odk.collect.android.fragments.dialogs;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.javarosa.core.model.SelectChoice;
import org.javarosa.form.api.FormEntryPrompt;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.odk.collect.android.R;
import org.odk.collect.android.support.CollectHelpers;
import org.odk.collect.android.support.MockFormEntryPromptBuilder;
import org.odk.collect.android.support.WidgetTestActivity;
import org.odk.collect.android.utilities.MediaUtils;
import org.odk.collect.audioclips.AudioClipViewModel;
import org.odk.collect.testshared.RobolectricHelpers;

import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;

@RunWith(AndroidJUnit4.class)
public class SelectMinimalDialogTest {

    protected FragmentManager fragmentManager;
    protected SelectMinimalDialog dialogFragment;
    protected FormEntryPrompt formEntryPrompt;

    @Before
    public void setup() {
        FragmentActivity activity = CollectHelpers.createThemedActivity(WidgetTestActivity.class);

        // Set up AudioClipViewModel used by the dialog
        new ViewModelProvider(activity, new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                if (modelClass == AudioClipViewModel.class) {
                    return (T) mock(AudioClipViewModel.class);
                } else {
                    return ViewModelProvider.Factory.super.create(modelClass);
                }
            }
        }).get(AudioClipViewModel.class);

        fragmentManager = activity.getSupportFragmentManager();
    }

    @Test
    public void whenClickBackButton_shouldDialogBeClosed() {
        List<SelectChoice> items = getTestChoices();
        setUpFormEntryPrompt(items, "autocomplete");
        dialogFragment = new SelectOneMinimalDialog(null, false, true, ApplicationProvider.getApplicationContext(), items, formEntryPrompt, null, 0, 1, false, mock(MediaUtils.class));
        SelectMinimalDialog.SelectMinimalDialogListener listener = mock(SelectMinimalDialog.SelectMinimalDialogListener.class);
        dialogFragment.setListener(listener);

        dialogFragment.show(fragmentManager, "TAG");
        RobolectricHelpers.runLooper();

        assertThat(isDialogVisible(), is(true));
        dialogFragment.onBackPressed();
        assertThat(isDialogVisible(), is(false));
    }

    @Test
    public void whenClickBackArrowButton_shouldDialogBeClosed() {
        List<SelectChoice> items = getTestChoices();
        setUpFormEntryPrompt(items, "autocomplete");
        dialogFragment = new SelectOneMinimalDialog(null, false, true, ApplicationProvider.getApplicationContext(), items, formEntryPrompt, null, 0, 1, false, mock(MediaUtils.class));
        SelectMinimalDialog.SelectMinimalDialogListener listener = mock(SelectMinimalDialog.SelectMinimalDialogListener.class);
        dialogFragment.setListener(listener);

        dialogFragment.show(fragmentManager, "TAG");
        RobolectricHelpers.runLooper();

        assertThat(isDialogVisible(), is(true));
        dialogFragment.getToolbar().getChildAt(0).performClick();
        assertThat(isDialogVisible(), is(false));
    }

    @Test
    public void whenAutoCompleteAppearanceUsed_shouldSearchBarBeVisible() {
        List<SelectChoice> items = getTestChoices();
        setUpFormEntryPrompt(items, "autocomplete");
        dialogFragment = new SelectOneMinimalDialog(null, false, true, ApplicationProvider.getApplicationContext(), items, formEntryPrompt, null, 0, 1, false, mock(MediaUtils.class));
        SelectMinimalDialog.SelectMinimalDialogListener listener = mock(SelectMinimalDialog.SelectMinimalDialogListener.class);
        dialogFragment.setListener(listener);

        dialogFragment.show(fragmentManager, "TAG");
        RobolectricHelpers.runLooper();

        assertThat(dialogFragment.getToolbar().findViewById(R.id.menu_filter).getVisibility(), equalTo(View.VISIBLE));
    }

    protected boolean isDialogVisible() {
        return dialogFragment != null
                && dialogFragment.getDialog() != null
                && dialogFragment.getDialog().isShowing()
                && !dialogFragment.isRemoving();
    }

    protected List<SelectChoice> getTestChoices() {
        return asList(
                new SelectChoice("AAA", "AAA"),
                new SelectChoice("BBB", "BBB")
        );
    }

    protected void setUpFormEntryPrompt(List<SelectChoice> items, String appearance) {
        formEntryPrompt = new MockFormEntryPromptBuilder()
                .withSelectChoices(items)
                .withAppearance(appearance)
                .build();
    }
}
