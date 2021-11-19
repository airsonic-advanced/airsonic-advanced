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
package org.airsonic.player.service;

import org.airsonic.player.spring.WebsocketConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

@Service
public class NetworkService {

    private static UrlPathHelper urlPathHelper = new UrlPathHelper();
    private static final String X_FORWARDED_SERVER = "X-Forwarded-Server";
    private static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";
    private static final String X_FORWARDED_HOST = "X-Forwarded-Host";
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String X_FORWARDED_SCHEME = "X-Forwarded-Scheme";

    private final static Logger LOG = LoggerFactory.getLogger(NetworkService.class);

    public static String getBaseUrl(SimpMessageHeaderAccessor websocketHeaders) {
        return getBaseUrl((HttpServletRequest) websocketHeaders.getSessionAttributes().get(WebsocketConfiguration.UNDERLYING_SERVLET_REQUEST));
    }

    public static String getBaseUrl(HttpServletRequest request) {
        try {
            URI uri;
            try {
                uri = calculateProxyUri(request);
            } catch (Exception e) {
                LOG.debug("Could not calculate proxy uri: " + e.getMessage());
                uri = calculateNonProxyUri(request);
            }

            String baseUrl = uri.toString() + "/";
            LOG.debug("Calculated base url to " + baseUrl);
            return baseUrl;
        } catch (MalformedURLException | URISyntaxException e) {
            throw new RuntimeException("Could not calculate base url: " + e.getMessage());
        }
    }

    private static URI calculateProxyUri(HttpServletRequest request) throws URISyntaxException {
        String xForwardedHost = request.getHeader(X_FORWARDED_HOST);
        // If the request has been through multiple reverse proxies,
        // We need to return the original Host that the client used
        if (xForwardedHost != null) {
            xForwardedHost = xForwardedHost.split(",")[0];
        }

        if (!isValidXForwardedHost(xForwardedHost)) {
            xForwardedHost = request.getHeader(X_FORWARDED_SERVER);

            // If the request has been through multiple reverse proxies,
            // We need to return the original Host that the client used
            if (xForwardedHost != null) {
                xForwardedHost = xForwardedHost.split(",")[0];
            }

            if (!isValidXForwardedHost(xForwardedHost)) {
                xForwardedHost = request.getHeader(X_FORWARDED_FOR);

                // If the request has been through multiple reverse proxies,
                // We need to return the original Host that the client used
                if (xForwardedHost != null) {
                    xForwardedHost = xForwardedHost.split(",")[0];
                }

                if (!isValidXForwardedHost(xForwardedHost)) {
                    throw new RuntimeException("Cannot calculate proxy uri without HTTP headers: " + X_FORWARDED_HOST + ", " + X_FORWARDED_SERVER + ", " + X_FORWARDED_FOR);
                }
            }
        }

        URI proxyHost = new URI("ignored://" + xForwardedHost);
        String host = proxyHost.getHost();
        int port = proxyHost.getPort();
        String scheme = request.getHeader(X_FORWARDED_PROTO);
        if (StringUtils.isBlank(scheme)) {
            scheme = request.getHeader(X_FORWARDED_SCHEME);
            if (StringUtils.isBlank(scheme)) {
                throw new RuntimeException("Scheme not provided");
            }
        }

        return new URI(scheme, null, host, port, urlPathHelper.getContextPath(request), null, null);
    }

    private static boolean isValidXForwardedHost(String xForwardedHost) {
        return StringUtils.isNotBlank(xForwardedHost) && !StringUtils.equals("null", xForwardedHost);
    }

    private static URI calculateNonProxyUri(HttpServletRequest request) throws MalformedURLException, URISyntaxException {
        URL url = new URL(request.getRequestURL().toString());
        String host = url.getHost();
        String scheme = url.getProtocol();
        int port = url.getPort();
        String userInfo = url.getUserInfo();
        return new URI(scheme, userInfo, host, port, urlPathHelper.getContextPath(request), null, null);
    }
}
