package org.airsonic.player.security;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.crypto.password.PasswordEncoder;

public class AesGcmPasswordEncoder implements PasswordEncoder, PasswordDecoder {
    private static final Logger LOG = LoggerFactory.getLogger(AesGcmPasswordEncoder.class);
    private final TextEncryptor encryptor;

    public AesGcmPasswordEncoder(String password, String salt) {
        this.encryptor = Encryptors.delux(password, salt);
    }

    @Override
    public String decode(String encoded) throws Exception {
        return encryptor.decrypt(encoded);
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return encryptor.encrypt(rawPassword.toString());
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        try {
            return StringUtils.equals(rawPassword, decode(encodedPassword));
        } catch (Exception e) {
            LOG.warn("Exception while trying to match passwords", e);
            return false;
        }
    }

    // The following used as placeholder before the real instance with actual
    // password is instantiated
    private static final String CONSTANT_PASSWORD = "airsonicencryptorpassword";
    private static final String CONSTANT_SALT = "44C82DD88E1FA9672628C3637B7F6976";

    public AesGcmPasswordEncoder() {
        this(CONSTANT_PASSWORD, CONSTANT_SALT);
    }
}
