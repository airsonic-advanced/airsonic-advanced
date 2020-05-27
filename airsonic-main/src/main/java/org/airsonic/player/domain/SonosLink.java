package org.airsonic.player.domain;

import org.springframework.util.Assert;

public class SonosLink {
    private final String username;
    private final String householdId;
    private final String linkcode;

    public SonosLink(String username, String householdId, String linkcode) {
        Assert.notNull(username, "The username must be provided");
        Assert.notNull(householdId, "The householdId must be provided");
        Assert.notNull(linkcode, "The linkcode must be provided");
        this.username = username;
        this.householdId = householdId;
        this.linkcode = linkcode;
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
}
