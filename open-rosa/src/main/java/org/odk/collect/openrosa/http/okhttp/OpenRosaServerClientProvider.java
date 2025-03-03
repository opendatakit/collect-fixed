package org.odk.collect.openrosa.http.okhttp;

import androidx.annotation.NonNull;

import org.odk.collect.openrosa.http.HttpCredentialsInterface;

public interface OpenRosaServerClientProvider {

    OpenRosaServerClient get(String schema, String userAgent, @NonNull HttpCredentialsInterface credentialsInterface);
}
