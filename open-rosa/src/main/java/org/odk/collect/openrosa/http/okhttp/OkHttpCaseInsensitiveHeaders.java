package org.odk.collect.openrosa.http.okhttp;

import androidx.annotation.Nullable;

import org.odk.collect.openrosa.http.CaseInsensitiveHeaders;

import java.util.List;
import java.util.Set;

import okhttp3.Headers;

public class OkHttpCaseInsensitiveHeaders implements CaseInsensitiveHeaders {
    Headers headers;

    public OkHttpCaseInsensitiveHeaders(Headers headers) {
        this.headers = headers;
    }

    @Override
    public Set<String> getHeaders() {
        return headers.names();
    }

    @Override
    public boolean containsHeader(String header) {
        return header != null && headers.get(header) != null;
    }

    @Nullable
    @Override
    public String getAnyValue(String header) {
        return headers.get(header);
    }

    @Nullable
    @Override
    public List<String> getValues(String header) {
        return headers.values(header);
    }
}
