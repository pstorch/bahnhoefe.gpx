package org.railwaystations.rsapi.auth;

import org.apache.commons.codec.digest.DigestUtils;
import org.railwaystations.rsapi.model.User;

public class TokenGenerator {

    private final String salt;

    public TokenGenerator(final String salt) {
        this.salt = salt;
    }

    public String buildFor(final String email, final Long userSalt) {
        return DigestUtils.sha1Hex(salt + "-" + User.normalizeEmail(email) + "-" + userSalt);
    }

}
