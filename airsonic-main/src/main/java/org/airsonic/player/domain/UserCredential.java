package org.airsonic.player.domain;

import java.time.Instant;

public class UserCredential {
    private String username;
    private String locationUsername;
    private String credential;
    private String type;
    private String location;
    private Instant created;
    private Instant updated;
    private Instant expiration;

    public UserCredential(String username, String locationUsername, String credential, String type, String location,
            Instant created, Instant updated, Instant expiration) {
        super();
        this.username = username;
        this.locationUsername = locationUsername;
        this.credential = credential;
        this.type = type;
        this.location = location;
        this.created = created;
        this.updated = updated;
        this.expiration = expiration;
    }

    public UserCredential(String username, String locationUsername, String credential, String type, String location) {
        this(username, locationUsername, credential, type, location, null, null, null);
        Instant now = Instant.now();
        setCreated(now);
        setUpdated(now);
    }

    public UserCredential(UserCredential uc) {
        this(uc.getUsername(), uc.getLocationUsername(), uc.getCredential(), uc.getType(), uc.getLocation(),
                uc.getCreated(), uc.getUpdated(), uc.getExpiration());
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getLocationUsername() {
        return locationUsername;
    }

    public void setLocationUsername(String locationUsername) {
        this.locationUsername = locationUsername;
    }

    public String getCredential() {
        return credential;
    }

    public void setCredential(String credential) {
        this.credential = credential;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
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
}
