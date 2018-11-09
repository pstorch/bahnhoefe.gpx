package org.railwaystations.api;

import org.apache.commons.codec.digest.DigestUtils;
import org.railwaystations.api.model.User;

public class TokenGenerator {

    private final String salt;

    public TokenGenerator(final String salt) {
        this.salt = salt;
    }

    public String buildFor(final String nickname, final String email, final Long userSalt) {
        if (userSalt != null) {
            return DigestUtils.sha1Hex(salt + "-" + User.normalizeEmail(email) + "-" + userSalt);
        }

        // for backward compatibility
        return DigestUtils.sha1Hex(salt + "-" + nickname + "-" + User.normalizeEmail(email));
    }

}
