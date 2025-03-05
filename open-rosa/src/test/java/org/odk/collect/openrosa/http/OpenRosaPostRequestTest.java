package org.odk.collect.openrosa.http;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.isEmptyString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static java.util.Arrays.asList;

import androidx.annotation.NonNull;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.odk.collect.openrosa.support.MockWebServerRule;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;

public abstract class OpenRosaPostRequestTest {

    protected abstract OpenRosaHttpInterface buildSubject(OpenRosaHttpInterface.FileToContentTypeMapper mapper);

    @Rule
    public MockWebServerRule mockWebServerRule = new MockWebServerRule();

    private MockWebServer mockWebServer;
    private OpenRosaHttpInterface subject;

    @Before
    public void setup() throws Exception {
        subject = buildSubject(new XmlOrBlahContentTypeMapper());
        mockWebServer = mockWebServerRule.start();
    }

    @Test
    public void makesAPostRequestToUri() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(201));

        URI uri = mockWebServer.url("/blah").uri();
        subject.uploadSubmissionAndFiles(File.createTempFile("blah", "blah"), new ArrayList<>(), uri, null, 0);

        assertThat(mockWebServer.getRequestCount(), equalTo(1));

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod(), equalTo("POST"));
        assertThat(request.getRequestUrl().uri(), equalTo(uri));
    }

    @Test
    public void returnsPostResult() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("I AM BODY"));

        URI uri = mockWebServer.url("/blah").uri();
        HttpPostResult response = subject.uploadSubmissionAndFiles(File.createTempFile("blah", "blah"), new ArrayList<>(), uri, null, 0);

        assertThat(response.getResponseCode(), equalTo(200));
        assertThat(response.getHttpResponse(), equalTo("I AM BODY"));
    }

    @Test
    public void whenResponseIsGzipped_returnsBody() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Encoding", "gzip")
                .setBody(new Buffer().write(gzip("I AM BODY"))));

        URI uri = mockWebServer.url("/blah").uri();
        HttpPostResult response = subject.uploadSubmissionAndFiles(File.createTempFile("blah", "blah"), new ArrayList<>(), uri, null, 0);

        assertThat(response.getHttpResponse(), equalTo("I AM BODY"));
    }

    @Test(expected = Exception.class)
    public void whenResponseIs204_throwsException() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(204));

        URI uri = mockWebServer.url("/blah").uri();
        subject.uploadSubmissionAndFiles(File.createTempFile("blah", "blah"), new ArrayList<>(), uri, null, 0);
    }

    @Test
    public void whenThereIsAServerError_returnsPostBody() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("blah"));

        URI uri = mockWebServer.url("/blah").uri();
        HttpPostResult response = subject.uploadSubmissionAndFiles(File.createTempFile("blah", "blah"), new ArrayList<>(), uri, null, 0);

        assertThat(response, notNullValue());
        assertThat(response.getResponseCode(), equalTo(500));
    }

    @Test
    public void whenRequestFails_throwsExceptionWithMessage() {
        try {
            URI uri = new URI("http://localhost:8443");
            subject.uploadSubmissionAndFiles(File.createTempFile("blah", "blah"), new ArrayList<>(), uri, null, 0);
            fail();
        } catch (Exception e) {
            assertThat(e, isA(Exception.class));
            assertThat(e.getMessage(), not(isEmptyString()));
        }
    }

    @Test
    public void sendsSubmissionFileAsFirstPartOfBody() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(201));

        URI uri = mockWebServer.url("/blah").uri();
        String submissionContent = "<node>content</node>";
        File tempFile = createTempFile(submissionContent);
        subject.uploadSubmissionAndFiles(tempFile, new ArrayList<>(), uri, null, 0);

        RecordedRequest request = mockWebServer.takeRequest();
        String[] firstPartLines = splitMultiPart(request).get(0);
        assertThat(firstPartLines[1], containsString("name=\"xml_submission_file\""));
        assertThat(firstPartLines[1], containsString("filename=\"" + tempFile.getName() + "\""));
        assertThat(firstPartLines[2], containsString("Content-Type: text/xml"));
        assertThat(firstPartLines[5], equalTo("<node>content</node>"));
    }

    @Test
    public void sendsAttachmentsAsPartsOfBody() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(201));

        URI uri = mockWebServer.url("/blah").uri();
        File attachment1 = createTempFile("blah blah blah");
        File attachment2 = createTempFile("blah2 blah2 blah2");
        subject.uploadSubmissionAndFiles(createTempFile("<node>content</node>"), asList(attachment1, attachment2), uri, null, 1024);

        RecordedRequest request = mockWebServer.takeRequest();
        List<String[]> parts = splitMultiPart(request);

        String[] secondPartLines = parts.get(1);
        assertThat(secondPartLines[1], containsString("name=\"" + attachment1.getName() + "\""));
        assertThat(secondPartLines[1], containsString("filename=\"" + attachment1.getName() + "\""));
        assertThat(secondPartLines[5], equalTo("blah blah blah"));

        String[] thirdPartLines = parts.get(2);
        assertThat(thirdPartLines[1], containsString("name=\"" + attachment2.getName() + "\""));
        assertThat(thirdPartLines[1], containsString("filename=\"" + attachment2.getName() + "\""));
        assertThat(thirdPartLines[5], equalTo("blah2 blah2 blah2"));
    }

    @Test
    public void sendsAttachmentsAsPartsOfBody_withContentType() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(201));

        URI uri = mockWebServer.url("/blah").uri();
        File xmlAttachment = createTempFile("<node>blah blah blah</node>", ".xml");
        File plainAttachment = createTempFile("blah", ".blah");

        subject.uploadSubmissionAndFiles(createTempFile("<node>content</node>"), asList(xmlAttachment, plainAttachment), uri, null, 1024);

        RecordedRequest request = mockWebServer.takeRequest();
        List<String[]> parts = splitMultiPart(request);
        assertThat(parts.get(1)[2], containsString("Content-Type: text/xml"));
        assertThat(parts.get(2)[2], containsString("Content-Type: text/blah"));
    }

    @Test
    public void whenMoreThanOneAttachment_andRequestIsLargerThanMaxContentLength_sendsTwoRequests() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(201));
        mockWebServer.enqueue(new MockResponse().setResponseCode(201));

        URI uri = mockWebServer.url("/blah").uri();
        File attachment1 = createTempFile("blah blah blah");
        File attachment2 = createTempFile("blah2 blah2 blah2");
        subject.uploadSubmissionAndFiles(createTempFile("<node>content</node>"), asList(attachment1, attachment2), uri, null, 0);

        RecordedRequest request = mockWebServer.takeRequest();
        List<String[]> parts = splitMultiPart(request);

        assertThat(parts.size(), equalTo(3));

        String[] secondPartLines = parts.get(1);
        assertThat(secondPartLines[1], containsString("name=\"" + attachment1.getName() + "\""));
        assertThat(secondPartLines[1], containsString("filename=\"" + attachment1.getName() + "\""));
        assertThat(secondPartLines[5], equalTo("blah blah blah"));

        String[] thirdPartLines = parts.get(2);
        assertThat(thirdPartLines[1], containsString("name=\"*isIncomplete*\""));

        request = mockWebServer.takeRequest();
        parts = splitMultiPart(request);

        assertThat(parts.size(), equalTo(2));

        secondPartLines = parts.get(1);
        assertThat(secondPartLines[1], containsString("name=\"" + attachment2.getName() + "\""));
        assertThat(secondPartLines[1], containsString("filename=\"" + attachment2.getName() + "\""));
        assertThat(secondPartLines[5], equalTo("blah2 blah2 blah2"));
    }

    @Test
    public void whenMoreThanOneAttachment_andRequestIsLargerThanMaxContentLength__andFirstRequestIs500_returnsErrorResult() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        URI uri = mockWebServer.url("/blah").uri();
        File attachment1 = createTempFile("blah blah blah");
        File attachment2 = createTempFile("blah2 blah2 blah2");
        HttpPostResult response = subject.uploadSubmissionAndFiles(createTempFile("<node>content</node>"), asList(attachment1, attachment2), uri, null, 0);

        assertThat(mockWebServer.getRequestCount(), equalTo(1));
        assertThat(response, notNullValue());
        assertThat(response.getResponseCode(), equalTo(500));
    }

    @Test
    public void whenMoreThanOneAttachment_andRequestIsLargerThanMaxContentLength_andSecondRequestIs500_returnsErrorResult() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(201));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        URI uri = mockWebServer.url("/blah").uri();
        File attachment1 = createTempFile("blah blah blah");
        File attachment2 = createTempFile("blah2 blah2 blah2");
        HttpPostResult response = subject.uploadSubmissionAndFiles(createTempFile("<node>content</node>"), asList(attachment1, attachment2), uri, null, 0);

        assertThat(mockWebServer.getRequestCount(), equalTo(2));
        assertThat(response, notNullValue());
        assertThat(response.getResponseCode(), equalTo(500));
    }

    private File createTempFile(String content) throws Exception {
        return createTempFile(content, ".tmp");
    }

    private File createTempFile(String content, String extension) throws Exception {
        File temp = File.createTempFile("tempfile", extension);

        BufferedWriter bw = new BufferedWriter(new FileWriter(temp));
        bw.write(content);
        bw.close();

        return temp;
    }

    private List<String[]> splitMultiPart(RecordedRequest request) {
        String body = request.getBody().readUtf8();
        String boundary = body.split("\r\n")[0];
        String[] split = body.split(boundary);
        String[] stringParts = Arrays.copyOfRange(split, 1, split.length - 1);
        return Arrays.stream(stringParts).map(part -> part.split("\r\n")).collect(Collectors.toList());
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

    private class XmlOrBlahContentTypeMapper implements OpenRosaHttpInterface.FileToContentTypeMapper {

        @NonNull
        @Override
        public String map(String fileName) {
            if (fileName.endsWith(".xml")) {
                return "text/xml";
            } else {
                return "text/blah";
            }
        }
    }
}
