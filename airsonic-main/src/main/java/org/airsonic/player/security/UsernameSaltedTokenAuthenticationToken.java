package org.airsonic.player.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

public class UsernameSaltedTokenAuthenticationToken extends UsernamePasswordAuthenticationToken {

    public UsernameSaltedTokenAuthenticationToken(Object principal, String salt, String token) {
        super(principal, new SaltToken(salt, token).toString());
    }

}
