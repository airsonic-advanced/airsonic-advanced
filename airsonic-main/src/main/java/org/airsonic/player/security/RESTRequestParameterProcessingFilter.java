/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.security;

import org.airsonic.player.controller.JAXBWriter;
import org.airsonic.player.controller.SubsonicRESTController;
import org.airsonic.player.controller.SubsonicRESTController.APIException;
import org.airsonic.player.controller.SubsonicRESTController.ErrorCode;
import org.airsonic.player.domain.Version;
import org.airsonic.player.util.StringUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Performs authentication based on credentials being present in the HTTP request parameters. Also checks
 * API versions and license information.
 * <p/>
 * The username should be set in parameter "u", and the password should be set in parameter "p".
 * The REST protocol version should be set in parameter "v".
 * <p/>
 * The password can either be in plain text or be UTF-8 hexencoded preceded by "enc:".
 *
 * @author Sindre Mehus
 */
public class RESTRequestParameterProcessingFilter extends AbstractAuthenticationProcessingFilter {
    private static final Logger LOG = LoggerFactory.getLogger(RESTRequestParameterProcessingFilter.class);

    private static final RequestMatcher requiresAuthenticationRequestMatcher = new RegexRequestMatcher("/rest/.+", null);
    private static final Version serverVersion = new Version(JAXBWriter.getRestProtocolVersion());

    protected RESTRequestParameterProcessingFilter(RequestMatcher requiresAuthenticationRequestMatcher) {
        super(requiresAuthenticationRequestMatcher);
        setAuthenticationFailureHandler(new RESTAuthenticationFailureHandler());
        setAuthenticationSuccessHandler((req, res, auth) -> {
        });
    }

    public RESTRequestParameterProcessingFilter() {
        this(requiresAuthenticationRequestMatcher);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException, IOException, ServletException {
        String username = StringUtils.trimToNull(request.getParameter("u"));
        String password = decrypt(StringUtils.trimToNull(request.getParameter("p")));
        String salt = StringUtils.trimToNull(request.getParameter("s"));
        String token = StringUtils.trimToNull(request.getParameter("t"));
        String version = StringUtils.trimToNull(request.getParameter("v"));
        String client = StringUtils.trimToNull(request.getParameter("c"));

        // The username and credentials parameters are not required if the user
        // was previously authenticated, for example using Basic Auth.
        Authentication previousAuth = SecurityContextHolder.getContext().getAuthentication();
        if (previousAuth != null && previousAuth.isAuthenticated()) {
            return previousAuth;
        }

        boolean passwordOrTokenPresent = password != null || (salt != null && token != null);
        boolean missingCredentials = (username == null || !passwordOrTokenPresent);
        if (missingCredentials || version == null || client == null) {
            throw new AuthenticationServiceException("", new APIException(ErrorCode.MISSING_PARAMETER));
        }

        checkAPIVersion(version);

        UsernamePasswordAuthenticationToken authRequest = null;
        if (salt != null && token != null) {
            authRequest = new UsernameSaltedTokenAuthenticationToken(username, salt, token);
        } else if (password != null) {
            authRequest = new UsernamePasswordAuthenticationToken(username, password);
        }

        authRequest.setDetails(authenticationDetailsSource.buildDetails(request));

        return this.getAuthenticationManager().authenticate(authRequest);
    }

    private void checkAPIVersion(String version) {
        Version clientVersion = new Version(version);

        try {
            if (serverVersion.getMajor() > clientVersion.getMajor()) {
                throw new APIException(ErrorCode.PROTOCOL_MISMATCH_CLIENT_TOO_OLD);
            } else if (serverVersion.getMajor() < clientVersion.getMajor()) {
                throw new APIException(ErrorCode.PROTOCOL_MISMATCH_SERVER_TOO_OLD);
            } else if (serverVersion.getMinor() < clientVersion.getMinor()) {
                throw new APIException(ErrorCode.PROTOCOL_MISMATCH_SERVER_TOO_OLD);
            }
        } catch (APIException e) {
            throw new AuthenticationServiceException("", e);
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
            Authentication authResult) throws IOException, ServletException {
        super.successfulAuthentication(request, response, chain, authResult);
        // carry on with the request
        chain.doFilter(request, response);
    }

    public static String decrypt(String s) {
        if (s == null) {
            return null;
        }
        if (!s.startsWith("enc:")) {
            return s;
        }
        try {
            return StringUtil.utf8HexDecode(s.substring(4));
        } catch (Exception e) {
            return s;
        }
    }

    public static class RESTAuthenticationFailureHandler implements AuthenticationFailureHandler {
        private static final JAXBWriter jaxbWriter = new JAXBWriter();

        @Override
        public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                AuthenticationException exception) throws IOException, ServletException {
            ErrorCode errorCode = null;
            if (exception.getCause() instanceof APIException) {
                errorCode = ((APIException) exception.getCause()).getError();
            } else {
                errorCode = ErrorCode.NOT_AUTHENTICATED;
            }

            sendErrorXml(request, response, errorCode);
        }

        private static void sendErrorXml(HttpServletRequest request, HttpServletResponse response,
                SubsonicRESTController.ErrorCode errorCode) {
            try {
                jaxbWriter.writeErrorResponse(request, response, errorCode, errorCode.getMessage());
            } catch (Exception e) {
                LOG.error("Failed to send error response.", e);
            }
        }
    }
}
