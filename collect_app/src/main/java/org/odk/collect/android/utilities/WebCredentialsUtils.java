package org.odk.collect.android.utilities;

import android.net.Uri;

import androidx.annotation.NonNull;

import org.odk.collect.metadata.PropertyManager;
import org.odk.collect.openrosa.forms.OpenRosaXmlFetcher;
import org.odk.collect.openrosa.http.HttpCredentials;
import org.odk.collect.openrosa.http.HttpCredentialsInterface;
import org.odk.collect.settings.keys.ProjectKeys;
import org.odk.collect.shared.settings.Settings;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Singleton;

@Singleton
public class WebCredentialsUtils implements OpenRosaXmlFetcher.WebCredentialsProvider {

    private final Settings generalSettings;

    private static final Map<String, HttpCredentialsInterface> HOST_CREDENTIALS = new HashMap<>();

    public WebCredentialsUtils(Settings generalSettings) {
        this.generalSettings = generalSettings;
    }

    public void saveCredentials(@NonNull String url, @NonNull String username, @NonNull String password) {
        if (username.isEmpty()) {
            return;
        }

        String host = Uri.parse(url).getHost();
        HOST_CREDENTIALS.put(host, new HttpCredentials(username, password));
    }

    public void saveCredentialsPreferences(String userName, String password, PropertyManager propertyManager) {
        generalSettings.save(ProjectKeys.KEY_USERNAME, userName);
        generalSettings.save(ProjectKeys.KEY_PASSWORD, password);

        propertyManager.reload();
    }

    /**
     * Forgets the temporary credentials saved in memory for a particular host. This is used when an
     * activity that does some work requiring authentication is called with intent extras specifying
     * credentials. Once the work is done, the temporary credentials are cleared so that different
     * ones can be used on a later request.
     *
     * TODO: is this necessary in all cases it's used? Maybe it's needed if we want to be able to do
     * an authenticated call followed by an anonymous one but even then, can't we pass in null
     * username and password if the intent extras aren't set?
     */
    public void clearCredentials(@NonNull String url) {
        if (url.isEmpty()) {
            return;
        }

        String host = Uri.parse(url).getHost();
        if (host != null) {
            HOST_CREDENTIALS.remove(host);
        }
    }

    public static void clearAllCredentials() {
        HOST_CREDENTIALS.clear();
    }

    public String getServerUrlFromPreferences() {
        return generalSettings.getString(ProjectKeys.KEY_SERVER_URL);
    }

    public String getPasswordFromPreferences() {
        return generalSettings.getString(ProjectKeys.KEY_PASSWORD);
    }

    public String getUserNameFromPreferences() {
        return generalSettings.getString(ProjectKeys.KEY_USERNAME);
    }

    /**
     * Returns a credentials object from the url
     *
     * @param url to find the credentials object
     * @return either null or an instance of HttpCredentialsInterface
     */
    public @NonNull HttpCredentialsInterface getCredentials(@NonNull URI url) {
        String host = url.getHost();
        String serverPrefsUrl = getServerUrlFromPreferences();
        String prefsServerHost = (serverPrefsUrl == null) ? null : Uri.parse(serverPrefsUrl).getHost();

        HttpCredentialsInterface hostCredentials = HOST_CREDENTIALS.get(host);

        // URL host is the same as the host in preferences
        if (prefsServerHost != null && prefsServerHost.equalsIgnoreCase(host)) {
            // Use the temporary credentials if they exist, otherwise use the credentials saved to preferences
            if (hostCredentials != null) {
                return hostCredentials;
            } else {
                return new HttpCredentials(getUserNameFromPreferences(), getPasswordFromPreferences());
            }
        } else {
            if (hostCredentials != null) {
                return hostCredentials;
            } else {
                return new HttpCredentials("", "");
            }
        }
    }

}
