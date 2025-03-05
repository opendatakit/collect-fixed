package org.odk.collect.openrosa.http;

public class HttpPostResult {

    private final String httpResponse;
    private final int responseCode;
    private final String reasonPhrase;

    public HttpPostResult(String httpResponse, int responseCode, String reasonPhrase) {
        this.httpResponse = httpResponse;
        this.responseCode = responseCode;
        this.reasonPhrase = reasonPhrase;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }

    public String getHttpResponse() {
        return httpResponse;
    }
}
