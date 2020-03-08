package org.airsonic.player.security;

import org.apache.commons.text.StringTokenizer;

public class SaltToken {
    private final String salt;
    private final String token;

    public SaltToken(String salt, String token) {
        this.salt = salt;
        this.token = token;
    }

    public String getSalt() {
        return salt;
    }

    public String getToken() {
        return token;
    }

    public static String SALT_TOKEN_DELIMITER = "$#$SALTTOKEN$#$";

    @Override
    public String toString() {
        return SALT_TOKEN_DELIMITER + salt + SALT_TOKEN_DELIMITER + token + SALT_TOKEN_DELIMITER;
    }

    public static SaltToken fromString(String str) {
        String[] splitString = new StringTokenizer(str, SALT_TOKEN_DELIMITER).setIgnoreEmptyTokens(false)
                .getTokenArray();
        if (splitString.length != 4) {
            throw new IllegalArgumentException("Cannot parse into SaltToken object from this String");
        }

        return new SaltToken(splitString[1], splitString[2]);
    }
}
