/*
 * Copyright 2018 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.odk.collect.openrosa.http;

import androidx.annotation.NonNull;

import java.io.InputStream;
import java.util.Map;

import timber.log.Timber;

public class HttpGetResult {

    private static final String OPEN_ROSA_VERSION_HEADER = OpenRosaConstants.VERSION_HEADER;
    private static final String OPEN_ROSA_VERSION = "1.0";

    private final InputStream inputStream;
    private final Map<String, String> headers;
    private final String hash;
    private final int statusCode;

    public HttpGetResult(InputStream is, @NonNull Map<String, String> headers, String hash, int statusCode) {
        inputStream = is;
        this.headers = headers;
        this.hash = hash;
        this.statusCode = statusCode;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public String getHash() {
        return hash;
    }

    public boolean isOpenRosaResponse() {
        boolean openRosaResponse = false;

        if (!headers.isEmpty()) {
            boolean versionMatch = false;
            boolean first = true;

            StringBuilder appendedVersions = new StringBuilder();

            for (String key : headers.keySet()) {
                if (key.equalsIgnoreCase(OPEN_ROSA_VERSION_HEADER)) {
                    openRosaResponse = true;
                    if (OPEN_ROSA_VERSION.equals(headers.get(key))) {
                        versionMatch = true;
                        break;
                    }
                    if (!first) {
                        appendedVersions.append("; ");
                    }
                    first = false;
                    appendedVersions.append(headers.get(key));
                }
            }
            if (!versionMatch) {
                Timber.w("%s unrecognized version(s): %s", OPEN_ROSA_VERSION_HEADER, appendedVersions.toString());
            }
        }

        return openRosaResponse;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
