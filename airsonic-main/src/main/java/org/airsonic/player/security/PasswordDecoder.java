package org.airsonic.player.security;

public interface PasswordDecoder {
    public String decode(String encoded) throws Exception;
}
