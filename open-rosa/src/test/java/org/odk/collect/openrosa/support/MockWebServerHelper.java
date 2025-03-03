package org.odk.collect.openrosa.support;

import okhttp3.Request;
import okhttp3.mockwebserver.MockWebServer;

public final class MockWebServerHelper {

    private MockWebServerHelper() {

    }

    public static Request buildRequest(MockWebServer mockWebServer, String path) {
        return new Request.Builder().url(mockWebServer.url(path)).build();
    }
}
