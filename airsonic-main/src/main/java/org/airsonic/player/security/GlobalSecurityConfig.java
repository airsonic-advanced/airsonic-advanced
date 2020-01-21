package org.airsonic.player.security;

import org.airsonic.player.service.JWTSecurityService;
import org.airsonic.player.service.SettingsService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import static org.airsonic.player.security.MultipleCredsMatchingAuthenticationProvider.SALT_TOKEN_MECHANISM_SPECIALIZATION;

@Configuration
@Order(SecurityProperties.BASIC_AUTH_ORDER - 2)
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
public class GlobalSecurityConfig extends GlobalAuthenticationConfigurerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalSecurityConfig.class);

    static final String FAILURE_URL = "/login?error=1";

    static final String DEVELOPMENT_REMEMBER_ME_KEY = "airsonic";

    @Autowired
    private CsrfSecurityRequestMatcher csrfSecurityRequestMatcher;

    @Autowired
    SettingsService settingsService;

    @Autowired
    CustomUserDetailsContextMapper customUserDetailsContextMapper;

    @Autowired
    MultipleCredsMatchingAuthenticationProvider multipleCredsProvider;

    @SuppressWarnings("deprecation")
    @Bean
    public PasswordEncoder passwordEncoder() {
        String encodingId = "bcrypt";
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put(encodingId, new BCryptPasswordEncoder());
        encoders.put("ldap", new org.springframework.security.crypto.password.LdapShaPasswordEncoder());
        encoders.put("MD4", new org.springframework.security.crypto.password.Md4PasswordEncoder());
        encoders.put("MD5", new org.springframework.security.crypto.password.MessageDigestPasswordEncoder("MD5"));
        encoders.put("pbkdf2", new Pbkdf2PasswordEncoder());
        encoders.put("scrypt", new SCryptPasswordEncoder());
        encoders.put("SHA-1", new org.springframework.security.crypto.password.MessageDigestPasswordEncoder("SHA-1"));
        encoders.put("SHA-256", new org.springframework.security.crypto.password.MessageDigestPasswordEncoder("SHA-256"));
        encoders.put("sha256", new org.springframework.security.crypto.password.StandardPasswordEncoder());
        encoders.put("argon2", new Argon2PasswordEncoder());

        // base decodable encoders
        encoders.put("noop", org.springframework.security.crypto.password.NoOpPasswordEncoder.getInstance());
        encoders.put("hex", HexPasswordEncoder.getInstance());

        // base decodable encoders that rely on salt+token being passed in (not stored in db with this type)
        encoders.put("noop" + SALT_TOKEN_MECHANISM_SPECIALIZATION, new SaltedTokenPasswordEncoder(p -> p));
        encoders.put("hex" + SALT_TOKEN_MECHANISM_SPECIALIZATION, new SaltedTokenPasswordEncoder(HexPasswordEncoder.getInstance()));

        // TODO: legacy marked base encoders, to be upgraded to one-way formats at breaking version change
        encoders.put("legacynoop", org.springframework.security.crypto.password.NoOpPasswordEncoder.getInstance());
        encoders.put("legacyhex", HexPasswordEncoder.getInstance());

        encoders.put("legacynoop" + SALT_TOKEN_MECHANISM_SPECIALIZATION, new SaltedTokenPasswordEncoder(p -> p));
        encoders.put("legacyhex" + SALT_TOKEN_MECHANISM_SPECIALIZATION, new SaltedTokenPasswordEncoder(HexPasswordEncoder.getInstance()));

        return new DelegatingPasswordEncoder(encodingId, encoders);
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        if (settingsService.isLdapEnabled()) {
            auth.ldapAuthentication()
                    .contextSource()
                        .managerDn(settingsService.getLdapManagerDn())
                        .managerPassword(settingsService.getLdapManagerPassword())
                        .url(settingsService.getLdapUrl())
                    .and()
                    .userSearchFilter(settingsService.getLdapSearchFilter())
                    .userDetailsContextMapper(customUserDetailsContextMapper);
        }
        // auth.userDetailsService(securityService);
        String jwtKey = settingsService.getJWTKey();
        if (StringUtils.isBlank(jwtKey)) {
            LOG.warn("Generating new jwt key");
            jwtKey = JWTSecurityService.generateKey();
            settingsService.setJWTKey(jwtKey);
            settingsService.save();
        }
        auth.authenticationProvider(new JWTAuthenticationProvider(jwtKey));
        auth.authenticationProvider(multipleCredsProvider);
    }

    private static String generateRememberMeKey() {
        byte[] array = new byte[32];
        new SecureRandom().nextBytes(array);
        return new String(array);
    }

    @Configuration
    @Order(1)
    public class ExtSecurityConfiguration extends WebSecurityConfigurerAdapter {

        public ExtSecurityConfiguration() {
            super(true);
        }

        @Bean(name = "jwtAuthenticationFilter")
        public JWTRequestParameterProcessingFilter jwtAuthFilter() throws Exception {
            return new JWTRequestParameterProcessingFilter(authenticationManager(), FAILURE_URL);
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {

            http = http.addFilter(new WebAsyncManagerIntegrationFilter());
            http = http.addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class);

            http
                    .antMatcher("/ext/**")
                    .csrf().requireCsrfProtectionMatcher(csrfSecurityRequestMatcher).and()
                    .headers().frameOptions().sameOrigin().and()
                    .authorizeRequests()
                    .antMatchers("/ext/stream/**", "/ext/coverArt*", "/ext/share/**", "/ext/hls/**")
                    .hasAnyRole("TEMP", "USER").and()
                    .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                    .exceptionHandling().and()
                    .securityContext().and()
                    .requestCache().and()
                    .anonymous().and()
                    .servletApi();
        }
    }

    @Configuration
    @Order(2)
    public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {

            RESTRequestParameterProcessingFilter restAuthenticationFilter = new RESTRequestParameterProcessingFilter();
            restAuthenticationFilter.setAuthenticationManager(authenticationManagerBean());
//            restAuthenticationFilter.setSecurityService(securityService);
//            restAuthenticationFilter.setEventPublisher(eventPublisher);
            http = http.addFilterBefore(restAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

            // Try to load the 'remember me' key.
            //
            // Note that using a fixed key compromises security as perfect
            // forward secrecy is not guaranteed anymore.
            //
            // An external entity can then re-use our authentication cookies before
            // the expiration time, or even, given enough time, recover the password
            // from the MD5 hash.
            //
            // See: https://docs.spring.io/spring-security/site/docs/3.0.x/reference/remember-me.html

            String rememberMeKey = settingsService.getRememberMeKey();
            boolean development = SettingsService.isDevelopmentMode();
            if (StringUtils.isBlank(rememberMeKey) && !development) {
                // ...if it is empty, generate a random key on startup (default).
                LOG.debug("Generating a new ephemeral 'remember me' key in a secure way.");
                rememberMeKey = generateRememberMeKey();
            } else if (StringUtils.isBlank(rememberMeKey) && development) {
                // ...if we are in development mode, we can use a fixed key.
                LOG.warn("Using a fixed 'remember me' key because we're in development mode, this is INSECURE.");
                rememberMeKey = DEVELOPMENT_REMEMBER_ME_KEY;
            } else {
                // ...otherwise, use the custom key directly.
                LOG.info("Using a fixed 'remember me' key from system properties, this is insecure.");
            }

            http
                    .csrf()
                    .requireCsrfProtectionMatcher(csrfSecurityRequestMatcher)
                    .and().headers()
                    .frameOptions()
                    .sameOrigin()
                    .and().authorizeRequests()
                    .antMatchers("/recover*", "/accessDenied*",
                            "/style/**", "/icons/**", "/flash/**", "/script/**",
                            "/sonos/**", "/login", "/error")
                    .permitAll()
                    .antMatchers("/personalSettings*", "/passwordSettings*",
                            "/playerSettings*", "/shareSettings*", "/passwordSettings*")
                    .hasRole("SETTINGS")
                    .antMatchers("/generalSettings*", "/advancedSettings*", "/userSettings*",
                            "/musicFolderSettings*", "/databaseSettings*", "/transcodeSettings*", "/rest/startScan*")
                    .hasRole("ADMIN")
                    .antMatchers("/deletePlaylist*", "/savePlaylist*")
                    .hasRole("PLAYLIST")
                    .antMatchers("/download*")
                    .hasRole("DOWNLOAD")
                    .antMatchers("/upload*")
                    .hasRole("UPLOAD")
                    .antMatchers("/createShare*")
                    .hasRole("SHARE")
                    .antMatchers("/changeCoverArt*", "/editTags*")
                    .hasRole("COVERART")
                    .antMatchers("/setMusicFileInfo*")
                    .hasRole("COMMENT")
                    .antMatchers("/podcastReceiverAdmin*")
                    .hasRole("PODCAST")
                    .antMatchers("/**")
                    .hasRole("USER")
                    .anyRequest().authenticated()
                    .and().formLogin()
                    .loginPage("/login")
                    .permitAll()
                    .defaultSuccessUrl("/index", true)
                    .failureUrl(FAILURE_URL)
                    .usernameParameter("j_username")
                    .passwordParameter("j_password")
                    // see http://docs.spring.io/spring-security/site/docs/3.2.4.RELEASE/reference/htmlsingle/#csrf-logout
                    .and().logout().logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET")).logoutSuccessUrl(
                    "/login?logout")
                    .and().rememberMe().key(rememberMeKey);
        }

    }
}
