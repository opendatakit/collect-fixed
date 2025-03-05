package org.odk.collect.openrosa.support;

import androidx.annotation.NonNull;

import org.odk.collect.openrosa.forms.OpenRosaXmlFetcher;
import org.odk.collect.openrosa.http.HttpCredentials;
import org.odk.collect.openrosa.http.HttpCredentialsInterface;

import java.net.URI;

public class StubWebCredentialsProvider implements OpenRosaXmlFetcher.WebCredentialsProvider {

    @Override
    public HttpCredentialsInterface getCredentials(@NonNull URI url) {
        return new HttpCredentials(null, null);
    }
}
