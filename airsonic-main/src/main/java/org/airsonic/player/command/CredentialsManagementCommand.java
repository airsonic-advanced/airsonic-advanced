package org.airsonic.player.command;

import org.airsonic.player.domain.UserCredential;
import org.airsonic.player.domain.UserCredential.App;
import org.airsonic.player.validator.CredentialsManagementValidators.ConsistentPasswordConfirmation;
import org.airsonic.player.validator.CredentialsManagementValidators.CredTypeForLocationValid;
import org.airsonic.player.validator.CredentialsManagementValidators.CredTypeValid;
import org.airsonic.player.validator.CredentialsManagementValidators.CredentialCreateChecks;
import org.airsonic.player.validator.CredentialsManagementValidators.CredentialUpdateChecks;
import org.airsonic.player.validator.CredentialsManagementValidators.EncoderTypeValid;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class CredentialsManagementCommand {
    @Valid
    private List<CredentialsCommand> credentials;

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

    @CredTypeForLocationValid(groups = CredentialCreateChecks.class)
    @ConsistentPasswordConfirmation
    public static class CredentialsCommand {
        @NotBlank(groups = CredentialCreateChecks.class)
        private String username;

        @NotBlank(groups = CredentialCreateChecks.class)
        private String credential;

        @NotBlank(groups = CredentialCreateChecks.class)
        private String confirmCredential;

        @NotNull(groups = CredentialCreateChecks.class)
        private App location;

        @NotBlank
        @CredTypeValid
        private String type;

        @NotBlank(groups = CredentialUpdateChecks.class)
        private String hash;

        @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        private Date expiration;

        private Instant created;
        private Instant updated;
        private String comment;
        private Set<String> displayComments = new HashSet<>();
        private boolean markedForDeletion;

        public CredentialsCommand(String username, String type, App location, Instant created,
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

        public App getLocation() {
            return location;
        }

        public void setLocation(App location) {
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

        public boolean getMarkedForDeletion() {
            return markedForDeletion;
        }

        public void setMarkedForDeletion(boolean markedForDeletion) {
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

    public static class AppCredSettings {
        private final boolean usernameRequired;
        private final boolean nonDecodableEncodersAllowed;

        public AppCredSettings(boolean usernameRequired, boolean nonDecodableEncodersAllowed) {
            this.usernameRequired = usernameRequired;
            this.nonDecodableEncodersAllowed = nonDecodableEncodersAllowed;
        }

        public boolean getUsernameRequired() {
            return usernameRequired;
        }

        public boolean getNonDecodableEncodersAllowed() {
            return nonDecodableEncodersAllowed;
        }
    }

    public static class AdminControls {
        private boolean credsStoredInLegacyTables;
        private boolean legacyCredsPresent;
        private boolean openCredsPresent;
        private boolean defaultAdminCredsPresent;

        private String jwtKey;
        private String encryptionKey;
        private String encryptionKeySalt;
        @EncoderTypeValid(decodable = false)
        private String nonDecodableEncoder;
        @EncoderTypeValid(decodable = true)
        private String decodableEncoder;
        private boolean preferNonDecodable;

        private boolean jwtKeyChanged;
        private boolean encryptionKeyChanged;
        private boolean nonDecodableEncoderChanged;
        private boolean decodableEncoderChanged;
        private boolean nonDecodablePreferenceChanged;

        private boolean purgeCredsInLegacyTables;
        private boolean migrateLegacyCredsToNonLegacyDefault;
        private boolean migrateLegacyCredsToNonLegacyDecodableOnly;

        public AdminControls() {
        }

        public AdminControls(boolean credsStoredInLegacyTables, boolean legacyCredsPresent, boolean openCredsPresent,
                boolean defaultAdminCredsPresent, String jwtKey, String encryptionKey, String encryptionKeySalt,
                String nonDecodableEncoder, String decodableEncoder, boolean preferNonDecodable) {
            super();
            this.credsStoredInLegacyTables = credsStoredInLegacyTables;
            this.legacyCredsPresent = legacyCredsPresent;
            this.openCredsPresent = openCredsPresent;
            this.defaultAdminCredsPresent = defaultAdminCredsPresent;
            this.jwtKey = jwtKey;
            this.encryptionKey = encryptionKey;
            this.encryptionKeySalt = encryptionKeySalt;
            this.nonDecodableEncoder = nonDecodableEncoder;
            this.decodableEncoder = decodableEncoder;
            this.preferNonDecodable = preferNonDecodable;
        }

        public boolean getCredsStoredInLegacyTables() {
            return credsStoredInLegacyTables;
        }

        public void setCredsStoredInLegacyTables(boolean credsStoredInLegacyTables) {
            this.credsStoredInLegacyTables = credsStoredInLegacyTables;
        }

        public boolean getLegacyCredsPresent() {
            return legacyCredsPresent;
        }

        public void setLegacyCredsPresent(boolean legacyCredsPresent) {
            this.legacyCredsPresent = legacyCredsPresent;
        }

        public boolean getOpenCredsPresent() {
            return openCredsPresent;
        }

        public void setOpenCredsPresent(boolean openCredsPresent) {
            this.openCredsPresent = openCredsPresent;
        }

        public boolean getDefaultAdminCredsPresent() {
            return defaultAdminCredsPresent;
        }

        public void setDefaultAdminCredsPresent(boolean defaultAdminCredsPresent) {
            this.defaultAdminCredsPresent = defaultAdminCredsPresent;
        }

        public String getJwtKey() {
            return jwtKey;
        }

        public void setJwtKey(String jwtKey) {
            this.jwtKey = jwtKey;
        }

        public String getEncryptionKey() {
            return encryptionKey;
        }

        public void setEncryptionKey(String encryptionKey) {
            this.encryptionKey = encryptionKey;
        }

        public String getEncryptionKeySalt() {
            return encryptionKeySalt;
        }

        public void setEncryptionKeySalt(String encryptionKeySalt) {
            this.encryptionKeySalt = encryptionKeySalt;
        }

        public String getNonDecodableEncoder() {
            return nonDecodableEncoder;
        }

        public void setNonDecodableEncoder(String nonDecodableEncoder) {
            this.nonDecodableEncoder = nonDecodableEncoder;
        }

        public String getDecodableEncoder() {
            return decodableEncoder;
        }

        public void setDecodableEncoder(String decodableEncoder) {
            this.decodableEncoder = decodableEncoder;
        }

        public boolean getPreferNonDecodable() {
            return preferNonDecodable;
        }

        public void setPreferNonDecodable(boolean preferNonDecodable) {
            this.preferNonDecodable = preferNonDecodable;
        }

        public boolean getJwtKeyChanged() {
            return jwtKeyChanged;
        }

        public void setJwtKeyChanged(boolean jwtKeyChanged) {
            this.jwtKeyChanged = jwtKeyChanged;
        }

        public boolean getEncryptionKeyChanged() {
            return encryptionKeyChanged;
        }

        public void setEncryptionKeyChanged(boolean encryptionKeyChanged) {
            this.encryptionKeyChanged = encryptionKeyChanged;
        }

        public boolean getNonDecodableEncoderChanged() {
            return nonDecodableEncoderChanged;
        }

        public void setNonDecodableEncoderChanged(boolean nonDecodableEncoderChanged) {
            this.nonDecodableEncoderChanged = nonDecodableEncoderChanged;
        }

        public boolean getDecodableEncoderChanged() {
            return decodableEncoderChanged;
        }

        public void setDecodableEncoderChanged(boolean decodableEncoderChanged) {
            this.decodableEncoderChanged = decodableEncoderChanged;
        }

        public boolean getNonDecodablePreferenceChanged() {
            return nonDecodablePreferenceChanged;
        }

        public void setNonDecodablePreferenceChanged(boolean nonDecodablePreferenceChanged) {
            this.nonDecodablePreferenceChanged = nonDecodablePreferenceChanged;
        }

        public boolean getPurgeCredsInLegacyTables() {
            return purgeCredsInLegacyTables;
        }

        public void setPurgeCredsInLegacyTables(boolean purgeCredsInLegacyTables) {
            this.purgeCredsInLegacyTables = purgeCredsInLegacyTables;
        }

        public boolean getMigrateLegacyCredsToNonLegacyDefault() {
            return migrateLegacyCredsToNonLegacyDefault;
        }

        public void setMigrateLegacyCredsToNonLegacyDefault(boolean migrateLegacyCredsToNonLegacyDefault) {
            this.migrateLegacyCredsToNonLegacyDefault = migrateLegacyCredsToNonLegacyDefault;
        }

        public boolean getMigrateLegacyCredsToNonLegacyDecodableOnly() {
            return migrateLegacyCredsToNonLegacyDecodableOnly;
        }

        public void setMigrateLegacyCredsToNonLegacyDecodableOnly(boolean migrateLegacyCredsToNonLegacyDecodableOnly) {
            this.migrateLegacyCredsToNonLegacyDecodableOnly = migrateLegacyCredsToNonLegacyDecodableOnly;
        }
    }
}
