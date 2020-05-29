package org.airsonic.player.domain;

import org.springframework.util.Assert;

import java.time.Instant;

public class SonosLink {
    private final String username;
    private final String linkcode;
    private final String householdId;
    private final String initiator;
    private final Instant initiated;

    public SonosLink(String username, String linkcode, String householdId, String initiator, Instant initiated) {
        Assert.notNull(username, "The username must be provided");
        Assert.notNull(householdId, "The householdId must be provided");
        Assert.notNull(linkcode, "The linkcode must be provided");
        this.username = username;
        this.linkcode = linkcode;
        this.householdId = householdId;
        this.initiator = initiator;
        this.initiated = initiated;
    }

    public String getUsername() {
        return username;
    }

    public String getHouseholdId() {
        return householdId;
    }

    public String getLinkcode() {
        return linkcode;
    }

    public String getInitiator() {
        return initiator;
    }

    public Instant getInitiated() {
        return initiated;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((householdId == null) ? 0 : householdId.hashCode());
        result = prime * result + ((linkcode == null) ? 0 : linkcode.hashCode());
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
        SonosLink other = (SonosLink) obj;
        if (householdId == null) {
            if (other.householdId != null)
                return false;
        } else if (!householdId.equals(other.householdId))
            return false;
        if (linkcode == null) {
            if (other.linkcode != null)
                return false;
        } else if (!linkcode.equals(other.linkcode))
            return false;
        if (username == null) {
            if (other.username != null)
                return false;
        } else if (!username.equals(other.username))
            return false;
        return true;
    }
}
