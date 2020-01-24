package org.airsonic.player.command;

import org.airsonic.player.domain.UserCredential;
import org.airsonic.player.security.GlobalSecurityConfig;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class CredentialsManagementCommand {
    private List<CredentialsCommand> credentials;
    private final Set<String> decodableEncoders = GlobalSecurityConfig.NONLEGACY_DECODABLE_ENCODERS;
    private final Set<String> nonDecodableEncoders = GlobalSecurityConfig.NONLEGACY_NONDECODABLE_ENCODERS;

    public CredentialsManagementCommand() {
    }

    public CredentialsManagementCommand(List<CredentialsCommand> credentials) {
        super();
        this.credentials = credentials;
    }

    public List<CredentialsCommand> getCredentials() {
        return credentials;
    }

    public void setCredentials(List<CredentialsCommand> credentials) {
        this.credentials = credentials;
    }

    public Set<String> getDecodableEncoders() {
        return decodableEncoders;
    }

    public Set<String> getNonDecodableEncoders() {
        return nonDecodableEncoders;
    }

    public static class CredentialsCommand {
        private String username;
        private String credential;
        private String confirmCredential;
        private String type;
        private String location;
        private Instant created;
        private Instant updated;
        @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        private Date expiration;
        private String comment;
        private Set<String> displayComments = new HashSet<>();
        private Boolean markedForDeletion = Boolean.FALSE;
        private String hash;

        public CredentialsCommand(String username, String type, String location, Instant created,
                Instant updated, Instant expiration, String comment, String hash) {
            this.username = username;
            this.type = type;
            this.location = location;
            this.created = created;
            this.updated = updated;
            this.expiration = Optional.ofNullable(expiration).map(e -> new Date(e.toEpochMilli())).orElse(null);
            this.comment = comment;
            this.hash = hash;
        }

        public CredentialsCommand() {
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getCredential() {
            return credential;
        }

        public void setCredential(String credential) {
            this.credential = credential;
        }

        public String getConfirmCredential() {
            return confirmCredential;
        }

        public void setConfirmCredential(String confirmCredential) {
            this.confirmCredential = confirmCredential;
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

        public Date getExpiration() {
            return expiration;
        }

        public Instant getExpirationInstant() {
            return Optional.ofNullable(expiration).map(e -> Instant.ofEpochMilli(e.getTime())).orElse(null);
        }

        public void setExpiration(Date expiration) {
            this.expiration = expiration;
        }

        public Set<String> getDisplayComments() {
            return displayComments;
        }

        public void addDisplayComment(String comment) {
            this.displayComments.add(comment);
        }

        public Boolean getMarkedForDeletion() {
            return markedForDeletion;
        }

        public void setMarkedForDeletion(Boolean markedForDeletion) {
            this.markedForDeletion = markedForDeletion;
        }

        public String getHash() {
            return hash;
        }

        public void setHash(String hash) {
            this.hash = hash;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public static CredentialsCommand fromUserCredential(UserCredential uc) {
            // do not copy credential itself and leave comments blank
            return new CredentialsCommand(uc.getLocationUsername(), uc.getType(), uc.getLocation(),
                    uc.getCreated(), uc.getUpdated(),
                    uc.getExpiration(),
                    uc.getComment(),
                    String.valueOf(uc.hashCode()));
        }
    }
}
