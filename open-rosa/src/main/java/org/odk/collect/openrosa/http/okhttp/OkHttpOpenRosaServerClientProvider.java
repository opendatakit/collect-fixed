package org.odk.collect.openrosa.http.okhttp;

import android.os.Build;

import androidx.annotation.NonNull;

import com.burgstaller.okhttp.AuthenticationCacheInterceptor;
import com.burgstaller.okhttp.CachingAuthenticatorDecorator;
import com.burgstaller.okhttp.DispatchingAuthenticator;
import com.burgstaller.okhttp.basic.BasicAuthenticator;
import com.burgstaller.okhttp.digest.CachingAuthenticator;
import com.burgstaller.okhttp.digest.Credentials;
import com.burgstaller.okhttp.digest.DigestAuthenticator;

import org.odk.collect.openrosa.http.HttpCredentialsInterface;
import org.odk.collect.openrosa.http.OpenRosaConstants;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import kotlin.Pair;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.tls.HandshakeCertificates;
import timber.log.Timber;

public class OkHttpOpenRosaServerClientProvider implements OpenRosaServerClientProvider {

    private static final int CONNECTION_TIMEOUT = 30000;
    private static final int WRITE_CONNECTION_TIMEOUT = 60000; // it can take up to 27 seconds to spin up an Aggregate
    private static final int READ_CONNECTION_TIMEOUT = 60000; // it can take up to 27 seconds to spin up an Aggregate
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String OPEN_ROSA_VERSION_HEADER = OpenRosaConstants.VERSION_HEADER;
    private static final String OPEN_ROSA_VERSION = "1.0";
    private static final String DATE_HEADER = "Date";

    private final OkHttpClient baseClient;
    private final String cacheDir;

    private final Map<Pair<String, HttpCredentialsInterface>, OkHttpOpenRosaServerClient> clients = new HashMap<>();

    public OkHttpOpenRosaServerClientProvider(@NonNull OkHttpClient baseClient, String cacheDir) {
        this.baseClient = baseClient;
        this.cacheDir = cacheDir;
    }

    public OkHttpOpenRosaServerClientProvider(String cacheDir) {
        this(new OkHttpClient(), cacheDir);
    }

    @Override
    public synchronized OpenRosaServerClient get(String scheme, String userAgent, @NonNull HttpCredentialsInterface credentials) {
        OkHttpOpenRosaServerClient existingClient = clients.get(new Pair<>(scheme, credentials));

        if (existingClient == null) {
            OkHttpOpenRosaServerClient newClient = createNewClient(scheme, userAgent, credentials);
            clients.put(new Pair<>(scheme, credentials), newClient);
            return newClient;
        } else {
            return existingClient;
        }
    }

    @NonNull
    private OkHttpOpenRosaServerClient createNewClient(String scheme, String userAgent, @NonNull HttpCredentialsInterface credentials) {
        OkHttpClient.Builder builder = baseClient.newBuilder()
                .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
                .writeTimeout(WRITE_CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(READ_CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
                .followRedirects(true);

        if (cacheDir != null && new File(cacheDir).exists()) {
            builder.cache(new Cache(
                    new File(cacheDir, "http_" + credentials.hashCode()),
                    50L * 1024L * 1024L // 50 MiB
            ));
        }

        // Let's Encrypt root used as of Jan 2021 isn't trusted by Android 7.1.1 and below. Android
        // 7.0 and 7.1 (API 24/25) use network_security_config to get support.
        if (Build.VERSION.SDK_INT <= 23) {
            try {
                addTrustForLetsEncryptRoot(builder);
            } catch (CertificateException e) {
                Timber.w(e, "Failure attempting to add Let's Encrypt root");
            }
        }

        if (credentials != null) {
            Credentials cred = new Credentials(credentials.getUsername(), credentials.getPassword());

            DispatchingAuthenticator.Builder daBuilder = new DispatchingAuthenticator.Builder();
            daBuilder.with("digest", new DigestAuthenticator(cred));
            if (scheme.equalsIgnoreCase("https")) {
                daBuilder.with("basic", new BasicAuthenticator(cred));
            }

            DispatchingAuthenticator authenticator = daBuilder.build();
            ConcurrentHashMap<String, CachingAuthenticator> authCache = new ConcurrentHashMap<>();
            builder.authenticator(new CachingAuthenticatorDecorator(authenticator, authCache))
                    .addInterceptor(new AuthenticationCacheInterceptor(authCache)).build();
        }

        return new OkHttpOpenRosaServerClient(builder.build(), userAgent);
    }

    // https://stackoverflow.com/a/64844360/137744
    private void addTrustForLetsEncryptRoot(OkHttpClient.Builder builder) throws CertificateException {
        String isgCert =
                "-----BEGIN CERTIFICATE-----\n" +
                        "MIIFazCCA1OgAwIBAgIRAIIQz7DSQONZRGPgu2OCiwAwDQYJKoZIhvcNAQELBQAw\n" +
                        "TzELMAkGA1UEBhMCVVMxKTAnBgNVBAoTIEludGVybmV0IFNlY3VyaXR5IFJlc2Vh\n" +
                        "cmNoIEdyb3VwMRUwEwYDVQQDEwxJU1JHIFJvb3QgWDEwHhcNMTUwNjA0MTEwNDM4\n" +
                        "WhcNMzUwNjA0MTEwNDM4WjBPMQswCQYDVQQGEwJVUzEpMCcGA1UEChMgSW50ZXJu\n" +
                        "ZXQgU2VjdXJpdHkgUmVzZWFyY2ggR3JvdXAxFTATBgNVBAMTDElTUkcgUm9vdCBY\n" +
                        "MTCCAiIwDQYJKoZIhvcNAQEBBQADggIPADCCAgoCggIBAK3oJHP0FDfzm54rVygc\n" +
                        "h77ct984kIxuPOZXoHj3dcKi/vVqbvYATyjb3miGbESTtrFj/RQSa78f0uoxmyF+\n" +
                        "0TM8ukj13Xnfs7j/EvEhmkvBioZxaUpmZmyPfjxwv60pIgbz5MDmgK7iS4+3mX6U\n" +
                        "A5/TR5d8mUgjU+g4rk8Kb4Mu0UlXjIB0ttov0DiNewNwIRt18jA8+o+u3dpjq+sW\n" +
                        "T8KOEUt+zwvo/7V3LvSye0rgTBIlDHCNAymg4VMk7BPZ7hm/ELNKjD+Jo2FR3qyH\n" +
                        "B5T0Y3HsLuJvW5iB4YlcNHlsdu87kGJ55tukmi8mxdAQ4Q7e2RCOFvu396j3x+UC\n" +
                        "B5iPNgiV5+I3lg02dZ77DnKxHZu8A/lJBdiB3QW0KtZB6awBdpUKD9jf1b0SHzUv\n" +
                        "KBds0pjBqAlkd25HN7rOrFleaJ1/ctaJxQZBKT5ZPt0m9STJEadao0xAH0ahmbWn\n" +
                        "OlFuhjuefXKnEgV4We0+UXgVCwOPjdAvBbI+e0ocS3MFEvzG6uBQE3xDk3SzynTn\n" +
                        "jh8BCNAw1FtxNrQHusEwMFxIt4I7mKZ9YIqioymCzLq9gwQbooMDQaHWBfEbwrbw\n" +
                        "qHyGO0aoSCqI3Haadr8faqU9GY/rOPNk3sgrDQoo//fb4hVC1CLQJ13hef4Y53CI\n" +
                        "rU7m2Ys6xt0nUW7/vGT1M0NPAgMBAAGjQjBAMA4GA1UdDwEB/wQEAwIBBjAPBgNV\n" +
                        "HRMBAf8EBTADAQH/MB0GA1UdDgQWBBR5tFnme7bl5AFzgAiIyBpY9umbbjANBgkq\n" +
                        "hkiG9w0BAQsFAAOCAgEAVR9YqbyyqFDQDLHYGmkgJykIrGF1XIpu+ILlaS/V9lZL\n" +
                        "ubhzEFnTIZd+50xx+7LSYK05qAvqFyFWhfFQDlnrzuBZ6brJFe+GnY+EgPbk6ZGQ\n" +
                        "3BebYhtF8GaV0nxvwuo77x/Py9auJ/GpsMiu/X1+mvoiBOv/2X/qkSsisRcOj/KK\n" +
                        "NFtY2PwByVS5uCbMiogziUwthDyC3+6WVwW6LLv3xLfHTjuCvjHIInNzktHCgKQ5\n" +
                        "ORAzI4JMPJ+GslWYHb4phowim57iaztXOoJwTdwJx4nLCgdNbOhdjsnvzqvHu7Ur\n" +
                        "TkXWStAmzOVyyghqpZXjFaH3pO3JLF+l+/+sKAIuvtd7u+Nxe5AW0wdeRlN8NwdC\n" +
                        "jNPElpzVmbUq4JUagEiuTDkHzsxHpFKVK7q4+63SM1N95R1NbdWhscdCb+ZAJzVc\n" +
                        "oyi3B43njTOQ5yOf+1CceWxG1bQVs5ZufpsMljq4Ui0/1lvh+wjChP4kqKOJ2qxq\n" +
                        "4RgqsahDYVvTH9w7jXbyLeiNdd8XM2w9U/t7y0Ff/9yi0GE44Za4rF2LN9d11TPA\n" +
                        "mRGunUHBcnWEvgJBQl9nJEiU0Zsnvgc/ubhPgXRR4Xq37Z0j4r7g1SgEEzwxA57d\n" +
                        "emyPxgcYxn/eR44/KJ4EBs+lVDR3veyJm+kXQ99b21/+jh5Xos1AnX5iItreGCc=\n" +
                        "-----END CERTIFICATE-----";

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Certificate isgCertificate = cf.generateCertificate(new ByteArrayInputStream(isgCert.getBytes(StandardCharsets.UTF_8)));

        HandshakeCertificates certificates = new HandshakeCertificates.Builder()
                .addTrustedCertificate((X509Certificate) isgCertificate)
                .addPlatformTrustedCertificates() // Add all other non Let's Encrypt certs
                .build();

        builder.sslSocketFactory(certificates.sslSocketFactory(), certificates.trustManager());
    }

    private static class OkHttpOpenRosaServerClient implements OpenRosaServerClient {

        private final OkHttpClient client;
        private final String userAgent;

        OkHttpOpenRosaServerClient(OkHttpClient client, String userAgent) {
            this.client = client;
            this.userAgent = userAgent;
        }

        @Override
        public Response makeRequest(Request request, Date currentTime) throws IOException {
            return client.newCall(request.newBuilder()
                    .addHeader(USER_AGENT_HEADER, userAgent)
                    .addHeader(OPEN_ROSA_VERSION_HEADER, OPEN_ROSA_VERSION)
                    .addHeader(DATE_HEADER, getHeaderDate(currentTime))
                    .build()).execute();
        }

        private static String getHeaderDate(Date currentTime) {
            SimpleDateFormat dateFormatGmt = new SimpleDateFormat("E, dd MMM yyyy hh:mm:ss zz", Locale.US);
            dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
            return dateFormatGmt.format(currentTime);
        }
    }
}
