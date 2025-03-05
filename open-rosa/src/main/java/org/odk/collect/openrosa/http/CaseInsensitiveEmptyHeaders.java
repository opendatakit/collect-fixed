package org.odk.collect.openrosa.http;

import androidx.annotation.Nullable;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class CaseInsensitiveEmptyHeaders implements CaseInsensitiveHeaders {
    @Nullable
    @Override
    public Set<String> getHeaders() {
        return new TreeSet<>();
    }

    @Override
    public boolean containsHeader(String header) {
        return false;
    }

    @Nullable
    @Override
    public String getAnyValue(String header) {
        return null;
    }

    @Nullable
    @Override
    public List<String> getValues(String header) {
        return null;
    }
}
