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
package org.odk.collect.android.preferences.screens

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.preference.Preference
import org.odk.collect.android.R
import org.odk.collect.android.injection.DaggerUtils
import org.odk.collect.android.preferences.ProjectPreferencesViewModel
import org.odk.collect.android.preferences.dialogs.AdminPasswordDialogFragment
import org.odk.collect.android.preferences.dialogs.ChangeAdminPasswordDialog
import org.odk.collect.android.preferences.keys.AdminKeys
import org.odk.collect.android.utilities.DialogUtils
import org.odk.collect.android.utilities.MultiClickGuard
import org.odk.collect.android.version.VersionInformation
import org.odk.collect.androidshared.data.Consumable
import javax.inject.Inject

class ProjectPreferencesFragment :
    BaseProjectPreferencesFragment(),
    Preference.OnPreferenceClickListener {

    @Inject
    lateinit var versionInformation: VersionInformation

    override fun onAttach(context: Context) {
        super.onAttach(context)
        DaggerUtils.getComponent(context).inject(this)
        setHasOptionsMenu(true)

        projectPreferencesViewModel.state.observe(
            this,
            { state: Consumable<ProjectPreferencesViewModel.State> ->
                if (!state.isConsumed()) {
                    state.consume()
                    updatePreferencesVisibility()
                    requireActivity().invalidateOptionsMenu()
                }
            }
        )
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.project_preferences, rootKey)

        findPreference<Preference>(PROTOCOL_PREFERENCE_KEY)!!.onPreferenceClickListener = this
        findPreference<Preference>(PROJECT_DISPLAY_PREFERENCE_KEY)!!.onPreferenceClickListener = this
        findPreference<Preference>(USER_INTERFACE_PREFERENCE_KEY)!!.onPreferenceClickListener = this
        findPreference<Preference>(MAPS_PREFERENCE_KEY)!!.onPreferenceClickListener = this
        findPreference<Preference>(FORM_MANAGEMENT_PREFERENCE_KEY)!!.onPreferenceClickListener = this
        findPreference<Preference>(USER_AND_DEVICE_IDENTITY_PREFERENCE_KEY)!!.onPreferenceClickListener = this
        findPreference<Preference>(EXPERIMENTAL_PREFERENCE_KEY)!!.onPreferenceClickListener = this
        findPreference<Preference>(UNLOCK_PROTECTED_SETTINGS_PREFERENCE_KEY)!!.onPreferenceClickListener = this
        findPreference<Preference>(AdminKeys.KEY_CHANGE_ADMIN_PASSWORD)!!.onPreferenceClickListener = this
        findPreference<Preference>(PROJECT_MANAGEMENT_PREFERENCE_KEY)!!.onPreferenceClickListener = this
        findPreference<Preference>(ACCESS_CONTROL_PREFERENCE_KEY)!!.onPreferenceClickListener = this
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updatePreferencesVisibility()
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        if (MultiClickGuard.allowClick(javaClass.name)) {
            when (preference.key) {
                PROTOCOL_PREFERENCE_KEY -> displayPreferences(ServerPreferencesFragment())
                PROJECT_DISPLAY_PREFERENCE_KEY -> displayPreferences(ProjectDisplayPreferencesFragment())
                USER_INTERFACE_PREFERENCE_KEY -> displayPreferences(UserInterfacePreferencesFragment())
                MAPS_PREFERENCE_KEY -> displayPreferences(MapsPreferencesFragment())
                FORM_MANAGEMENT_PREFERENCE_KEY -> displayPreferences(FormManagementPreferencesFragment())
                USER_AND_DEVICE_IDENTITY_PREFERENCE_KEY -> displayPreferences(IdentityPreferencesFragment())
                EXPERIMENTAL_PREFERENCE_KEY -> displayPreferences(ExperimentalPreferencesFragment())
                UNLOCK_PROTECTED_SETTINGS_PREFERENCE_KEY -> DialogUtils.showIfNotShowing(AdminPasswordDialogFragment::class.java, requireActivity().supportFragmentManager)
                AdminKeys.KEY_CHANGE_ADMIN_PASSWORD -> DialogUtils.showIfNotShowing(ChangeAdminPasswordDialog::class.java, requireActivity().supportFragmentManager)
                PROJECT_MANAGEMENT_PREFERENCE_KEY -> displayPreferences(ProjectManagementPreferencesFragment())
                ACCESS_CONTROL_PREFERENCE_KEY -> displayPreferences(AccessControlPreferencesFragment())
            }
            return true
        }
        return false
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        when {
            projectPreferencesViewModel.isStateLocked() -> {
                menu.findItem(R.id.menu_locked).isVisible = true
                menu.findItem(R.id.menu_unlocked).isVisible = false
            }
            projectPreferencesViewModel.isStateUnlocked() -> {
                menu.findItem(R.id.menu_locked).isVisible = false
                menu.findItem(R.id.menu_unlocked).isVisible = true
            }
            else -> {
                menu.findItem(R.id.menu_locked).isVisible = false
                menu.findItem(R.id.menu_unlocked).isVisible = false
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.project_preferences_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_locked) {
            DialogUtils.showIfNotShowing(AdminPasswordDialogFragment::class.java, requireActivity().supportFragmentManager)
            return true
        }
        return false
    }

    private fun updatePreferencesVisibility() {
        findPreference<Preference>(PROTOCOL_PREFERENCE_KEY)!!.isVisible = true
        findPreference<Preference>(PROJECT_DISPLAY_PREFERENCE_KEY)!!.isVisible = true
        findPreference<Preference>(USER_INTERFACE_PREFERENCE_KEY)!!.isVisible = true
        findPreference<Preference>(MAPS_PREFERENCE_KEY)!!.isVisible = true
        findPreference<Preference>(FORM_MANAGEMENT_PREFERENCE_KEY)!!.isVisible = true
        findPreference<Preference>(USER_AND_DEVICE_IDENTITY_PREFERENCE_KEY)!!.isVisible = true
        findPreference<Preference>(EXPERIMENTAL_PREFERENCE_KEY)!!.isVisible = true
        findPreference<Preference>(UNLOCK_PROTECTED_SETTINGS_PREFERENCE_KEY)!!.isVisible = true
        findPreference<Preference>(AdminKeys.KEY_CHANGE_ADMIN_PASSWORD)!!.isVisible = true
        findPreference<Preference>(PROJECT_MANAGEMENT_PREFERENCE_KEY)!!.isVisible = true
        findPreference<Preference>(ACCESS_CONTROL_PREFERENCE_KEY)!!.isVisible = true

        if (projectPreferencesViewModel.isStateLocked()) {
            findPreference<Preference>(AdminKeys.KEY_CHANGE_ADMIN_PASSWORD)!!.isVisible = false
            findPreference<Preference>(PROJECT_MANAGEMENT_PREFERENCE_KEY)!!.isVisible = false
            findPreference<Preference>(ACCESS_CONTROL_PREFERENCE_KEY)!!.isVisible = false
        } else {
            findPreference<Preference>(UNLOCK_PROTECTED_SETTINGS_PREFERENCE_KEY)!!.isVisible = false
        }

        if (!projectPreferencesViewModel.isStateUnlocked()) {
            if (!hasAtLeastOneSettingEnabled(
                    listOf(
                            AdminKeys.KEY_CHANGE_SERVER
                        )
                )
            ) {
                findPreference<Preference>(PROTOCOL_PREFERENCE_KEY)!!.isVisible = false
            }

            if (!hasAtLeastOneSettingEnabled(
                    listOf(
                            AdminKeys.KEY_CHANGE_PROJECT_DISPLAY
                        )
                )
            ) {
                findPreference<Preference>(PROJECT_DISPLAY_PREFERENCE_KEY)!!.isVisible = false
            }

            if (!hasAtLeastOneSettingEnabled(
                    listOf(
                            AdminKeys.KEY_APP_THEME,
                            AdminKeys.KEY_APP_LANGUAGE,
                            AdminKeys.KEY_CHANGE_FONT_SIZE,
                            AdminKeys.KEY_NAVIGATION,
                            AdminKeys.KEY_SHOW_SPLASH_SCREEN
                        )
                )
            ) {
                findPreference<Preference>(USER_INTERFACE_PREFERENCE_KEY)!!.isVisible = false
            }

            if (!hasAtLeastOneSettingEnabled(
                    listOf(
                            AdminKeys.KEY_MAPS
                        )
                )
            ) {
                findPreference<Preference>(MAPS_PREFERENCE_KEY)!!.isVisible = false
            }

            if (!hasAtLeastOneSettingEnabled(
                    listOf(
                            AdminKeys.KEY_FORM_UPDATE_MODE,
                            AdminKeys.KEY_PERIODIC_FORM_UPDATES_CHECK,
                            AdminKeys.KEY_AUTOMATIC_UPDATE,
                            AdminKeys.KEY_HIDE_OLD_FORM_VERSIONS,
                            AdminKeys.KEY_AUTOSEND,
                            AdminKeys.KEY_DELETE_AFTER_SEND,
                            AdminKeys.KEY_DEFAULT_TO_FINALIZED,
                            AdminKeys.KEY_CONSTRAINT_BEHAVIOR,
                            AdminKeys.KEY_HIGH_RESOLUTION,
                            AdminKeys.KEY_IMAGE_SIZE,
                            AdminKeys.KEY_GUIDANCE_HINT,
                            AdminKeys.KEY_EXTERNAL_APP_RECORDING,
                            AdminKeys.KEY_INSTANCE_FORM_SYNC
                        )
                )
            ) {
                findPreference<Preference>(FORM_MANAGEMENT_PREFERENCE_KEY)!!.isVisible = false
            }

            if (!hasAtLeastOneSettingEnabled(
                    listOf(
                            AdminKeys.KEY_CHANGE_FORM_METADATA,
                            AdminKeys.KEY_ANALYTICS
                        )
                )
            ) {
                findPreference<Preference>(USER_AND_DEVICE_IDENTITY_PREFERENCE_KEY)!!.isVisible = false
            }
        }

        if (versionInformation.isRelease) {
            findPreference<Preference>(EXPERIMENTAL_PREFERENCE_KEY)!!.isVisible = false
        }
    }

    private fun hasAtLeastOneSettingEnabled(keys: Collection<String>): Boolean {
        for (key in keys) {
            val value = settingsProvider.getAdminSettings().getBoolean(key)
            if (value) {
                return true
            }
        }
        return false
    }

    companion object {
        private const val PROTOCOL_PREFERENCE_KEY = "protocol"
        private const val PROJECT_DISPLAY_PREFERENCE_KEY = "project_display"
        private const val USER_INTERFACE_PREFERENCE_KEY = "user_interface"
        private const val MAPS_PREFERENCE_KEY = "maps"
        private const val FORM_MANAGEMENT_PREFERENCE_KEY = "form_management"
        private const val USER_AND_DEVICE_IDENTITY_PREFERENCE_KEY = "user_and_device_identity"
        private const val EXPERIMENTAL_PREFERENCE_KEY = "experimental"
        private const val UNLOCK_PROTECTED_SETTINGS_PREFERENCE_KEY = "unlock_protected_settings"
        private const val PROJECT_MANAGEMENT_PREFERENCE_KEY = "project_management"
        private const val ACCESS_CONTROL_PREFERENCE_KEY = "access_control"
    }
}
