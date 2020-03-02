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

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit test of {@link SecurityService}.
 *
 * @author Sindre Mehus
 */
public class SecurityServiceTestCase {

    @Test
    public void testIsFileInFolder() {
        assertTrue(SecurityService.isFileInFolder("/music/foo.mp3", "\\"));
        assertTrue(SecurityService.isFileInFolder("/music/foo.mp3", "/"));

        assertTrue(SecurityService.isFileInFolder("/music/foo.mp3", "/music"));
        assertTrue(SecurityService.isFileInFolder("\\music\\foo.mp3", "/music"));
        assertTrue(SecurityService.isFileInFolder("/music/foo.mp3", "\\music"));
        assertTrue(SecurityService.isFileInFolder("/music/foo.mp3", "\\music\\"));

        assertFalse(SecurityService.isFileInFolder("", "/tmp"));
        assertFalse(SecurityService.isFileInFolder("foo.mp3", "/tmp"));
        assertFalse(SecurityService.isFileInFolder("/music/foo.mp3", "/tmp"));
        assertFalse(SecurityService.isFileInFolder("/music/foo.mp3", "/tmp/music"));

        // identity tests
        assertTrue(SecurityService.isFileInFolder("/music/a", "/music/a"));
        assertTrue(SecurityService.isFileInFolder("/music/a", "/music/a/"));
        assertTrue(SecurityService.isFileInFolder("/music/a/", "/music/a/"));
        assertTrue(SecurityService.isFileInFolder("/music/a/", "/music/a"));
        assertFalse(SecurityService.isFileInFolder("/music/a2", "/music/a"));
        assertFalse(SecurityService.isFileInFolder("/music/a", "/music/a2"));

        // Test that references to the parent directory (..) is not allowed.
        assertTrue(SecurityService.isFileInFolder("/music/foo..mp3", "/music"));
        assertTrue(SecurityService.isFileInFolder("/music/foo..", "/music"));
        assertTrue(SecurityService.isFileInFolder("/music/foo.../", "/music"));
        assertFalse(SecurityService.isFileInFolder("/music/foo/..", "/music"));
        assertFalse(SecurityService.isFileInFolder("../music/foo", "/music"));
        assertFalse(SecurityService.isFileInFolder("/music/../foo", "/music"));
        assertFalse(SecurityService.isFileInFolder("/music/../bar/../foo", "/music"));
        assertFalse(SecurityService.isFileInFolder("/music\\foo\\..", "/music"));
        assertFalse(SecurityService.isFileInFolder("..\\music/foo", "/music"));
        assertFalse(SecurityService.isFileInFolder("/music\\../foo", "/music"));
        assertFalse(SecurityService.isFileInFolder("/music/..\\bar/../foo", "/music"));
    }
}

