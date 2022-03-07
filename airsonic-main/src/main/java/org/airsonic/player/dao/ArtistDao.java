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
package org.airsonic.player.dao;

import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MusicFolder;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toList;

/**
 * Provides database services for artists.
 *
 * @author Sindre Mehus
 */
@Repository
public class ArtistDao extends AbstractDao {
    private static final String ARTIST_INSERT_COLUMNS = "name, last_scanned, present";
    private static final String ARTIST_QUERY_COLUMNS = "id, " + ARTIST_INSERT_COLUMNS;
    private static final String QUERY_COLUMNS = prefix(ARTIST_QUERY_COLUMNS, "ar") + ", aa.album_id";

    private final DBArtistMapper rowMapper = new DBArtistMapper();

    private static String constructArtistQuery(boolean withMediaFileJoin, String... criteria) {
        String sql = "select " + QUERY_COLUMNS + " from artist ar "
                + " left join artist_album aa on ar.id = aa.artist_id ";

        if (withMediaFileJoin) {
            sql = sql + " left join album_file af on aa.album_id = af.album_id "
                    + " left join media_file m on af.media_file_id = m.id";
        }

        if (criteria.length == 0) {
            return sql;
        }
        return sql + Stream.of(criteria).collect(joining(" and ", " where ", ""));
    }

    /**
     * Returns the artist with the given name.
     *
     * @param artistName The artist name.
     * @return The artist or null.
     */
    public Artist getArtist(String artistName) {
        return queryOneArtist(consolidateDBArtists(query(constructArtistQuery(false, "ar.name=?"), rowMapper, artistName)));
    }

    /**
     * Returns the artist with the given name.
     *
     * @param artistName   The artist name.
     * @param musicFolders Only return artists that have at least one album in these folders.
     * @return The artist or null.
     */
    public Artist getArtist(final String artistName, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return null;
        }
        Map<String, Object> args = new HashMap<>();
        args.put("name", artistName);
        args.put("folders", MusicFolder.toIdList(musicFolders));
        return queryOneArtist(consolidateDBArtists(namedQuery(constructArtistQuery(true, "ar.name = :name", "m.folder_id in (:folders)"), rowMapper, args)));
    }

    /**
     * Returns the artist with the given ID.
     *
     * @param id The artist ID.
     * @return The artist or null.
     */
    public Artist getArtist(int id) {
        return queryOneArtist(consolidateDBArtists(query(constructArtistQuery(false, "ar.id=?"), rowMapper, id)));
    }

    public Collection<Integer> getFolderIds(Artist a) {
        if (a.getAlbumIds().isEmpty()) {
            return Collections.emptyList();
        }
        String sql = "select distinct(m.folder_id) from album_file af join media_file m on af.media_file_id = m.id where af.album_id in (:ids)";
        Map<String, Object> args = new HashMap<>();
        args.put("ids", a.getAlbumIds());
        return namedQueryForTypes(sql, Integer.class, args);
    }

    /**
     * Creates or updates an artist.
     *
     * @param artist The artist to create/update.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void createOrUpdateArtist(Artist artist) {
        String sql = "update artist set " +
                     "last_scanned=?, " +
                     "present=? " +
                     "where name=?";

        int n = update(sql, artist.getLastScanned(), artist.isPresent(), artist.getName());

        if (n == 0) {
            update("insert into artist (" + ARTIST_INSERT_COLUMNS + ") values (" + questionMarks(ARTIST_INSERT_COLUMNS) + ")",
                   artist.getName(), artist.getLastScanned(), artist.isPresent());
        }

        int id = queryForInt("select id from artist where name=?", null, artist.getName());
        artist.setId(id);

        updateArtistAlbums(artist);
    }

    public void updateArtistAlbums(Artist artist) {
        // TODO we could do a diff with db and insert or delete only relevant ids
        update("delete from artist_album where artist_id=?", artist.getId());
        batchedUpdate("insert into artist_album(artist_id, album_id) values (?,?)",
                artist.getAlbumIds().parallelStream().map(id -> new Object[] { artist.getId(), id }).collect(Collectors.toList()));
    }

    /**
     * Returns artists in alphabetical order.
     *
     * @param offset       Number of artists to skip.
     * @param count        Maximum number of artists to return.
     * @param musicFolders Only return artists that have at least one album in these folders.
     * @return Artists in alphabetical order.
     */
    public List<Artist> getAlphabeticalArtists(final int offset, final int count, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<>();
        args.put("folders", MusicFolder.toIdList(musicFolders));
        args.put("count", count);
        args.put("offset", offset);

        return consolidateDBArtists(namedQuery(
                constructArtistQuery(true, "ar.present", "m.folder_id in (:folders)")
                        + " order by ar.name, ar.id limit :count offset :offset",
                rowMapper, args));
    }

    /**
     * Returns the most recently starred artists.
     *
     * @param offset       Number of artists to skip.
     * @param count        Maximum number of artists to return.
     * @param username     Returns artists starred by this user.
     * @param musicFolders Only return artists that have at least one album in these folders.
     * @return The most recently starred artists for this user.
     */
    public List<Artist> getStarredArtists(final int offset, final int count, final String username, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<>();
        args.put("folders", MusicFolder.toIdList(musicFolders));
        args.put("username", username);
        args.put("count", count);
        args.put("offset", offset);

        String sql = ""
                + "select ar.*, aa.album_id "
                + "from artist ar "
                + "join starred_artist sa on ar.id = sa.artist_id "
                + "left join artist_album aa on ar.id = aa.artist_id "
                + "left join album_file af on aa.album_id = af.album_id "
                + "left join media_file m on af.media_file_id = m.id "
                + "where ar.present "
                + "and m.folder_id in (:folders) "
                + "and sa.username = :username "
                + "order by sa.created desc, ar.id limit :count offset :offset";

        return consolidateDBArtists(namedQuery(sql, rowMapper, args));
    }

    public void markNonPresent(Instant lastScanned) {
        update("update artist set present=false where last_scanned < ? and present", lastScanned);
    }

    public List<Integer> getExpungeCandidates() {
        return queryForInts("select id from artist where not present");
    }

    public void expunge() {
        update("delete from artist where not present");
    }

    public void starArtist(int artistId, String username) {
        unstarArtist(artistId, username);
        update("insert into starred_artist(artist_id, username, created) values (?,?,?)", artistId, username, Instant.now());
    }

    public void unstarArtist(int artistId, String username) {
        update("delete from starred_artist where artist_id=? and username=?", artistId, username);
    }

    public Instant getArtistStarredDate(int artistId, String username) {
        return queryForInstant("select created from starred_artist where artist_id=? and username=?", null, artistId, username);
    }

    private Artist queryOneArtist(List<Artist> artists) {
        if (artists.size() == 0) {
            return null;
        }
        return artists.get(0);
    }

    private List<Artist> consolidateDBArtists(List<DBArtist> artists) {
        if (artists == null) {
            return null;
        }
        if (artists.size() == 1) {
            if (artists.get(0).getAlbumId() != null) {
                artists.get(0).getAlbumIds().add(artists.get(0).getAlbumId());
            }
            return Collections.singletonList(artists.get(0));
        }
        return artists.stream().collect(Collectors.groupingBy(DBArtist::getId, reducing((da1, da2) -> {
            if (da1.getAlbumId() != null) {
                da1.getAlbumIds().add(da1.getAlbumId());
            }
            if (da2.getAlbumId() != null) {
                da1.getAlbumIds().add(da2.getAlbumId());
            }
            return da1;
        }))).values().stream().filter(Optional::isPresent).map(Optional::get).collect(toList());
    }

    private static class DBArtist extends Artist {
        private Integer albumId;

        public DBArtist(int id, String name, Instant lastScanned, boolean present, Integer albumId) {
            super(id, name, lastScanned, present);
            this.albumId = albumId;
        }

        public Integer getAlbumId() {
            return albumId;
        }
    }

    private static class DBArtistMapper implements RowMapper<DBArtist> {
        @Override
        public DBArtist mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new DBArtist(
                    rs.getInt("id"),
                    rs.getString("name"),
                    Optional.ofNullable(rs.getTimestamp("last_scanned")).map(x -> x.toInstant()).orElse(null),
                    rs.getBoolean("present"),
                    (Integer) rs.getObject("album_id"));
        }
    }
}
