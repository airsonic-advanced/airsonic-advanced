package org.airsonic.player.service.sonos;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.collect.Sets;

import com.sonos.services._1.Credentials;

import org.airsonic.player.dao.SonosLinkDao;
import org.airsonic.player.domain.SonosLink;
import org.airsonic.player.domain.User;
import org.airsonic.player.security.JWTAuthenticationProvider.VerificationCheck;
import org.airsonic.player.security.JWTAuthenticationToken;
import org.airsonic.player.service.SettingsService;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.headers.Header;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.w3c.dom.Node;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

import java.util.Set;

import static org.airsonic.player.service.sonos.SonosServiceRegistration.AuthenticationType;

/**
 * <p>
 * Interceptor for the Soap sonos, validates access for all methods (Soap
 * Action) except methods for exchanging initial tokens between airsonic and
 * sonos controller.
 * </p>
 *
 * <p>
 * The validation can be adapted for different link methods from sonos.
 * </p>
 */
@Component
public class SonosLinkInterceptor extends AbstractSoapInterceptor {
    private static final Logger LOG = LoggerFactory.getLogger(SonosLinkInterceptor.class);

    public static final String CLAIM_HOUSEHOLDID = "householdid";
    public static final String CLAIM_LINKCODE = "linkcode";
    public static final String CLAIM_AUTHTYPE = "authtype";

    // these do not carry creds
    private static Set<String> openMethod = Sets.newHashSet("getAppLink", "getDeviceAuthToken");

    public SonosLinkInterceptor() {
        super(Phase.PRE_INVOKE);
    }

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private SonosHelper sonosHelper;

    private JAXBContext jaxbContext;

    @PostConstruct
    public void postConstruct() throws JAXBException {
        jaxbContext = JAXBContext.newInstance("com.sonos.services._1");
    }

    @Override
    public void handleMessage(SoapMessage message) throws Fault {
        try {
            if (!settingsService.isSonosEnabled()) {
                throw new SonosSoapFault.LoginUnauthorized();
            }

            HttpServletRequest request = (HttpServletRequest) message.get(AbstractHTTPDestination.HTTP_REQUEST);

            String action = getAction(message);

            if (action != null) {
                if (openMethod.contains(action)) {
                    LOG.debug("No auth required for SOAP message: {}", message.toString());
                    return;
                }
                AuthenticationType authenticationType = AuthenticationType.valueOf(settingsService.getSonosLinkMethod());
                String sonosLinkToken = null;
                if (authenticationType == AuthenticationType.APPLICATION_LINK) {
                    sonosLinkToken = getToken(message);
                } else if (authenticationType == AuthenticationType.ANONYMOUS) {
                    sonosLinkToken = sonosHelper.createJwt(
                            new SonosLink(User.USERNAME_SONOS, "irrelevant", "irrelevant"),
                            request.getRequestURI() + "?" + request.getQueryString(),
                            AuthenticationType.ANONYMOUS.toString());
                }

                if (sonosLinkToken != null) {
                    JWTAuthenticationToken token = new JWTAuthenticationToken(null, sonosLinkToken,
                            request.getRequestURI() + "?" + request.getQueryString());
                    SecurityContextHolder.getContext().setAuthentication(authenticationManager.authenticate(token));
                    return;
                }
            }

            LOG.debug("Unable to process SOAP message: {}", message.toString());
            throw new SonosSoapFault.LoginUnauthorized();
        } catch (CredentialsExpiredException e) {
            throw new SonosSoapFault.AuthTokenExpired();
        } catch (Exception e) {
            throw new SonosSoapFault.LoginUnauthorized();
        }
    }

    private String getToken(SoapMessage message) throws JAXBException {
        QName creadentialQName = new QName("http://www.sonos.com/Services/1.1", "credentials");

        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

        for (Header header : message.getHeaders()) {
            if (creadentialQName.equals(header.getName())) {
                Credentials credentials = unmarshaller.unmarshal((Node) header.getObject(), Credentials.class).getValue();

                return credentials.getLoginToken().getToken();
            }
        }

        return null;
    }

    private String getAction(SoapMessage message) {
        Object soapAction = message.get("SOAPAction");

        if (soapAction instanceof String) {
            String[] split = ((String) soapAction).split("#");
            if (split.length > 1) {
                return split[1];
            }
        }
        return null;

    }

    /**
     * Separate class to prevent circular dependency chain This class injects into
     * GlobalSecurityConfig as a dependency to the JWTAuthProvider JWTAuthProvider
     * is a dependency for AuthManager AuthManager is a dependency for the
     * SonosLinkSecurityInterceptor class above
     *
     * Verifier -> AuthProvider -> AuthManager -> Interceptor If Verifier and
     * Interceptor are made the same class, we'll have a circular dependency chain
     */
    @Component
    public static class SonosJWTVerification implements VerificationCheck {
        @Autowired
        private SonosLinkDao sonosLinkDao;

        @Override
        public void check(DecodedJWT jwt) throws InsufficientAuthenticationException {
            AuthenticationType authenticationType = AuthenticationType.valueOf(jwt.getClaim(CLAIM_AUTHTYPE).asString());
            // no need to verify if anonymous because household ids and link codes don't exist
            if (authenticationType == AuthenticationType.ANONYMOUS) {
                return;
            }
            String linkcode = jwt.getClaim(CLAIM_LINKCODE).asString();
            SonosLink sonosLink = sonosLinkDao.findByLinkcode(linkcode);

            if (!StringUtils.equals(jwt.getSubject(), sonosLink.getUsername())
                    || !StringUtils.equals(linkcode, sonosLink.getLinkcode())
                    || !StringUtils.equals(jwt.getClaim(CLAIM_HOUSEHOLDID).asString(), sonosLink.getHouseholdId())) {
                throw new InsufficientAuthenticationException("Sonos creds not valid");
            }
        }
    }
}
