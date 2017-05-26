package org.railwaystations.api;

import org.apache.commons.codec.digest.DigestUtils;

public class UploadTokenGenerator {

    private final String salt;

    public UploadTokenGenerator(final String salt) {
        this.salt = salt;
    }

    public String buildFor(final String nickname) {
        return DigestUtils.sha1Hex(salt + "-" + nickname);
    }

}
