package org.odk.collect.openrosa.http;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.odk.collect.openrosa.support.MockWebServerRule;
import org.odk.collect.shared.strings.Md5;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.zip.GZIPOutputStream;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;

public abstract class OpenRosaGetRequestTest {

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
    public void makesAGetRequestToUri() throws Exception {
        mockWebServer.enqueue(new MockResponse());

        URI uri = mockWebServer.url("/blah").uri();
        subject.executeGetRequest(uri, null, null);

        assertThat(mockWebServer.getRequestCount(), equalTo(1));

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod(), equalTo("GET"));
        assertThat(request.getRequestUrl().uri(), equalTo(uri));
    }

    @Test
    public void sendsCollectHeaders() throws Exception {
        mockWebServer.enqueue(new MockResponse());

        subject.executeGetRequest(mockWebServer.url("").uri(), null, null);

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getHeader("User-Agent"), equalTo(USER_AGENT));
    }

    @Test
    public void returnsBodyWithEmptyHash() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setBody("I AM BODY"));

        HttpGetResult result = subject.executeGetRequest(mockWebServer.url("").uri(), null, null);
        assertThat(IOUtils.toString(result.getInputStream(), Charset.defaultCharset()), equalTo("I AM BODY"));
        assertThat(result.getHash(), equalTo(""));
    }

    @Test
    public void whenResponseIsGzipped_returnsBody() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .addHeader("Content-Encoding", "gzip")
                .setBody(new Buffer().write(gzip("I AM BODY"))));

        HttpGetResult result = subject.executeGetRequest(mockWebServer.url("").uri(), null, null);
        assertThat(IOUtils.toString(result.getInputStream(), Charset.defaultCharset()), equalTo("I AM BODY"));
    }

    @Test
    public void whenContentTypeIsXML_returnsBodyWithMD5Hash() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .addHeader("Content-Type", "text/xml")
                .setBody("I AM BODY"));

        HttpGetResult result = subject.executeGetRequest(mockWebServer.url("").uri(), "text/xml", null);
        assertThat(IOUtils.toString(result.getInputStream(), Charset.defaultCharset()), equalTo("I AM BODY"));
        assertThat(result.getHash(), equalTo(Md5.getMd5Hash(new ByteArrayInputStream("I AM BODY".getBytes()))));
    }

    @Test(expected = Exception.class)
    public void withContentType_whenResponseHasDifferentContentType_throwsException() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json"));

        subject.executeGetRequest(mockWebServer.url("").uri(), "text/xml", null);
    }

    @Test
    public void withContentType_whenResponseContainsContentType_returnsResult() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .setBody("I AM BODY"));

        HttpGetResult result = subject.executeGetRequest(mockWebServer.url("").uri(), "application/json", null);
        assertThat(IOUtils.toString(result.getInputStream(), Charset.defaultCharset()), equalTo("I AM BODY"));
    }

    @Test
    public void returnsOpenRosaVersion() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .addHeader(OpenRosaConstants.VERSION_HEADER, "1.0"));

        HttpGetResult result1 = subject.executeGetRequest(mockWebServer.url("").uri(), null, null);
        assertThat(result1.isOpenRosaResponse(), equalTo(true));

        mockWebServer.enqueue(new MockResponse());

        HttpGetResult result2 = subject.executeGetRequest(mockWebServer.url("").uri(), null, null);
        assertThat(result2.isOpenRosaResponse(), equalTo(false));
    }

    @Test
    public void whenStatusCodeIsNot200_returnsNullBodyAndStatusCode() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        HttpGetResult result = subject.executeGetRequest(mockWebServer.url("").uri(), null, null);
        assertThat(result.getInputStream(), nullValue());
        assertThat(result.getStatusCode(), equalTo(500));
    }

    @Test
    public void whenResponseBodyIsNull_returnsNullBodyAndStatusCode() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(204));

        HttpGetResult result1 = subject.executeGetRequest(mockWebServer.url("").uri(), null, null);
        assertThat(result1.getInputStream(), nullValue());
        assertThat(result1.getStatusCode(), equalTo(204));

        mockWebServer.enqueue(new MockResponse().setResponseCode(304));

        HttpGetResult result2 = subject.executeGetRequest(mockWebServer.url("").uri(), null, null);
        assertThat(result2.getInputStream(), nullValue());
        assertThat(result2.getStatusCode(), equalTo(304));
    }

    private static byte[] gzip(String data) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length());
        GZIPOutputStream gzipStream = new GZIPOutputStream(outputStream);
        gzipStream.write(data.getBytes());
        gzipStream.close();

        byte[] compressed = outputStream.toByteArray();
        outputStream.close();

        return compressed;
    }
}
