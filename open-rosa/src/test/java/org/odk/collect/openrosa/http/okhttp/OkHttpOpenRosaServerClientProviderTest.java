package org.odk.collect.openrosa.http.okhttp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import org.junit.Test;
import org.odk.collect.openrosa.http.HttpCredentials;
import org.odk.collect.openrosa.support.MockWebServerHelper;
import org.odk.collect.shared.TempFiles;

import java.io.File;
import java.util.Date;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.tls.internal.TlsUtil;

public class OkHttpOpenRosaServerClientProviderTest extends OpenRosaServerClientProviderTest {

    @Override
    protected OpenRosaServerClientProvider buildSubject() {
        return buildSubject(null);
    }

    private OkHttpOpenRosaServerClientProvider buildSubject(String cacheDir) {
        OkHttpClient baseClient = new OkHttpClient.Builder()
                .sslSocketFactory(
                        TlsUtil.localhost().sslSocketFactory(),
                        TlsUtil.localhost().trustManager())
                .build();

        return new OkHttpOpenRosaServerClientProvider(baseClient, cacheDir);
    }

    @Test
    public void differentCredentialsHaveDifferentInstances() {
        OpenRosaServerClientProvider provider = buildSubject();

        OpenRosaServerClient instance1 = provider.get("http", "Android", new HttpCredentials("user", "pass"));
        OpenRosaServerClient instance2 = provider.get("http", "Android", new HttpCredentials("other", "pass"));
        OpenRosaServerClient instance3 = provider.get("http", "Android", new HttpCredentials("user", "pass"));


        assertThat(instance1, not(equalTo(instance2)));
        assertThat(instance1, equalTo(instance3));
    }

    @Test
    public void whenCacheDirDoesNotExist_doesNotCreateCache() throws Exception {
        File noneExistingFile = new File(TempFiles.getPathInTempDir());
        assertThat(noneExistingFile.exists(), equalTo(false));

        MockWebServer mockWebServer = mockWebServerRule.start();
        enqueueSuccess(mockWebServer);

        OkHttpOpenRosaServerClientProvider provider = buildSubject(noneExistingFile.getAbsolutePath());
        OpenRosaServerClient client = provider.get("http", "Android", new HttpCredentials("", ""));
        client.makeRequest(MockWebServerHelper.buildRequest(mockWebServer, ""), new Date());

        assertThat(noneExistingFile.exists(), equalTo(false));
    }

    @Test
    public void whenCacheDirIsFile_doesNotCreateCache() throws Exception {
        File file = new File(TempFiles.getPathInTempDir());
        file.createNewFile();
        assertThat(file.exists(), equalTo(true));
        assertThat(file.isDirectory(), equalTo(false));

        MockWebServer mockWebServer = mockWebServerRule.start();
        enqueueSuccess(mockWebServer);

        OkHttpOpenRosaServerClientProvider provider = buildSubject(file.getAbsolutePath());
        OpenRosaServerClient client = provider.get("http", "Android", new HttpCredentials("", ""));
        client.makeRequest(MockWebServerHelper.buildRequest(mockWebServer, ""), new Date());

        assertThat(file.exists(), equalTo(true));
        assertThat(file.isDirectory(), equalTo(false));
    }
}
