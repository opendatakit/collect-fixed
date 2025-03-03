package org.odk.collect.openrosa.http;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.odk.collect.openrosa.support.MockWebServerRule;

import java.net.URI;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public abstract class OpenRosaHeadRequestTest {

    protected static final String USER_AGENT = "Test Agent";

    protected abstract OpenRosaHttpInterface buildSubject();

    @Rule
    public MockWebServerRule mockWebServerRule = new MockWebServerRule();

    private MockWebServer mockWebServer;
    private OpenRosaHttpInterface subject;

    @Before
    public void setup() throws Exception {
        subject = buildSubject();
        mockWebServer = mockWebServerRule.start();
    }

    @Test
    public void makesAHeadRequestToUri() throws Exception {
        mockWebServer.enqueue(new MockResponse());

        URI uri = mockWebServer.url("/blah").uri();
        subject.executeHeadRequest(uri, null);

        assertThat(mockWebServer.getRequestCount(), equalTo(1));

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod(), equalTo("HEAD"));
        assertThat(request.getRequestUrl().uri(), equalTo(uri));
    }

    @Test
    public void sendsCollectHeaders() throws Exception {
        mockWebServer.enqueue(new MockResponse());

        subject.executeHeadRequest(mockWebServer.url("").uri(), null);

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getHeader("User-Agent"), equalTo(USER_AGENT));
    }

    @Test
    public void when204Response_returnsHeaders() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(204)
                .addHeader("X-1", "Blah1")
                .addHeader("X-2", "Blah2"));

        HttpHeadResult result = subject.executeHeadRequest(mockWebServer.url("").uri(), null);
        assertThat(result.getHeaders().getAnyValue("X-1"), equalTo("Blah1"));
        assertThat(result.getHeaders().getAnyValue("X-2"), equalTo("Blah2"));
    }

    // Ensure we can look up lower-case headers using mixed-case header names.
    // https://github.com/getodk/collect/issues/3068
    @Test
    public void when204Response_returnsLowerCaseHeaders() throws Exception {
        String headerLowerCase = "header-case-test";
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(204)
                .addHeader(headerLowerCase, "value"));

        String headerMixedCase = "Header-Case-Test";
        HttpHeadResult result = subject.executeHeadRequest(mockWebServer.url("").uri(), null);
        assertTrue(result.getHeaders().containsHeader(headerMixedCase));
        assertThat(result.getHeaders().getAnyValue(headerMixedCase), equalTo("value"));
    }

    @Test
    public void whenRequestFails_throwsExceptionWithMessage() {
        try {
            subject.executeHeadRequest(new URI("http://localhost:8443"), null);
            fail();
        } catch (Exception e) {
            assertThat(e.getMessage(), not(isEmptyString()));
        }
    }
}
