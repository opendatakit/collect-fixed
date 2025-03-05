package org.odk.collect.openrosa.http.okhttp;

import org.odk.collect.openrosa.http.OpenRosaHttpInterface;
import org.odk.collect.openrosa.http.OpenRosaPostRequestTest;

public class OkHttpConnectionPostRequestTest extends OpenRosaPostRequestTest {

    @Override
    protected OpenRosaHttpInterface buildSubject(OpenRosaHttpInterface.FileToContentTypeMapper mapper) {
        return new OkHttpConnection(
                null,
                mapper,
                "Test Agent"
        );
    }
}
