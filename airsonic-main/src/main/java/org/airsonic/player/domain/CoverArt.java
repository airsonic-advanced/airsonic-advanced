package org.airsonic.player.domain;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

public class CoverArt {
    private int entityId;
    private EntityType entityType;
    private String path;
    private Integer folderId;
    private boolean overridden;
    private Instant created = Instant.now();
    private Instant updated = created;

    public enum EntityType {
        MEDIA_FILE, ALBUM, ARTIST
    }

    public CoverArt(int entityId, EntityType entityType, String path, Integer folderId, boolean overridden) {
        super();
        this.entityId = entityId;
        this.entityType = entityType;
        this.path = path;
        this.folderId = folderId;
        this.overridden = overridden;
    }

    public CoverArt(int entityId, EntityType entityType, String path, Integer folderId, boolean overridden, Instant created, Instant updated) {
        this(entityId, entityType, path, folderId, overridden);
        this.created = created;
        this.updated = updated;
    }

    public int getEntityId() {
        return entityId;
    }

    public void setEntityId(int entityId) {
        this.entityId = entityId;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Path getRelativePath() {
        return Paths.get(path);
    }

    public Path getFullPath(Path relativeMediaFolderPath) {
        return relativeMediaFolderPath.resolve(path);
    }

    public Integer getFolderId() {
        return folderId;
    }

    public void setFolderId(Integer folderId) {
        this.folderId = folderId;
    }

    public boolean getOverridden() {
        return overridden;
    }

    public void setOverridden(boolean overridden) {
        this.overridden = overridden;
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

}
