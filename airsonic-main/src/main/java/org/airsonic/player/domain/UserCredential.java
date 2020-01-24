package org.airsonic.player.domain;

import java.time.Instant;

public class UserCredential {
    private String username;
    private String locationUsername;
    private String credential;
    private String type;
    private String location;
    private String comment;
    private Instant expiration;
    private Instant created;
    private Instant updated;

    public UserCredential(String username, String locationUsername, String credential, String type, String location,
            String comment, Instant expiration, Instant created, Instant updated) {
        super();
        this.username = username;
        this.locationUsername = locationUsername;
        this.credential = credential;
        this.type = type;
        this.location = location;
        this.comment = comment;
        this.expiration = expiration;
        this.created = created;
        this.updated = updated;
    }

    public UserCredential(String username, String locationUsername, String credential, String type, String location,
            String comment, Instant expiration) {
        this(username, locationUsername, credential, type, location, comment, expiration, null, null);
        Instant now = Instant.now();
        setCreated(now);
        setUpdated(now);
    }

    public UserCredential(String username, String locationUsername, String credential, String type, String location,
            String comment) {
        this(username, locationUsername, credential, type, location, comment, null);
    }

    public UserCredential(String username, String locationUsername, String credential, String type, String location) {
        this(username, locationUsername, credential, type, location, null);
    }

    public UserCredential(UserCredential uc) {
        this(uc.getUsername(), uc.getLocationUsername(), uc.getCredential(), uc.getType(), uc.getLocation(),
                uc.getComment(), uc.getExpiration(), uc.getCreated(), uc.getUpdated());
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((comment == null) ? 0 : comment.hashCode());
        result = prime * result + ((created == null) ? 0 : created.hashCode());
        result = prime * result + ((credential == null) ? 0 : credential.hashCode());
        result = prime * result + ((expiration == null) ? 0 : expiration.hashCode());
        result = prime * result + ((location == null) ? 0 : location.hashCode());
        result = prime * result + ((locationUsername == null) ? 0 : locationUsername.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((updated == null) ? 0 : updated.hashCode());
        result = prime * result + ((username == null) ? 0 : username.hashCode());
        return result;
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
        if (location == null) {
            if (other.location != null)
                return false;
        } else if (!location.equals(other.location))
            return false;
        if (locationUsername == null) {
            if (other.locationUsername != null)
                return false;
        } else if (!locationUsername.equals(other.locationUsername))
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
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
}
