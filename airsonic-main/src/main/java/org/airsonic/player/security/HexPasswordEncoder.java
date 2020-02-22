package org.airsonic.player.security;

import com.google.common.io.BaseEncoding;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;

public final class HexPasswordEncoder implements PasswordEncoder, PasswordDecoder {
    private final static BaseEncoding hex = BaseEncoding.base16().lowerCase();

    @Override
    public String encode(CharSequence rawPassword) {
        return hex.encode(rawPassword.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String decode(String encoded) {
        return new String(hex.decode(encoded), StandardCharsets.UTF_8);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return StringUtils.equals(encode(rawPassword), encodedPassword);
    }

    public static HexPasswordEncoder getInstance() {
        return INSTANCE;
    }

    private static final HexPasswordEncoder INSTANCE = new HexPasswordEncoder();

    private HexPasswordEncoder() {
    }
}
