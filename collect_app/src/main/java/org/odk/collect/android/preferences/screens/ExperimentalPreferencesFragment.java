package org.odk.collect.android.preferences.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import org.jetbrains.annotations.NotNull;
import org.odk.collect.android.R;
import org.odk.collect.androidshared.ui.ToastUtils;
import org.odk.collect.entities.browser.EntityBrowserActivity;

public class ExperimentalPreferencesFragment extends BaseProjectPreferencesFragment {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        setPreferencesFromResource(R.xml.experimental_preferences, rootKey);

        findPreference("entities").setOnPreferenceClickListener(preference -> {
            FragmentActivity activity = requireActivity();
            activity.startActivity(new Intent(activity, EntityBrowserActivity.class));
            return true;
        });

        findPreference("dev_tools").setOnPreferenceClickListener(preference -> {
            displayPreferences(new DevToolsPreferencesFragment());
            return true;
        });
    }

    @Override
    public void onViewCreated(@NotNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getPreferenceScreen().getPreferenceCount() == 0) {
            ToastUtils.showLongToast("No experimental settings at the moment!");
            getParentFragmentManager().popBackStack();
        }
    }
}
