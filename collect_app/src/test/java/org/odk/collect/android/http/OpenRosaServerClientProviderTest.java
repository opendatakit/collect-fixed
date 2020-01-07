package org.odk.collect.android.http;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.odk.collect.android.http.openrosa.HttpCredentials;
import org.odk.collect.android.http.openrosa.OpenRosaConstants;
import org.odk.collect.android.http.openrosa.OpenRosaServerClient;
import org.odk.collect.android.http.openrosa.OpenRosaServerClientProvider;
import org.odk.collect.android.http.support.MockWebServerRule;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.odk.collect.android.http.support.MockWebServerHelper.buildRequest;

public abstract class OpenRosaServerClientProviderTest {

    protected abstract OpenRosaServerClientProvider buildSubject();

    private OpenRosaServerClientProvider subject;

    @Rule
    public MockWebServerRule mockWebServerRule = new MockWebServerRule();

    @Before
    public void setup() {
        subject = buildSubject();
    }

    @Test
    public void sendsOpenRosaHeaders() throws Exception {
        MockWebServer mockWebServer = mockWebServerRule.start();
        enqueueSuccess(mockWebServer);

        OpenRosaServerClient client = subject.get("http", "Android", null,"localhost");
        client.makeRequest(buildRequest(mockWebServer, ""), new Date());

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getHeader(OpenRosaConstants.VERSION_HEADER), equalTo("1.0"));
    }

    @Test
    public void sendsDateHeader() throws Exception {
        MockWebServer mockWebServer = mockWebServerRule.start();
        enqueueSuccess(mockWebServer);

        Date currentTime = new Date();

        OpenRosaServerClient client = subject.get("http", "Android", null,"localhost");
        client.makeRequest(buildRequest(mockWebServer, ""), currentTime);

        RecordedRequest request = mockWebServer.takeRequest();

        SimpleDateFormat dateFormatGmt = new SimpleDateFormat("E, dd MMM yyyy hh:mm:ss zz", Locale.US);
        dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
        assertThat(request.getHeader("Date"), equalTo(dateFormatGmt.format(currentTime)));
    }

    @Test
    public void sendsAcceptsGzipHeader() throws Exception {
        MockWebServer mockWebServer = mockWebServerRule.start();
        enqueueSuccess(mockWebServer);

        OpenRosaServerClient client = subject.get("http", "Android", null,"localhost");
        client.makeRequest(buildRequest(mockWebServer, ""), new Date());

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getHeader("Accept-Encoding"), equalTo("gzip"));
    }

    @Test
    public void withCredentials_whenBasicChallengeReceived_doesNotRetryWithCredentials() throws Exception {
        MockWebServer mockWebServer = mockWebServerRule.start();
        enqueueBasicChallenge(mockWebServer);
        enqueueSuccess(mockWebServer);

        OpenRosaServerClient client = subject.get("http", "Android", new HttpCredentials("user", "pass"), "localhost");
        client.makeRequest(buildRequest(mockWebServer, ""), new Date());

        assertThat(mockWebServer.getRequestCount(), equalTo(1));
    }

    @Test
    public void withCredentials_whenBasicChallengeReceived_whenHttps_retriesWithCredentials() throws Exception {
        MockWebServer httpsMockWebServer = mockWebServerRule.start();

        enqueueBasicChallenge(httpsMockWebServer);
        enqueueSuccess(httpsMockWebServer);

        OpenRosaServerClient client = subject.get("https", "Android", new HttpCredentials("user", "pass"), "localhost");
        client.makeRequest(buildRequest(httpsMockWebServer, ""), new Date());

        assertThat(httpsMockWebServer.getRequestCount(), equalTo(2));
        httpsMockWebServer.takeRequest();
        RecordedRequest request = httpsMockWebServer.takeRequest();
        assertThat(request.getHeader("Authorization"), equalTo("Basic dXNlcjpwYXNz"));
    }

    @Test
    public void withCredentials_whenDigestChallengeReceived_retriesWithCredentials() throws Exception {
        MockWebServer mockWebServer = mockWebServerRule.start();
        enqueueDigestChallenge(mockWebServer);
        enqueueSuccess(mockWebServer);

        OpenRosaServerClient client = subject.get("http", "Android", new HttpCredentials("user", "pass"),"localhost");
        client.makeRequest(buildRequest(mockWebServer, ""), new Date());

        assertThat(mockWebServer.getRequestCount(), equalTo(2));
        mockWebServer.takeRequest();
        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getHeader("Authorization"), startsWith("Digest"));
    }

    @Test
    public void withCredentials_whenDigestChallengeReceived_whenHttps_retriesWithCredentials() throws Exception {
        MockWebServer httpsMockWebServer = mockWebServerRule.startHTTPS();

        enqueueDigestChallenge(httpsMockWebServer);
        enqueueSuccess(httpsMockWebServer);

        OpenRosaServerClient client = subject.get("https", "Android", new HttpCredentials("user", "pass"),"localhost");
        client.makeRequest(buildRequest(httpsMockWebServer, ""), new Date());

        assertThat(httpsMockWebServer.getRequestCount(), equalTo(2));
        httpsMockWebServer.takeRequest();
        RecordedRequest request = httpsMockWebServer.takeRequest();
        assertThat(request.getHeader("Authorization"), startsWith("Digest"));
    }

    @Test
    public void withCredentials_onceBasicChallenged_whenHttps_proactivelySendsCredentials() throws Exception {
        MockWebServer httpsMockWebServer = mockWebServerRule.startHTTPS();

        enqueueBasicChallenge(httpsMockWebServer);
        enqueueSuccess(httpsMockWebServer);
        enqueueSuccess(httpsMockWebServer);

        OpenRosaServerClient client = subject.get("https", "Android", new HttpCredentials("user", "pass"),"localhost");
        client.makeRequest(buildRequest(httpsMockWebServer, ""), new Date());
        client.makeRequest(buildRequest(httpsMockWebServer, "/different"), new Date());

        assertThat(httpsMockWebServer.getRequestCount(), equalTo(3));
        httpsMockWebServer.takeRequest();
        httpsMockWebServer.takeRequest();
        RecordedRequest request = httpsMockWebServer.takeRequest();
        assertThat(request.getHeader("Authorization"), equalTo("Basic dXNlcjpwYXNz"));
    }

    @Test
    public void withCredentials_onceDigestChallenged_proactivelySendsCredentials() throws Exception {
        MockWebServer mockWebServer = mockWebServerRule.start();

        enqueueDigestChallenge(mockWebServer);
        enqueueSuccess(mockWebServer);
        enqueueSuccess(mockWebServer);

        OpenRosaServerClient client = subject.get("http", "Android", new HttpCredentials("user", "pass"),"localhost");
        client.makeRequest(buildRequest(mockWebServer, ""), new Date());
        client.makeRequest(buildRequest(mockWebServer, "/different"), new Date());

        assertThat(mockWebServer.getRequestCount(), equalTo(3));
        mockWebServer.takeRequest();
        mockWebServer.takeRequest();
        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getHeader("Authorization"), startsWith("Digest"));
    }

    @Test
    public void withCredentials_onceDigestChallenged_whenHttps_proactivelySendsCredentials() throws Exception {
        MockWebServer httpsMockWebServer = mockWebServerRule.startHTTPS();

        enqueueDigestChallenge(httpsMockWebServer);
        enqueueSuccess(httpsMockWebServer);
        enqueueSuccess(httpsMockWebServer);

        OpenRosaServerClient client = subject.get("https", "Android", new HttpCredentials("user", "pass"),"localhost");
        client.makeRequest(buildRequest(httpsMockWebServer, ""), new Date());
        client.makeRequest(buildRequest(httpsMockWebServer, "/different"), new Date());

        assertThat(httpsMockWebServer.getRequestCount(), equalTo(3));
        httpsMockWebServer.takeRequest();
        httpsMockWebServer.takeRequest();
        RecordedRequest request = httpsMockWebServer.takeRequest();
        assertThat(request.getHeader("Authorization"), startsWith("Digest"));
    }

    @Test
    public void authenticationIsCachedBetweenInstances() throws Exception {
        MockWebServer mockWebServer = mockWebServerRule.start();

        enqueueDigestChallenge(mockWebServer);
        enqueueSuccess(mockWebServer);
        enqueueSuccess(mockWebServer);

        subject.get("http", "Android", new HttpCredentials("user", "pass"),"localhost").makeRequest(buildRequest(mockWebServer, ""), new Date());
        subject.get("http", "Android", new HttpCredentials("user", "pass"),"localhost").makeRequest(buildRequest(mockWebServer, "/different"), new Date());

        assertThat(mockWebServer.getRequestCount(), equalTo(3));
        mockWebServer.takeRequest();
        mockWebServer.takeRequest();
        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getHeader("Authorization"), startsWith("Digest"));
    }

    @Test
    public void whenUsingDifferentCredentials_authenticationIsNotCachedBetweenInstances() throws Exception {
        MockWebServer mockWebServer = mockWebServerRule.start();

        enqueueDigestChallenge(mockWebServer);
        enqueueSuccess(mockWebServer);
        enqueueDigestChallenge(mockWebServer);
        enqueueSuccess(mockWebServer);

        subject.get("http", "Android", new HttpCredentials("user", "pass"),"localhost").makeRequest(buildRequest(mockWebServer, ""), new Date());
        subject.get("http", "Android", new HttpCredentials("new-user", "pass"),"localhost").makeRequest(buildRequest(mockWebServer, "/different"), new Date());

        assertThat(mockWebServer.getRequestCount(), equalTo(4));
        mockWebServer.takeRequest();
        mockWebServer.takeRequest();
        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getHeader("Authorization"), equalTo(null));
    }

    @Test
    public void whenConnectingToDifferentHosts_authenticationIsNotCachedBetweenInstances() throws Exception {
        MockWebServer host1 = mockWebServerRule.start();
        MockWebServer host2 = mockWebServerRule.start();

        enqueueDigestChallenge(host1);
        enqueueSuccess(host1);

        enqueueDigestChallenge(host2);
        enqueueSuccess(host2);

        subject.get("http", "Android", new HttpCredentials("user", "pass"),"localhost").makeRequest(buildRequest(host1, ""), new Date());
        subject.get("http", "Android", new HttpCredentials("user", "pass"),"localhost").makeRequest(buildRequest(host2, ""), new Date());

        assertThat(host2.getRequestCount(), equalTo(2));

        RecordedRequest request = host2.takeRequest();
        assertThat(request.getHeader("Authorization"), equalTo(null));

        host2.shutdown();
    }

    @Test
    public void whenLastRequestSetCookies_nextRequestDoesNotSendThem() throws Exception {
        MockWebServer mockWebServer = mockWebServerRule.start();

        mockWebServer.enqueue(new MockResponse()
                .addHeader("Set-Cookie", "blah=blah"));
        enqueueSuccess(mockWebServer);

        subject.get("http", "Android", new HttpCredentials("user", "pass"),"localhost").makeRequest(buildRequest(mockWebServer, ""), new Date());
        subject.get("http", "Android", new HttpCredentials("user", "pass"),"localhost").makeRequest(buildRequest(mockWebServer, ""), new Date());

        mockWebServer.takeRequest();
        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getHeader("Cookie"), isEmptyOrNullString());
    }

    private void enqueueSuccess(MockWebServer mockWebServer) {
        mockWebServer.enqueue(new MockResponse());
    }

    private void enqueueBasicChallenge(MockWebServer mockWebServer) {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(401)
                .addHeader("WWW-Authenticate: Basic realm=\"protected area\"")
                .setBody("Please authenticate."));
    }

    private void enqueueDigestChallenge(MockWebServer mockWebServer) {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(401)
                .addHeader("WWW-Authenticate: Digest realm=\"ODK Aggregate\", qop=\"auth\", nonce=\"MTU2NTA4MjEzODI4OTpmMjc4MDM5N2YxZTJiNDRiNjNiYTBiMThiOWQ4ZTlkMg==\"")
                .setBody("Please authenticate."));
    }
}
