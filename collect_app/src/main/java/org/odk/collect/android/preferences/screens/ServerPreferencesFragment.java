/*
 * Copyright 2017 Shobhit
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

package org.odk.collect.android.preferences.screens;

import android.content.Context;
import android.os.Bundle;
import android.text.InputFilter;

import androidx.preference.EditTextPreference;
import androidx.preference.Preference;

import org.jetbrains.annotations.NotNull;
import org.odk.collect.android.R;
import org.odk.collect.android.backgroundwork.FormUpdateScheduler;
import org.odk.collect.android.injection.DaggerUtils;
import org.odk.collect.android.preferences.ServerPreferencesAdder;
import org.odk.collect.android.preferences.filters.ControlCharacterFilter;
import org.odk.collect.androidshared.ui.ToastUtils;
import org.odk.collect.androidshared.utils.Validator;
import org.odk.collect.settings.keys.ProjectKeys;

import javax.inject.Inject;

public class ServerPreferencesFragment extends BaseProjectPreferencesFragment {
    private EditTextPreference passwordPreference;

    @Inject
    FormUpdateScheduler formUpdateScheduler;

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        DaggerUtils.getComponent(context).inject(this);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        setPreferencesFromResource(R.xml.server_preferences, rootKey);
        addServerPreferences();
    }

    public void addServerPreferences() {
        if (!new ServerPreferencesAdder(this).add()) {
            return;
        }
        EditTextPreference serverUrlPreference = findPreference(ProjectKeys.KEY_SERVER_URL);
        EditTextPreference usernamePreference = findPreference(ProjectKeys.KEY_USERNAME);
        passwordPreference = findPreference(ProjectKeys.KEY_PASSWORD);

        serverUrlPreference.setOnPreferenceChangeListener(createChangeListener());
        serverUrlPreference.setSummary(serverUrlPreference.getText());

        usernamePreference.setOnPreferenceChangeListener(createChangeListener());
        usernamePreference.setSummary(usernamePreference.getText());

        usernamePreference.setOnBindEditTextListener(editText -> {
            editText.setFilters(new InputFilter[]{new ControlCharacterFilter()});
        });

        passwordPreference.setOnPreferenceChangeListener(createChangeListener());
        maskPasswordSummary(passwordPreference.getText());

        passwordPreference.setOnBindEditTextListener(editText -> {
            editText.setFilters(new InputFilter[]{new ControlCharacterFilter()});
        });
    }

    private Preference.OnPreferenceChangeListener createChangeListener() {
        return (preference, newValue) -> {
            switch (preference.getKey()) {
                case ProjectKeys.KEY_SERVER_URL:
                    String url = newValue.toString();

                    if (Validator.isUrlValid(url)) {
                        preference.setSummary(newValue.toString());
                    } else {
                        ToastUtils.showShortToast(org.odk.collect.strings.R.string.url_error);
                        return false;
                    }
                    break;

                case ProjectKeys.KEY_USERNAME:
                    String username = newValue.toString();

                    // do not allow leading and trailing whitespace
                    if (!username.equals(username.trim())) {
                        ToastUtils.showShortToast(org.odk.collect.strings.R.string.username_error_whitespace);
                        return false;
                    }

                    preference.setSummary(username);
                    return true;

                case ProjectKeys.KEY_PASSWORD:
                    String pw = newValue.toString();

                    // do not allow leading and trailing whitespace
                    if (!pw.equals(pw.trim())) {
                        ToastUtils.showShortToast(org.odk.collect.strings.R.string.password_error_whitespace);
                        return false;
                    }

                    maskPasswordSummary(pw);
                    break;
            }
            return true;
        };
    }

    private void maskPasswordSummary(String password) {
        passwordPreference.setSummary(password != null && password.length() > 0
                ? "********"
                : "");
    }
}
