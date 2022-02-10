package org.airsonic.player.domain;

import java.time.Instant;
import java.util.Objects;

public class UserCredential {
    private String username;
    private String appUsername;
    private String credential;
    private String encoder;
    private App app;
    private String comment;
    private Instant expiration;
    private Instant created;
    private Instant updated;

    public UserCredential(String username, String appUsername, String credential, String encoder, App app,
            String comment, Instant expiration, Instant created, Instant updated) {
        super();
        this.username = username;
        this.appUsername = appUsername;
        this.credential = credential;
        this.encoder = encoder;
        this.app = app;
        this.comment = comment;
        this.expiration = expiration;
        this.created = created;
        this.updated = updated;
    }

    public UserCredential(String username, String appUsername, String credential, String encoder, App app,
            String comment, Instant expiration) {
        this(username, appUsername, credential, encoder, app, comment, expiration, null, null);
        Instant now = Instant.now();
        setCreated(now);
        setUpdated(now);
    }

    public UserCredential(String username, String appUsername, String credential, String encoder, App app,
            String comment) {
        this(username, appUsername, credential, encoder, app, comment, null);
    }

    public UserCredential(String username, String appUsername, String credential, String encoder, App app) {
        this(username, appUsername, credential, encoder, app, null);
    }

    public UserCredential(UserCredential uc) {
        this(uc.getUsername(), uc.getAppUsername(), uc.getCredential(), uc.getEncoder(), uc.getApp(),
                uc.getComment(), uc.getExpiration(), uc.getCreated(), uc.getUpdated());
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAppUsername() {
        return appUsername;
    }

    public void setAppUsername(String appUsername) {
        this.appUsername = appUsername;
    }

    public String getCredential() {
        return credential;
    }

    public void setCredential(String credential) {
        this.credential = credential;
    }

    public String getEncoder() {
        return encoder;
    }

    public void setEncoder(String encoder) {
        this.encoder = encoder;
    }

    public App getApp() {
        return app;
    }

    public void setApp(App app) {
        this.app = app;
    }

    public Instant getCreated() {
        return created;
    }

    public void setCreated(Instant created) {
        this.created = created;
    }

    public Instant getUpdated() {
        return updated;
    }

    public void setUpdated(Instant updated) {
        this.updated = updated;
    }

    public Instant getExpiration() {
        return expiration;
    }

    public void setExpiration(Instant expiration) {
        this.expiration = expiration;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public int hashCode() {
        return Objects.hash(comment, created, credential, expiration, app, appUsername, encoder, updated, username);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        UserCredential other = (UserCredential) obj;
        if (comment == null) {
            if (other.comment != null)
                return false;
        } else if (!comment.equals(other.comment))
            return false;
        if (created == null) {
            if (other.created != null)
                return false;
        } else if (!created.equals(other.created))
            return false;
        if (credential == null) {
            if (other.credential != null)
                return false;
        } else if (!credential.equals(other.credential))
            return false;
        if (expiration == null) {
            if (other.expiration != null)
                return false;
        } else if (!expiration.equals(other.expiration))
            return false;
        if (app == null) {
            if (other.app != null)
                return false;
        } else if (!app.equals(other.app))
            return false;
        if (appUsername == null) {
            if (other.appUsername != null)
                return false;
        } else if (!appUsername.equals(other.appUsername))
            return false;
        if (encoder == null) {
            if (other.encoder != null)
                return false;
        } else if (!encoder.equals(other.encoder))
            return false;
        if (updated == null) {
            if (other.updated != null)
                return false;
        } else if (!updated.equals(other.updated))
            return false;
        if (username == null) {
            if (other.username != null)
                return false;
        } else if (!username.equals(other.username))
            return false;
        return true;
    }

    public enum App {
        AIRSONIC("Airsonic", false, true),
        LASTFM("Last.fm", true, false),
        LISTENBRAINZ("Listenbrainz", false, false),
        PODCASTINDEX("Podcast Index", true, false);

        private final String name;
        private final boolean usernameRequired;
        private final boolean nonDecodableEncodersAllowed;

        private App(String name, boolean usernameRequired, boolean nonDecodableEncodersAllowed) {
            this.name = name;
            this.usernameRequired = usernameRequired;
            this.nonDecodableEncodersAllowed = nonDecodableEncodersAllowed;
        }

        public String getName() {
            return name;
        }

        public boolean getUsernameRequired() {
            return usernameRequired;
        }

        public boolean getNonDecodableEncodersAllowed() {
            return nonDecodableEncodersAllowed;
        }

    }
}
