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

import org.airsonic.player.dao.UserDao;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.SonosLink;
import org.airsonic.player.domain.User;
import org.airsonic.player.domain.UserCredential;
import org.airsonic.player.domain.UserCredential.App;
import org.airsonic.player.security.GlobalSecurityConfig;
import org.airsonic.player.security.PasswordDecoder;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.airsonic.player.service.sonos.SonosSoapFault;
import org.airsonic.player.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.UUID;

import static org.airsonic.player.domain.User.USERNAME_SONOS;

/**
 * Provides security-related services for authentication and authorization.
 *
 * @author Sindre Mehus
 */
@Service
@CacheConfig(cacheNames = "userCache")
public class SecurityService implements UserDetailsService {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityService.class);

    @Autowired
    private UserDao userDao;

    @Autowired
    private SonosLinkDao sonosLinkDao;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private JWTSecurityService jwtSecurityService;

    /*
        TODO This is initialized in GlobalSecurityConfig.

        Something wrong here, some circular ref, maybe rebuild responsibilities...

        We are :
           - RESTRequestParameterProcessingFilter need SecurityService cause is UserDetailsService.
           - But SecurityService need AuthenticationManager they create in GlobalSecurityConfig
           - But GlobalSecurityConfig create RESTRequestParameterProcessingFilter

           So GlobalSecurityConfig -> RESTRequestParameterProcessingFilter -> SecurityService -> AuthenticationManager (but create in GlobalSecurityConfig)
     */
    private AuthenticationManager authenticationManager;


    /**
     * Locates the user based on the username.
     *
     * @param username The username
     * @return A fully populated user record (never <code>null</code>)
     * @throws UsernameNotFoundException if the user could not be found or the user has no GrantedAuthority.
     * @throws DataAccessException       If user could not be found for a repository-specific reason.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException, DataAccessException {
        return loadUserByUsername(username, true);
    }

    public UserDetails loadUserByUsername(String username, boolean caseSensitive)
            throws UsernameNotFoundException, DataAccessException {
        User user = getUserByName(username, caseSensitive);
        if (user == null) {
            throw new UsernameNotFoundException("User \"" + username + "\" was not found.");
        }

        List<GrantedAuthority> authorities = getGrantedAuthorities(user);

        return new UserDetail(
                username,
                getCredentials(user.getUsername(), App.AIRSONIC),
                true,
                true,
                true,
                true,
                authorities);
    }

    public boolean updateCredentials(UserCredential oldCreds, UserCredential newCreds, String comment,
            boolean reencodePlaintextNewCreds) {
        if (!StringUtils.equals(newCreds.getEncoder(), oldCreds.getEncoder()) || reencodePlaintextNewCreds) {
            if (reencodePlaintextNewCreds) {
                newCreds.setCredential(GlobalSecurityConfig.ENCODERS.get(newCreds.getEncoder()).encode(newCreds.getCredential()));
            } else if (GlobalSecurityConfig.DECODABLE_ENCODERS.contains(oldCreds.getEncoder())) {
                try {
                    // decode using original creds decoder
                    PasswordDecoder decoder = (PasswordDecoder) GlobalSecurityConfig.ENCODERS.get(oldCreds.getEncoder());
                    newCreds.setCredential(decoder.decode(oldCreds.getCredential()));
                    // reencode
                    newCreds.setCredential(GlobalSecurityConfig.ENCODERS.get(newCreds.getEncoder()).encode(newCreds.getCredential()));
                } catch (Exception e) {
                    LOG.warn("Could not update credentials for user {}", oldCreds.getUsername(), e);
                    // Do not try and save it
                    return false;
                }
            }
        }

        if (!newCreds.equals(oldCreds)) {
            newCreds.setComment(comment);
            newCreds.setUpdated(Instant.now());

            return userDao.updateCredential(oldCreds, newCreds);
        }

        return true;
    }

    public boolean createCredential(UserCredential newCreds) {
        newCreds.setCredential(GlobalSecurityConfig.ENCODERS.get(newCreds.getEncoder()).encode(newCreds.getCredential()));
        return userDao.createCredential(newCreds);
    }

    // ensure we can't delete all airsonic creds
    private Predicate<UserCredential> retainOneAirsonicCred = c ->
            App.AIRSONIC != c.getApp()
            || !userDao.getCredentials(c.getUsername(), c.getApp()).isEmpty();

    public boolean deleteCredential(UserCredential creds) {
        try {
            return userDao.deleteCredential(creds, retainOneAirsonicCred);
        } catch (Exception e) {
            LOG.info("Can't delete a credential", e);
            return false;
        }
    }

    public List<UserCredential> getCredentials(String username, App... apps) {
        return userDao.getCredentials(username, apps);
    }

    public Map<App, UserCredential> getDecodableCredsForApps(String username, App... apps) {
        return getCredentials(username, apps).parallelStream()
                .filter(c -> GlobalSecurityConfig.DECODABLE_ENCODERS.contains(c.getEncoder()))
                .filter(c -> c.getExpiration() == null || c.getExpiration().isAfter(Instant.now()))
                .collect(Collectors.groupingByConcurrent(
                        UserCredential::getApp,
                        Collectors.collectingAndThen(
                                Collectors.maxBy(Comparator.comparing(c -> c.getUpdated())), o -> o.orElse(null))));
    }

    public boolean checkDefaultAdminCredsPresent() {
        return userDao.getCredentials(User.USERNAME_ADMIN, App.AIRSONIC).parallelStream()
                .anyMatch(c -> GlobalSecurityConfig.ENCODERS.get(c.getEncoder()).matches(User.USERNAME_ADMIN, c.getCredential()));
    }

    public boolean checkOpenCredsPresent() {
        return GlobalSecurityConfig.OPENTEXT_ENCODERS.parallelStream()
                .mapToInt(userDao::getCredentialCountByEncoder)
                .anyMatch(i -> i > 0);
    }

    public boolean checkLegacyCredsPresent() {
        return userDao.getCredentialCountByEncoder("legacy%") != 0;
    }

    public boolean migrateLegacyCredsToNonLegacy(boolean useDecodableOnly) {
        String decodableEncoder = settingsService.getDecodablePasswordEncoder();
        String nonDecodableEncoder = useDecodableOnly ? decodableEncoder
                : settingsService.getNonDecodablePasswordEncoder();

        List<UserCredential> failures = new ArrayList<>();

        userDao.getCredentialsByEncoder("legacy%").forEach(c -> {
            UserCredential newCreds = new UserCredential(c);
            if (App.AIRSONIC == c.getApp()) {
                newCreds.setEncoder(nonDecodableEncoder);
            } else {
                newCreds.setEncoder(decodableEncoder);
            }
            if (!updateCredentials(c, newCreds, c.getComment() + " | Migrated to nonlegacy by admin", false)) {
                LOG.warn("System failed to migrate creds created on {} for user {}", c.getCreated(), c.getUsername());
                failures.add(c);
            }
        });

        return failures.isEmpty();
    }

    public String getPreferredPasswordEncoder(boolean nonDecodableAllowed) {
        if (!nonDecodableAllowed || !settingsService.getPreferNonDecodablePasswords()) {
            return settingsService.getDecodablePasswordEncoder();
        } else {
            return settingsService.getNonDecodablePasswordEncoder();
        }
    }

    public List<GrantedAuthority> getGrantedAuthorities(User user) {
        return Stream.concat(
                Stream.of(
                        new SimpleGrantedAuthority("IS_AUTHENTICATED_ANONYMOUSLY"),
                        new SimpleGrantedAuthority("ROLE_USER")),
                user.getRoles().stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role.name())))
                .collect(Collectors.toList());
    }

    /**
     * Returns the currently logged-in user for the given HTTP request.
     *
     * @param request The HTTP request.
     * @return The logged-in user, or <code>null</code>.
     */
    public User getCurrentUser(HttpServletRequest request) {
        String username = getCurrentUsername(request);
        return username == null ? null : getUserByName(username);
    }

    public static String getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.User) {
            return ((org.springframework.security.core.userdetails.User)authentication.getPrincipal()).getUsername();
        }

        return null;
    }


    /**
     * Returns the name of the currently logged-in user.
     *
     * @param request The HTTP request.
     * @return The name of the logged-in user, or <code>null</code>.
     */
    public String getCurrentUsername(HttpServletRequest request) {
        return new SecurityContextHolderAwareRequestWrapper(request, null).getRemoteUser();
    }

    /**
     * Returns the user with the given username.
     *
     * @param username The username used when logging in.
     * @return The user, or <code>null</code> if not found.
     */
    public User getUserByName(String username) {
        return getUserByName(username, true);
    }

    /**
     * Returns the user with the given username<br>
     * Cache note: Will only cache if case-sensitive. Otherwise, cache eviction is difficult
     *
     * @param username      The username to look for
     * @param caseSensitive If false, will do a case insensitive search
     * @return The corresponding User
     */
    @Cacheable(key = "#username", condition = "#caseSensitive", unless = "#result == null")
    public User getUserByName(String username, boolean caseSensitive) {
        return userDao.getUserByName(username, caseSensitive);
    }

    /**
     * Returns the user with the given email address.
     *
     * @param email The email address.
     * @return The user, or <code>null</code> if not found.
     */
    public User getUserByEmail(String email) {
        return userDao.getUserByEmail(email);
    }

    /**
     * Returns all users.
     *
     * @return Possibly empty array of all users.
     */
    public List<User> getAllUsers() {
        return userDao.getAllUsers();
    }

    /**
     * Returns whether the given user has administrative rights.
     */
    public boolean isAdmin(String username) {
        if (User.USERNAME_ADMIN.equals(username)) {
            return true;
        }
        User user = getUserByName(username);
        return user != null && user.isAdminRole();
    }

    /**
     * Creates a new user.
     *
     * @param user       The user to create.
     * @param credential The raw credential (will be encoded)
     */
    public void createUser(User user, String credential, String comment) {
        String defaultEncoder = getPreferredPasswordEncoder(true);
        UserCredential uc = new UserCredential(
                user.getUsername(),
                user.getUsername(),
                GlobalSecurityConfig.ENCODERS.get(defaultEncoder).encode(credential),
                defaultEncoder,
                App.AIRSONIC,
                comment);
        createUser(user, uc);
    }

    public void createUser(User user, UserCredential credential) {
        userDao.createUser(user, credential);
        settingsService.setMusicFoldersForUser(user.getUsername(), MusicFolder.toIdList(settingsService.getAllMusicFolders()));
        LOG.info("Created user " + user.getUsername());
    }

    /**
     * Deletes the user with the given username.
     *
     * @param username The username.
     */
    @CacheEvict
    public void deleteUser(String username) {
        userDao.deleteUser(username);
        LOG.info("Deleted user " + username);
    }

    /**
     * Updates the given user.
     *
     * @param user The user to update.
     */
    @CacheEvict(key = "#user.username")
    public void updateUser(User user) {
        userDao.updateUser(user);
    }

    /**
     * Updates the byte counts for given user.
     *
     * @param user                 The user to update, may be <code>null</code>.
     * @param bytesStreamedDelta   Increment bytes streamed count with this value.
     * @param bytesDownloadedDelta Increment bytes downloaded count with this value.
     * @param bytesUploadedDelta   Increment bytes uploaded count with this value.
     */
    @CacheEvict(key = "#user.username")
    public void updateUserByteCounts(User user, long bytesStreamedDelta, long bytesDownloadedDelta, long bytesUploadedDelta) {
        if (user == null) {
            return;
        }

        userDao.updateUserByteCounts(user.getUsername(), bytesStreamedDelta, bytesDownloadedDelta, bytesUploadedDelta);

        User updated = userDao.getUserByName(user.getUsername(), true);
        user.setBytesStreamed(updated.getBytesStreamed());
        user.setBytesDownloaded(updated.getBytesDownloaded());
        user.setBytesUploaded(updated.getBytesUploaded());
    }

    /**
     * Returns whether the given file may be read.
     *
     * @return Whether the given file may be read.
     */
    public boolean isReadAllowed(Path file) {
        // Allowed to read from both music folder and podcast folder.
        return isInMusicFolder(file) || isInPodcastFolder(file);
    }

    /**
     * Returns whether the given file may be written, created or deleted.
     *
     * @return Whether the given file may be written, created or deleted.
     */
    public boolean isWriteAllowed(Path file) {
        // Only allowed to write podcasts or cover art.
        boolean isPodcast = isInPodcastFolder(file);
        boolean isCoverArt = isInMusicFolder(file) && file.getFileName().toString().startsWith("cover.");

        return isPodcast || isCoverArt;
    }

    /**
     * Returns whether the given file may be uploaded.
     *
     * @return Whether the given file may be uploaded.
     */
    public void checkUploadAllowed(Path file, boolean checkFileExists) throws IOException {
        if (!isInMusicFolder(file)) {
            throw new AccessDeniedException(file.toString(), null, "Specified location is not in writable music folder");
        }

        if (checkFileExists && Files.exists(file)) {
            throw new FileAlreadyExistsException(file.toString(), null, "File already exists");
        }
    }

    /**
     * Returns whether the given file is located in one of the music folders (or any of their sub-folders).
     *
     * @param file The file in question.
     * @return Whether the given file is located in one of the music folders.
     */
    private boolean isInMusicFolder(Path file) {
        return getMusicFolderForFile(file) != null;
    }

    private MusicFolder getMusicFolderForFile(Path file) {
        String path = file.toString();
        return settingsService.getAllMusicFolders(false, true).stream().filter(folder -> isFileInFolder(path, folder.getPath().toString())).findFirst().orElse(null);
    }

    /**
     * Returns whether the given file is located in the Podcast folder (or any of its sub-folders).
     *
     * @param file The file in question.
     * @return Whether the given file is located in the Podcast folder.
     */
    private boolean isInPodcastFolder(Path file) {
        String podcastFolder = settingsService.getPodcastFolder();
        return isFileInFolder(file.toString(), podcastFolder);
    }

    public String getRootFolderForFile(Path file) {
        MusicFolder folder = getMusicFolderForFile(file);
        if (folder != null) {
            return folder.getPath().toString();
        }

        if (isInPodcastFolder(file)) {
            return settingsService.getPodcastFolder();
        }
        return null;
    }

    public boolean isFolderAccessAllowed(MediaFile file, String username) {
        if (isInPodcastFolder(file.getFile())) {
            return true;
        }

        return settingsService.getMusicFoldersForUser(username).parallelStream().anyMatch(musicFolder -> musicFolder.getPath().toString().equals(file.getFolder()));
    }

    /**
     * Returns whether the given file is located in the given folder (or any of its sub-folders).
     * If the given file contains the expression ".." (indicating a reference to the parent directory),
     * this method will return <code>false</code>.
     *
     * @param file   The file in question.
     * @param folder The folder in question.
     * @return Whether the given file is located in the given folder.
     */
    protected static boolean isFileInFolder(String file, String folder) {
        // Deny access if file contains ".." surrounded by slashes (or end of line).
        if (file.matches(".*(/|\\\\)\\.\\.(/|\\\\|$).*")) {
            return false;
        }

        // Convert slashes.
        file = file.replace('\\', '/');
        folder = folder.replace('\\', '/');

        return
                // identity matches
                // /a/ == /a, /a == /a/, /a == /a, /a/ == /a/
                StringUtils.equalsIgnoreCase(file, folder)
                || StringUtils.equalsIgnoreCase(file, StringUtils.appendIfMissing(folder, "/"))
                || StringUtils.equalsIgnoreCase(StringUtils.appendIfMissing(file, "/"), folder)
                || StringUtils.equalsIgnoreCase(StringUtils.appendIfMissing(file, "/"), StringUtils.appendIfMissing(folder, "/"))
                // file prefix is folder (MUST append '/', otherwise /a/b2 startswith /a/b)
                || StringUtils.startsWithIgnoreCase(file, StringUtils.appendIfMissing(folder, "/"));
    }


    // =======================================================================================================
    // Utilities for Sonos link.

    public Authentication authenticate(String username, String password) {
        UsernamePasswordAuthenticationToken authReq = new UsernamePasswordAuthenticationToken(username, password);
        return authenticationManager.authenticate(authReq);
    }

    public void authenticate(String sonosLinkToken) throws SonosSoapFault.LoginUnauthorized {
        SonosLink sonosLink = jwtSecurityService.verifySonosLink(sonosLinkToken);

        SonosLink saved = sonosLinkDao.findByLinkcode(sonosLink.getLinkcode());
        if (saved != null && saved.identical(sonosLink)) {
            setUser(sonosLink.getUsername());
        } else {
            throw new SonosSoapFault.LoginUnauthorized();
        }
    }

    public void authenticate() throws SonosSoapFault.LoginUnauthorized {
        setUser(USERNAME_SONOS);
    }


    /**
     * Generate a link code, and put in the cache for future use.
     *
     * @param householdId from sonos, represent a user on sonos

     * @return The link code.
     */
    public String generateLinkCode(String householdId) {
        String linkCode = createLinkCode();

        sonosLinkcodeCache.put(new Element(linkCode, householdId));

        return linkCode;
    }

    public static class UserDetail extends org.springframework.security.core.userdetails.User {
        private List<UserCredential> creds;

        public UserDetail(String username, List<UserCredential> creds, boolean enabled, boolean accountNonExpired,
                boolean credentialsNonExpired, boolean accountNonLocked,
                Collection<? extends GrantedAuthority> authorities) {
            super(username,
                    DigestUtils.md5Hex(creds.stream().map(x -> x.getEncoder() + "/" + x.getCredential() + "/" + x.getExpiration()).collect(Collectors.joining())),
                    enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);

            this.creds = creds;
        }

        public List<UserCredential> getCredentials() {
            return creds;
        }

        @Override
        public void eraseCredentials() {
            super.eraseCredentials();
            creds = null;
        }

    public String getHousehold(String linkCode) {
        Element element = sonosLinkcodeCache.get(linkCode);

        if (element != null) {
            return (String) element.getValue();
        }

        return  null;
    }

    /**
     * Insert the authorisation in sonoslink. Verify is the linkcode exist, and cannot be get again.
     *
     * @param username The username authorisation for sonos link
     * @param householdId The id of entry in sonos controller
     * @param linkcode The link code used between sonos and airsonic.
     * @return true if the insert is ok, false if some entry exist with the linkcode
     */
    public boolean authoriseSonos(String username, String householdId, String linkcode) {
        if (sonosLinkDao.findByLinkcode(linkcode) != null) {
            return false;
        }

        sonosLinkDao.create(new SonosLink(username, householdId, linkcode));
        return true;
    }

    /**
     * Find the user they have a linkCode for the householdId set, build the authToken and return it.
     *
     * @param householdId The householdId from Sonos
     * @param linkCode The linkCode return it before
     * @return The build authToken or null if didn't find any user with householdId and linkCode
     */
    public String getSonosAuthToken(String householdId, String linkCode) {

        SonosLink sonosLink = sonosLinkDao.findByLinkcode(linkCode);
        if (sonosLink != null && householdId.equals(sonosLink.getHouseholdid())) {
            return buildToken(sonosLink);
        } else {
            return null;
        }
    }

    private String buildToken(SonosLink link) {
        return jwtSecurityService.createSonosToken(link.getUsername(), link.getHouseholdid(), link.getLinkcode());
    }

    public SonosLink getSonosLink(String linkCode) {
        return sonosLinkDao.findByLinkcode(linkCode);
    }

    private void setUser(String username) throws SonosSoapFault.LoginUnauthorized {
        User user = getUserByName(username, true);
        Authentication authentication = authenticate(user.getUsername(), user.getPassword());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

        // The link code must be exactly 32 characters long
    private String createLinkCode() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public void setAuthenticationManager(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

}
