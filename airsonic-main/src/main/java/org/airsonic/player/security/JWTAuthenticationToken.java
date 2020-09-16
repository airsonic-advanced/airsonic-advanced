package org.airsonic.player.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class JWTAuthenticationToken extends UsernamePasswordAuthenticationToken {

    private String requestedPath;

    public JWTAuthenticationToken(Object principal, Object credentials, String requestedPath) {
        super(principal, credentials);
        this.requestedPath = requestedPath;
    }

    public JWTAuthenticationToken(Object principal, Object credentials, String requestedPath, Collection<? extends GrantedAuthority> authorities) {
        super(principal, credentials, authorities);
        this.requestedPath = requestedPath;
    }

    public JWTAuthenticationToken(Object principal, Object credentials, String requestedPath,
            Collection<? extends GrantedAuthority> authorities, Object details) {
        this(principal, credentials, requestedPath, authorities);
        this.setDetails(details);
    }

    public String getRequestedPath() {
        return requestedPath;
    }
}
