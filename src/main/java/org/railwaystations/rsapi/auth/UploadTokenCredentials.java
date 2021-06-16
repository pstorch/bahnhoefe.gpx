package org.railwaystations.rsapi.auth;

import java.util.Objects;

public class UploadTokenCredentials {

    private final String email;
    private final String uploadToken;

    public UploadTokenCredentials(final String email, final String uploadToken) {
        this.email = email;
        this.uploadToken = uploadToken;
    }

    public String getEmail() {
        return email;
    }

    public String getUploadToken() {
        return uploadToken;
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, uploadToken);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final UploadTokenCredentials other = (UploadTokenCredentials) obj;
        return Objects.equals(this.email, other.email)
                && Objects.equals(this.uploadToken, other.uploadToken);
    }


    @Override
    public String toString() {
        return "UploadTokenCredentials{email=" + email + ", uploadToken=**********}";
    }

}
