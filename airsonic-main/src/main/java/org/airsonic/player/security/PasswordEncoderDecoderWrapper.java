package org.airsonic.player.security;

import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordEncoderDecoderWrapper implements PasswordEncoder, PasswordDecoder {
    public final PasswordEncoder encoder;
    public final PasswordDecoder decoder;

    public PasswordEncoderDecoderWrapper(PasswordEncoder encoder, PasswordDecoder decoder) {
        this.encoder = encoder;
        this.decoder = decoder;
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public String decode(String encoded) throws Exception {
        return decoder.decode(encoded);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
}
