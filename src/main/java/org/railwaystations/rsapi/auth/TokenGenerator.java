package org.railwaystations.rsapi.auth;

import org.apache.commons.codec.digest.DigestUtils;
import org.railwaystations.rsapi.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TokenGenerator {

    @Value("${salt}")
    private final String salt;

    public TokenGenerator(final String salt) {
        this.salt = salt;
    }

    public String buildFor(final String email, final Long userSalt) {
        return DigestUtils.sha1Hex(salt + "-" + User.normalizeEmail(email) + "-" + userSalt);
    }

}
