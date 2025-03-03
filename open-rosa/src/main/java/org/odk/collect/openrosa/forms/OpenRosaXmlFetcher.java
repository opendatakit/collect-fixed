package org.odk.collect.openrosa.forms;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.javarosa.xform.parse.XFormParser;
import org.kxml2.kdom.Document;
import org.odk.collect.openrosa.http.HttpCredentialsInterface;
import org.odk.collect.openrosa.http.HttpGetResult;
import org.odk.collect.openrosa.http.OpenRosaHttpInterface;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import timber.log.Timber;

/**
 * This is only used inside {@link OpenRosaClient} and could potentially be absorbed there. Some
 * of the parsing logic here might be better broken out somewhere else however if it can be used
 * in other scenarios.
 */
public class OpenRosaXmlFetcher {

    private static final String HTTP_CONTENT_TYPE_TEXT_XML = "text/xml";

    private final OpenRosaHttpInterface httpInterface;
    private WebCredentialsProvider webCredentialsUtils;

    OpenRosaXmlFetcher(OpenRosaHttpInterface httpInterface, WebCredentialsProvider webCredentialsUtils) {
        this.httpInterface = httpInterface;
        this.webCredentialsUtils = webCredentialsUtils;
    }

    /**
     * Gets an XML document for a given url
     *
     * @param urlString - url of the XML document
     * @return DocumentFetchResult - an object that contains the results of the "get" operation
     */

    @SuppressWarnings("PMD.AvoidRethrowingException")
    public DocumentFetchResult getXML(String urlString) throws Exception {

        // parse response
        Document doc;
        HttpGetResult inputStreamResult;

        inputStreamResult = fetch(urlString, HTTP_CONTENT_TYPE_TEXT_XML);

        if (inputStreamResult.getStatusCode() != HttpURLConnection.HTTP_OK) {
            String error = "getXML failed while accessing "
                    + urlString + " with status code: " + inputStreamResult.getStatusCode();
            return new DocumentFetchResult(error, inputStreamResult.getStatusCode());
        }

        doc = XFormParser.getXMLDocument(new InputStreamReader(inputStreamResult.getInputStream()));

        return new DocumentFetchResult(doc, inputStreamResult.isOpenRosaResponse(), inputStreamResult.getHash());
    }

    /**
     * Creates a Http connection and input stream
     *
     * @param downloadUrl uri of the stream
     * @param contentType check the returned Mime Type to ensure it matches. "text/xml" causes a Hash to be calculated
     * @return HttpGetResult - An object containing the Stream, Hash and Headers
     * @throws Exception - Can throw a multitude of Exceptions, such as MalformedURLException or IOException
     */

    @NonNull
    public HttpGetResult fetch(@NonNull String downloadUrl, @Nullable final String contentType) throws Exception {
        URI uri;
        try {
            // assume the downloadUrl is escaped properly
            URL url = new URL(downloadUrl);
            uri = url.toURI();
        } catch (MalformedURLException | URISyntaxException e) {
            Timber.e(e, "Unable to get a URI for download URL : %s  due to %s : ", downloadUrl, e.getMessage());
            throw e;
        }

        if (uri.getHost() == null) {
            Timber.e(new Error("Invalid server URL (no hostname): " + downloadUrl));
            throw new Exception("Invalid server URL (no hostname): " + downloadUrl);
        }

        return httpInterface.executeGetRequest(uri, contentType, webCredentialsUtils.getCredentials(uri));
    }

    public void updateWebCredentialsProvider(WebCredentialsProvider webCredentialsUtils) {
        this.webCredentialsUtils = webCredentialsUtils;
    }

    public interface WebCredentialsProvider {
        HttpCredentialsInterface getCredentials(@NonNull URI url);
    }
}
