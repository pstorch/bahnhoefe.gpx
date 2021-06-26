package org.railwaystations.rsapi.auth;

import com.goterl.lazysodium.LazySodiumJava;
import com.goterl.lazysodium.SodiumJava;
import com.goterl.lazysodium.exceptions.SodiumException;
import com.goterl.lazysodium.interfaces.PwHash;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class LazySodiumPasswordEncoder implements PasswordEncoder {

    private static final LazySodiumJava LAZY_SODIUM = new LazySodiumJava(new SodiumJava(), StandardCharsets.UTF_8);
    private static final PwHash.Lazy PW_HASH_LAZY = LAZY_SODIUM;

    @Override
    public String encode(final CharSequence rawPassword) {
        try {
            return PW_HASH_LAZY.cryptoPwHashStr(String.valueOf(rawPassword), PwHash.OPSLIMIT_INTERACTIVE, PwHash.MEMLIMIT_INTERACTIVE);
        } catch (final SodiumException ex) {
            throw new RuntimeException("Exception encountered in hashPassword()", ex);
        }
    }

    @Override
    public boolean matches(final CharSequence rawPassword, final String encodedPassword) {
        if (StringUtils.isBlank(rawPassword) || StringUtils.isBlank(encodedPassword)) {
            return false;
        }
        return PW_HASH_LAZY.cryptoPwHashStrVerify(encodedPassword, String.valueOf(rawPassword));
    }

}
