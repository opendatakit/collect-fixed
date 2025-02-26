package org.odk.collect.android.activities

import android.os.Bundle
import android.text.SpannableStringBuilder
import androidx.activity.viewModels
import androidx.core.text.color
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.odk.collect.analytics.Analytics
import org.odk.collect.android.analytics.AnalyticsEvents
import org.odk.collect.android.databinding.FirstLaunchLayoutBinding
import org.odk.collect.android.injection.DaggerUtils
import org.odk.collect.android.mainmenu.MainMenuActivity
import org.odk.collect.android.projects.ManualProjectCreatorDialog
import org.odk.collect.android.projects.ProjectsDataService
import org.odk.collect.android.projects.QrCodeProjectCreatorDialog
import org.odk.collect.android.version.VersionInformation
import org.odk.collect.androidshared.system.ContextUtils.getThemeAttributeValue
import org.odk.collect.androidshared.ui.DialogFragmentUtils
import org.odk.collect.async.Scheduler
import org.odk.collect.material.MaterialProgressDialogFragment
import org.odk.collect.mobiledevicemanagement.MDMConfigObserver
import org.odk.collect.projects.Project
import org.odk.collect.projects.ProjectsRepository
import org.odk.collect.settings.SettingsProvider
import org.odk.collect.strings.localization.LocalizedActivity
import javax.inject.Inject

class FirstLaunchActivity : LocalizedActivity() {

    @Inject
    lateinit var projectsRepository: ProjectsRepository

    @Inject
    lateinit var versionInformation: VersionInformation

    @Inject
    lateinit var projectsDataService: ProjectsDataService

    @Inject
    lateinit var settingsProvider: SettingsProvider

    @Inject
    lateinit var scheduler: Scheduler

    @Inject
    lateinit var mdmConfigObserver: MDMConfigObserver

    private val viewModel: FirstLaunchViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return FirstLaunchViewModel(scheduler, projectsRepository, projectsDataService) as T
            }
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DaggerUtils.getComponent(this).inject(this)

        FirstLaunchLayoutBinding.inflate(layoutInflater).apply {
            setContentView(this.root)

            MaterialProgressDialogFragment.showOn(
                this@FirstLaunchActivity,
                viewModel.isLoading,
                supportFragmentManager
            ) {
                MaterialProgressDialogFragment().also { dialog ->
                    dialog.message = getString(org.odk.collect.strings.R.string.loading)
                }
            }

            lifecycleScope.launch {
                viewModel.currentProject
                    .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                    .collect { currentProject ->
                        if (currentProject != null) {
                            ActivityUtils.startActivityAndCloseAllOthers(
                                this@FirstLaunchActivity,
                                MainMenuActivity::class.java
                            )
                        }
                    }
            }

            viewModel.isLoading.observe(this@FirstLaunchActivity) { isLoading ->
                if (!isLoading) {
                    ActivityUtils.startActivityAndCloseAllOthers(
                        this@FirstLaunchActivity,
                        MainMenuActivity::class.java
                    )
                }
            }

            configureViaQrButton.setOnClickListener {
                DialogFragmentUtils.showIfNotShowing(
                    QrCodeProjectCreatorDialog::class.java,
                    supportFragmentManager
                )
            }

            configureManuallyButton.setOnClickListener {
                DialogFragmentUtils.showIfNotShowing(
                    ManualProjectCreatorDialog::class.java,
                    supportFragmentManager
                )
            }

            appName.text = String.format(
                "%s %s",
                getString(org.odk.collect.strings.R.string.collect_app_name),
                versionInformation.versionToDisplay
            )

            dontHaveServer.apply {
                text = SpannableStringBuilder()
                    .append(getString(org.odk.collect.strings.R.string.dont_have_project))
                    .append(" ")
                    .color(getThemeAttributeValue(context, com.google.android.material.R.attr.colorAccent)) {
                        append(getString(org.odk.collect.strings.R.string.try_demo))
                    }

                setOnClickListener {
                    viewModel.tryDemo()
                }
            }
        }
        lifecycle.addObserver(mdmConfigObserver)
    }
}

private class FirstLaunchViewModel(
    private val scheduler: Scheduler,
    private val projectsRepository: ProjectsRepository,
    private val projectsDataService: ProjectsDataService
) : ViewModel() {
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    val currentProject = projectsDataService.getCurrentProject()

    fun tryDemo() {
        Analytics.log(AnalyticsEvents.TRY_DEMO)

        _isLoading.value = true
        scheduler.immediate(
            background = {
                projectsRepository.save(Project.DEMO_PROJECT)
                projectsDataService.setCurrentProject(Project.DEMO_PROJECT_ID)
            },
            foreground = {
                _isLoading.value = false
            }
        )
    }
}
