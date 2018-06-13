package org.odk.collect.android.http;

import org.odk.collect.android.tasks.InstanceServerUploader;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = HttpInterfaceModule.class)
public interface HttpComponent {
    HttpInterface buildHttpInterface();

    void inject(InstanceServerUploader uploader);
}
