package org.railwaystations.api.auth;

import com.goterl.lazycode.lazysodium.LazySodiumJava;
import com.goterl.lazycode.lazysodium.SodiumJava;
import com.goterl.lazycode.lazysodium.exceptions.SodiumException;
import com.goterl.lazycode.lazysodium.interfaces.PwHash;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;

public class PasswordUtil {

    private static final LazySodiumJava LAZY_SODIUM = new LazySodiumJava(new SodiumJava(), StandardCharsets.UTF_8);
    private static final PwHash.Lazy PW_HASH_LAZY = LAZY_SODIUM;

    public static String hashPassword(final String password) {
        try {
            return PW_HASH_LAZY.cryptoPwHashStr(password, PwHash.OPSLIMIT_INTERACTIVE, PwHash.MEMLIMIT_INTERACTIVE);
        } catch (final SodiumException ex) {
            throw new RuntimeException("Exception encountered in hashPassword()", ex);
        }
    }

    public static boolean verifyPassword(final String password, final String key) {
        if (StringUtils.isBlank(password) || StringUtils.isBlank(key)) {
            return false;
        }
        return PW_HASH_LAZY.cryptoPwHashStrVerify(key, password);
    }

}
