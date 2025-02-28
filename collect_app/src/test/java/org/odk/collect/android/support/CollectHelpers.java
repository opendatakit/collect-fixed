package org.odk.collect.android.support;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.Intent;

import androidx.core.util.Pair;
import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ApplicationProvider;

import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.reference.ReferenceManager;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.injection.DaggerUtils;
import org.odk.collect.android.injection.config.AppDependencyComponent;
import org.odk.collect.android.injection.config.AppDependencyModule;
import org.odk.collect.android.injection.config.DaggerAppDependencyComponent;
import org.odk.collect.projects.Project;
import org.odk.collect.testshared.RobolectricHelpers;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;

import java.util.List;

public final class CollectHelpers {

    private CollectHelpers() {

    }

    public static void overrideReferenceManager(ReferenceManager referenceManager) {
        overrideAppDependencyModule(new AppDependencyModule() {
            @Override
            public ReferenceManager providesReferenceManager() {
                return referenceManager;
            }
        });
    }

    public static ReferenceManager setupFakeReferenceManager(List<Pair<String, String>> references) throws InvalidReferenceException {
        ReferenceManager referenceManager = mock(ReferenceManager.class);

        for (Pair<String, String> reference : references) {
            createFakeReference(referenceManager, reference.first, reference.second);
        }

        return referenceManager;
    }

    private static String createFakeReference(ReferenceManager referenceManager, String referenceURI, String localURI) throws InvalidReferenceException {
        Reference reference = mock(Reference.class);
        when(reference.getLocalURI()).thenReturn(localURI);
        when(referenceManager.deriveReference(referenceURI)).thenReturn(reference);

        return localURI;
    }

    public static AppDependencyComponent overrideAppDependencyModule(AppDependencyModule appDependencyModule) {
        AppDependencyComponent testComponent = DaggerAppDependencyComponent.builder()
                .application(ApplicationProvider.getApplicationContext())
                .appDependencyModule(appDependencyModule)
                .build();
        ((Collect) ApplicationProvider.getApplicationContext()).setComponent(testComponent);
        return testComponent;
    }

    public static void resetProcess(AppDependencyModule dependencies) {
        Collect application = ApplicationProvider.getApplicationContext();

        application.getState().clear();

        AppDependencyComponent newComponent = CollectHelpers.overrideAppDependencyModule(dependencies);
        newComponent.applicationInitializer().initialize();
    }

    public static <T extends FragmentActivity> T createThemedActivity(Class<T> clazz) {
        return RobolectricHelpers.createThemedActivity(clazz);
    }

    public static FragmentActivity createThemedActivity() {
        return createThemedActivity(FragmentActivity.class);
    }

    public static <T extends FragmentActivity> ActivityController<T> buildThemedActivity(Class<T> clazz) {
        ActivityController<T> activity = Robolectric.buildActivity(clazz);
        activity.get().setTheme(com.google.android.material.R.style.Theme_MaterialComponents);

        return activity;
    }

    public static <T extends FragmentActivity> ActivityController<T> buildThemedActivity(Class<T> clazz, Intent intent) {
        ActivityController<T> activity = Robolectric.buildActivity(clazz, intent);
        activity.get().setTheme(com.google.android.material.R.style.Theme_MaterialComponents);

        return activity;
    }

    public static String setupDemoProject() {
        createDemoProject();
        DaggerUtils.getComponent(ApplicationProvider.<Application>getApplicationContext()).currentProjectProvider().setCurrentProject(Project.DEMO_PROJECT_ID);
        return Project.DEMO_PROJECT_ID;
    }

    public static String createDemoProject() {
        return createProject(Project.Companion.getDEMO_PROJECT());
    }

    public static String createProject(Project project) {
        Project.Saved savedProject = DaggerUtils.getComponent(ApplicationProvider.<Application>getApplicationContext()).projectsRepository().save(project);
        return savedProject.getUuid();
    }
}
