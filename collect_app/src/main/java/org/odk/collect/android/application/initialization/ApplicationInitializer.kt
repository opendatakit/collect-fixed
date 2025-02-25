package org.odk.collect.android.application.initialization

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.startup.AppInitializer
import net.danlew.android.joda.JodaTimeInitializer
import org.odk.collect.analytics.Analytics
import org.odk.collect.android.BuildConfig
import org.odk.collect.android.application.Collect
import org.odk.collect.android.application.initialization.upgrade.UpgradeInitializer
import org.odk.collect.android.entities.EntitiesRepositoryProvider
import org.odk.collect.android.projects.ProjectsDataService
import org.odk.collect.androidshared.ui.ToastUtils
import org.odk.collect.metadata.PropertyManager
import org.odk.collect.projects.ProjectsRepository
import org.odk.collect.settings.SettingsProvider
import timber.log.Timber
import java.util.Locale

class ApplicationInitializer(
    private val context: Application,
    private val propertyManager: PropertyManager,
    private val analytics: Analytics,
    private val upgradeInitializer: UpgradeInitializer,
    private val analyticsInitializer: AnalyticsInitializer,
    private val mapsInitializer: MapsInitializer,
    private val projectsRepository: ProjectsRepository,
    private val settingsProvider: SettingsProvider,
    private val entitiesRepositoryProvider: EntitiesRepositoryProvider,
    private val projectsDataService: ProjectsDataService
) {
    fun initialize() {
        initializeLocale()
        runInitializers()
        initializeFrameworks()
    }

    private fun runInitializers() {
        upgradeInitializer.initialize()
        analyticsInitializer.initialize()
        UserPropertiesInitializer(
            analytics,
            projectsRepository,
            settingsProvider,
            context
        ).initialize()
        mapsInitializer.initialize()
        JavaRosaInitializer(propertyManager, projectsDataService, entitiesRepositoryProvider, settingsProvider).initialize()
        SystemThemeMismatchFixInitializer(context).initialize()
    }

    private fun initializeFrameworks() {
        ToastUtils.setApplication(context)
        initializeLogging()
        AppInitializer.getInstance(context).initializeComponent(JodaTimeInitializer::class.java)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }

    private fun initializeLocale() {
        Collect.defaultSysLanguage = Locale.getDefault().language
    }

    private fun initializeLogging() {
        if (BuildConfig.BUILD_TYPE == "odkCollectRelease") {
            Timber.plant(CrashReportingTree(analytics))
        } else {
            Timber.plant(Timber.DebugTree())
        }
    }
}
