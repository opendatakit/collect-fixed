package org.odk.collect.android.activities

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.whenever
import org.odk.collect.android.R
import org.odk.collect.android.application.Collect
import org.odk.collect.android.injection.DaggerUtils
import org.odk.collect.android.injection.config.AppDependencyModule
import org.odk.collect.android.mainmenu.MainMenuActivity
import org.odk.collect.android.projects.ManualProjectCreatorDialog
import org.odk.collect.android.projects.QrCodeProjectCreatorDialog
import org.odk.collect.android.support.CollectHelpers
import org.odk.collect.android.version.VersionInformation
import org.odk.collect.androidtest.ActivityScenarioLauncherRule
import org.odk.collect.androidtest.RecordedIntentsRule
import org.odk.collect.material.MaterialProgressDialogFragment
import org.odk.collect.mobiledevicemanagement.MDMConfigObserver
import org.odk.collect.projects.ProjectCreator
import org.odk.collect.projects.ProjectsRepository
import org.odk.collect.settings.ODKAppSettingsImporter
import org.odk.collect.settings.SettingsProvider
import org.odk.collect.strings.localization.getLocalizedString
import org.odk.collect.testshared.RobolectricHelpers

@RunWith(AndroidJUnit4::class)
class FirstLaunchActivityTest {

    @get:Rule
    val launcherRule = ActivityScenarioLauncherRule()

    @get:Rule
    val activityRule = RecordedIntentsRule()

    @Before
    fun setup() {
        CollectHelpers.overrideAppDependencyModule(object : AppDependencyModule() {
            override fun providesMDMConfigObserver(
                settingsProvider: SettingsProvider,
                projectsRepository: ProjectsRepository,
                projectCreator: ProjectCreator,
                settingsImporter: ODKAppSettingsImporter,
                context: Context
            ): MDMConfigObserver {
                return mock()
            }
        })
    }

    @Test
    fun `The QrCodeProjectCreatorDialog should be displayed after clicking on the 'Configure with QR code' button`() {
        val scenario = launcherRule.launch(FirstLaunchActivity::class.java)
        scenario.onActivity {
            onView(withText(org.odk.collect.strings.R.string.configure_with_qr_code)).perform(click())
            assertThat(
                it.supportFragmentManager.findFragmentByTag(QrCodeProjectCreatorDialog::class.java.name),
                `is`(notNullValue())
            )
        }
    }

    @Test
    fun `The ManualProjectCreatorDialog should be displayed after clicking on the 'Configure manually' button`() {
        val scenario = launcherRule.launch(FirstLaunchActivity::class.java)
        scenario.onActivity {
            onView(withText(org.odk.collect.strings.R.string.configure_manually)).perform(click())
            assertThat(
                it.supportFragmentManager.findFragmentByTag(ManualProjectCreatorDialog::class.java.name),
                `is`(notNullValue())
            )
        }
    }

    @Test
    fun `The ODK logo should be displayed`() {
        val scenario = launcherRule.launch(FirstLaunchActivity::class.java)
        scenario.onActivity {
            onView(withId(R.id.logo)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun `The app name with its version should be displayed`() {
        val versionInformation = mock(VersionInformation::class.java)
        whenever(versionInformation.versionToDisplay).thenReturn("vfake")
        CollectHelpers.overrideAppDependencyModule(object : AppDependencyModule() {
            override fun providesVersionInformation(): VersionInformation {
                return versionInformation
            }

            override fun providesMDMConfigObserver(
                settingsProvider: SettingsProvider,
                projectsRepository: ProjectsRepository,
                projectCreator: ProjectCreator,
                settingsImporter: ODKAppSettingsImporter,
                context: Context
            ): MDMConfigObserver {
                return mock()
            }
        })

        val scenario = launcherRule.launch(FirstLaunchActivity::class.java)
        scenario.onActivity {
            verify(versionInformation).versionToDisplay
            onView(
                withText(
                    ApplicationProvider.getApplicationContext<Collect>().getLocalizedString(
                        org.odk.collect.strings.R.string.collect_app_name
                    ) + " vfake"
                )
            ).perform(scrollTo()).check(matches(isDisplayed()))
        }
    }

    @Test
    fun `Adding demo project displays a progress dialog`() {
        val scenario = launcherRule.launch(FirstLaunchActivity::class.java)
        scenario.onActivity {
            val dialogClass = MaterialProgressDialogFragment::class.java
            assertThat(RobolectricHelpers.getFragmentByClass(it.supportFragmentManager, dialogClass), nullValue())

            onView(
                withText(
                    containsString(
                        ApplicationProvider.getApplicationContext<Collect>().getLocalizedString(
                            org.odk.collect.strings.R.string.try_demo
                        )
                    )
                )
            ).perform(scrollTo()).perform(click())

            assertThat(RobolectricHelpers.getFragmentByClass(it.supportFragmentManager, dialogClass), notNullValue())
        }
    }

    @Test
    fun `Navigate to the main menu when a new project is detected`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val projectCreator = DaggerUtils.getComponent(context).projectCreator()

        val scenario = launcherRule.launch(FirstLaunchActivity::class.java)
        scenario.onActivity {
            projectCreator.createNewProject("{\"general\":{},\"admin\":{}}", true)
            assertThat(
                Intents.getIntents()[0],
                hasComponent(MainMenuActivity::class.java.name)
            )
        }
    }
}
