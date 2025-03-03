package org.odk.collect.openrosa.http;

public class HttpCredentials implements HttpCredentialsInterface {

    private final String username;
    private final String password;

    public HttpCredentials(String username, String password) {
        this.username = (username == null) ? "" : username;
        this.password = (password == null) ? "" : password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (super.equals(obj)) {
            return true;
        }

        return ((HttpCredentials) obj).getUsername().equals(getUsername()) &&
                ((HttpCredentials) obj).getPassword().equals(getPassword());
    }

    @Override
    public int hashCode() {
        return (getUsername() + getPassword()).hashCode();
    }
}
