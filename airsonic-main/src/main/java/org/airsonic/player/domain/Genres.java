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
package org.airsonic.player.domain;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

/**
 * Represents a list of genres.
 *
 * @author Sindre Mehus
 * @version $Revision: 1.2 $ $Date: 2005/12/25 13:48:46 $
 */
public class Genres {

    private final Map<String, Genre> genres = new ConcurrentHashMap<>();

    // genre names can be ([genre] --> [split to])
    // - [abc] --> ['abc']
    // - [abc;] --> ['abc']
    // - [abc; ] --> ['abc', ' ']
    // - [abc;xyz] --> ['abc', 'xyz']
    // - [abc; xyz] --> ['abc', ' xyz']

    public static Set<Genre> parseGenres(String genreString, String separators) {
        if (StringUtils.isBlank(genreString)) {
            return Collections.emptySet();
        }
        String[] splitGenres = StringUtils.split(genreString, separators);
        return Stream.of(splitGenres)
                .map(StringUtils::trim)
                .filter(StringUtils::isNotBlank)
                .map(Genre::new)
                .collect(toSet());
    }

    public Set<Genre> addGenres(Set<Genre> incoming) {
        return incoming.stream().map(g -> genres.computeIfAbsent(g.getName(), k -> g)).collect(toSet());
    }

    public List<Genre> getGenres() {
        return new ArrayList<Genre>(genres.values());
    }
}
