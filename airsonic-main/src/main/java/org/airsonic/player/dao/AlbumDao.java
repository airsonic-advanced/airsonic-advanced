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

import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Genre;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.apache.commons.lang.ObjectUtils;
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
 * Provides database services for albums.
 *
 * @author Sindre Mehus
 */
@Repository
public class AlbumDao extends AbstractDao {
    private static final String ALBUM_INSERT_COLUMNS = "name, artist, song_count, duration, year, play_count, last_played, comment, created, last_scanned, present, mb_release_id";
    private static final String ALBUM_QUERY_COLUMNS = "id, " + ALBUM_INSERT_COLUMNS;
    private static final String QUERY_COLUMNS = prefix(ALBUM_QUERY_COLUMNS, "a") + ", ag.genre, af.media_file_id";

    private final DBAlbumMapper rowMapper = new DBAlbumMapper();

    private static String constructAlbumQuery(boolean withMediaFileJoin, String... criteria) {
        String sql = "select " + QUERY_COLUMNS + " from album a "
                + " left join album_file af on a.id = af.album_id "
                + " left join album_genre ag on a.id = ag.album_id";

        if (withMediaFileJoin) {
            sql = sql + " left join media_file m on af.media_file_id = m.id";
        }

        if (criteria.length == 0) {
            return sql;
        }
        return sql + Stream.of(criteria).collect(joining(" and ", " where ", ""));
    }

    public Album getAlbum(int id) {
        return queryOneAlbum(consolidateDBAlbums(query(constructAlbumQuery(false, "a.id=?"), rowMapper, id)));
    }

    /**
     * Returns the album with the given artist and album name.
     *
     * @param artistName The artist name.
     * @param albumName  The album name.
     * @return The album or null.
     */
    public Album getAlbum(String artistName, String albumName) {
        return queryOneAlbum(consolidateDBAlbums(query(constructAlbumQuery(false, "a.artist=?", "a.name=?"), rowMapper, artistName, albumName)));
    }

    /**
     * Returns the album that the given file (most likely) is part of.
     *
     * @param file The media file.
     * @return The album or null.
     */
    public Album getAlbumForFile(MediaFile file) {

        // First, get all albums with the correct album name (irrespective of artist).
        List<Album> candidates = consolidateDBAlbums(query(constructAlbumQuery(false, "a.name=?"), rowMapper, file.getAlbumName()));
        if (candidates.isEmpty()) {
            return null;
        }

        // Look for album with the correct artist.
        for (Album candidate : candidates) {
            if (ObjectUtils.equals(candidate.getArtist(), file.getArtist())) {
                return candidate;
            }
        }

        // Look for album with the same mediaFileId as the file.
        return candidates.stream().filter(c -> c.getMediaFileIds().contains(file.getId())).findAny().orElse(null);
    }

    public List<Album> getAlbumsForArtist(final String artist, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<>();
        args.put("artist", artist);
        args.put("folders", MusicFolder.toIdList(musicFolders));

        return consolidateDBAlbums(namedQuery(
                constructAlbumQuery(true, "a.artist = :artist", "a.present", "m.folder_id in (:folders)")
                    + " order by a.name",
                rowMapper, args));
    }

    /**
     * Creates or updates an album.
     *
     * @param album The album to create/update.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void createOrUpdateAlbum(Album album) {
        String sql = "update album set " +
                     "song_count=?," +
                     "duration=?," +
                     "year=?," +
                     "play_count=?," +
                     "last_played=?," +
                     "comment=?," +
                     "created=?," +
                     "last_scanned=?," +
                     "present=?, " +
                     "mb_release_id=? " +
                     "where artist=? and name=?";

        int n = update(sql, album.getSongCount(), album.getDuration(), album.getYear(), album.getPlayCount(),
                album.getLastPlayed(), album.getComment(), album.getCreated(), album.getLastScanned(),
                album.isPresent(), album.getMusicBrainzReleaseId(), album.getArtist(), album.getName());

        if (n == 0) {
            update("insert into album (" + ALBUM_INSERT_COLUMNS + ") values (" + questionMarks(ALBUM_INSERT_COLUMNS) + ")",
                   album.getName(), album.getArtist(), album.getSongCount(), album.getDuration(),
                   album.getYear(), album.getPlayCount(), album.getLastPlayed(),
                   album.getComment(), album.getCreated(), album.getLastScanned(), album.isPresent(), album.getMusicBrainzReleaseId());
        }

        int id = queryForInt("select id from album where artist=? and name=?", null, album.getArtist(), album.getName());
        album.setId(id);
        updateAlbumFileIds(album);
        updateAlbumGenres(album);
    }

    public void updateAlbumFileIds(Album album) {
        // TODO we could do a diff with db and insert or delete only relevant ids
        update("delete from album_file where album_id=?", album.getId());
        batchedUpdate("insert into album_file(album_id, media_file_id) values (?,?)",
                album.getMediaFileIds().parallelStream().map(id -> new Object[] { album.getId(), id }).collect(Collectors.toList()));
    }

    public void updateAlbumGenres(Album album) {
        // TODO we could do a diff with db and insert or delete only relevant genres
        update("delete from album_genre where album_id=?", album.getId());
        batchedUpdate("insert into album_genre(album_id, genre) values (?,?)",
                album.getGenres().parallelStream().map(g -> new Object[] { album.getId(), g.getName() }).collect(Collectors.toList()));
    }

    /**
     * Returns albums in alphabetical order.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param byArtist     Whether to sort by artist name
     * @param musicFolders Only return albums from these folders.
     * @param ignoreCase   Use case insensitive sorting
     * @return Albums in alphabetical order.
     */
    public List<Album> getAlphabeticalAlbums(final int offset, final int count, boolean byArtist, boolean ignoreCase, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<>();
        args.put("folders", MusicFolder.toIdList(musicFolders));
        args.put("count", count);
        args.put("offset", offset);
        String orderBy;
        if (ignoreCase) {
            orderBy = byArtist ? "LOWER(a.artist),  LOWER(a.name)" : "LOWER(a.name)";
        } else {
            orderBy = byArtist ? "a.artist, a.name" : "a.name";
        }

        return consolidateDBAlbums(namedQuery(
                constructAlbumQuery(true, "a.present", "m.folder_id in (:folders)")
                    + " order by " + orderBy + ", a.id limit :count offset :offset",
                rowMapper, args));
    }

    /**
     * Returns the count of albums in the given folders
     *
     * @param musicFolders Only return albums from these folders.
     * @return the count of present albums
     */
    public int getAlbumCount(final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return 0;
        }
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("folders", MusicFolder.toIdList(musicFolders));

        return namedQueryForInt("select count(distinct a.id) from album a left join album_file af on a.id=af.album_id left join media_file mf on af.media_file_id = mf.id where a.present and m.folder_id in (:folders)", 0, args);
    }

    public Collection<Integer> getFolderIds(Album a) {
        if (a.getMediaFileIds().isEmpty()) {
            return Collections.emptyList();
        }
        String sql = "select distinct(folder_id) from media_file where id in (:ids)";
        Map<String, Object> args = new HashMap<>();
        args.put("ids", a.getMediaFileIds());
        return namedQueryForTypes(sql, Integer.class, args);
    }

    /**
     * Returns the most frequently played albums.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param musicFolders Only return albums from these folders.
     * @return The most frequently played albums.
     */
    public List<Album> getMostFrequentlyPlayedAlbums(final int offset, final int count, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<>();
        args.put("folders", MusicFolder.toIdList(musicFolders));
        args.put("count", count);
        args.put("offset", offset);
        return consolidateDBAlbums(namedQuery(
                constructAlbumQuery(true, "a.present", "m.folder_id in (:folders)", "a.play_count > 0")
                    + " order by a.play_count desc, a.id limit :count offset :offset",
                rowMapper, args));
    }

    /**
     * Returns the most recently played albums.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param musicFolders Only return albums from these folders.
     * @return The most recently played albums.
     */
    public List<Album> getMostRecentlyPlayedAlbums(final int offset, final int count, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<>();
        args.put("folders", MusicFolder.toIdList(musicFolders));
        args.put("count", count);
        args.put("offset", offset);
        return consolidateDBAlbums(namedQuery(
                constructAlbumQuery(true, "a.present", "m.folder_id in (:folders)", "a.last_played is not null")
                    + " order by a.last_played desc, a.id limit :count offset :offset",
                rowMapper, args));
    }

    /**
     * Returns the most recently added albums.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param musicFolders Only return albums from these folders.
     * @return The most recently added albums.
     */
    public List<Album> getNewestAlbums(final int offset, final int count, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<>();
        args.put("folders", MusicFolder.toIdList(musicFolders));
        args.put("count", count);
        args.put("offset", offset);
        return consolidateDBAlbums(namedQuery(
                constructAlbumQuery(true, "a.present", "m.folder_id in (:folders)")
                    + " order by a.created desc, a.id desc limit :count offset :offset",
                rowMapper, args));
    }

    /**
     * Returns the most recently starred albums.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param username     Returns albums starred by this user.
     * @param musicFolders Only return albums from these folders.
     * @return The most recently starred albums for this user.
     */
    public List<Album> getStarredAlbums(final int offset, final int count, final String username, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<>();
        args.put("folders", MusicFolder.toIdList(musicFolders));
        args.put("count", count);
        args.put("offset", offset);
        args.put("username", username);

        String sql = ""
                + "select a.*, af.media_file_id, ag.genre "
                + "from album a "
                + "join starred_album sa on a.id = sa.album_id "
                + "left join album_file af on a.id = af.album_id "
                + "left join album_genre ag on a.id = ag.album_id "
                + "left join media_file m on af.media_file_id = m.id "
                + "where a.present "
                + "and m.folder_id in (:folders) "
                + "and sa.username = :username "
                + "order by sa.created desc, a.id limit :count offset :offset";

        return consolidateDBAlbums(namedQuery(sql, rowMapper, args));
    }

    /**
     * Returns albums in a genre.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param genres       The list of genres.
     * @param musicFolders Only return albums from these folders.
     * @return Albums in the genre.
     */
    public List<Album> getAlbumsByGenre(final int offset, final int count, final Set<Genre> genres, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<>();
        args.put("folders", MusicFolder.toIdList(musicFolders));
        args.put("count", count);
        args.put("offset", offset);
        args.put("genres", genres);
        return consolidateDBAlbums(namedQuery(
                constructAlbumQuery(true, "a.present", "m.folder_id in (:folders)", "ag.genre in (:genres)")
                    + " order by a.id limit :count offset :offset",
                rowMapper, args));
    }

    /**
     * Returns albums within a year range.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param fromYear     The first year in the range.
     * @param toYear       The last year in the range.
     * @param musicFolders Only return albums from these folders.
     * @return Albums in the year range.
     */
    public List<Album> getAlbumsByYear(final int offset, final int count, final int fromYear, final int toYear, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<>();
        args.put("folders", MusicFolder.toIdList(musicFolders));
        args.put("count", count);
        args.put("offset", offset);
        args.put("fromYear", fromYear);
        args.put("toYear", toYear);
        if (fromYear <= toYear) {
            return consolidateDBAlbums(namedQuery(
                    constructAlbumQuery(true, "a.present", "m.folder_id in (:folders)", "year between :fromYear and :toYear")
                            + " order by a.year, a.id limit :count offset :offset",
                    rowMapper, args));
        } else {
            return consolidateDBAlbums(namedQuery(
                    constructAlbumQuery(true, "a.present", "m.folder_id in (:folders)", "year between :toYear and :fromYear")
                            + " order by a.year desc, a.id limit :count offset :offset",
                    rowMapper, args));
        }
    }

    public void markNonPresent(Instant lastScanned) {
        update("update album set present=false where last_scanned < ? and present", lastScanned);
    }

    public List<Integer> getExpungeCandidates() {
        return queryForInts("select id from album where not present");
    }

    public void expunge() {
        update("delete from album where not present");
    }

    public void starAlbum(int albumId, String username) {
        unstarAlbum(albumId, username);
        update("insert into starred_album(album_id, username, created) values (?,?,?)", albumId, username, Instant.now());
    }

    public void unstarAlbum(int albumId, String username) {
        update("delete from starred_album where album_id=? and username=?", albumId, username);
    }

    public Instant getAlbumStarredDate(int albumId, String username) {
        return queryForInstant("select created from starred_album where album_id=? and username=?", null, albumId, username);
    }

    private Album queryOneAlbum(List<Album> albums) {
        if (albums.size() == 0) {
            return null;
        }
        return albums.get(0);
    }

    private List<Album> consolidateDBAlbums(List<DBAlbum> albums) {
        if (albums == null) {
            return null;
        }
        if (albums.size() == 1) {
            if (albums.get(0).getMediaFileId() != null) {
                albums.get(0).getMediaFileIds().add(albums.get(0).getMediaFileId());
            }
            if (albums.get(0).getGenre() != null) {
                albums.get(0).getGenres().add(albums.get(0).getGenre());
            }
            return Collections.singletonList(albums.get(0));
        }
        return albums.stream().collect(Collectors.groupingBy(DBAlbum::getId, reducing((da1, da2) -> {
            if (da1.getMediaFileId() != null) {
                da1.getMediaFileIds().add(da1.getMediaFileId());
            }
            if (da2.getMediaFileId() != null) {
                da1.getMediaFileIds().add(da2.getMediaFileId());
            }
            if (da1.getGenre() != null) {
                da1.getGenres().add(da1.getGenre());
            }
            if (da2.getGenre() != null) {
                da1.getGenres().add(da2.getGenre());
            }
            return da1;
        }))).values().stream().filter(Optional::isPresent).map(Optional::get).collect(toList());
    }

    private static class DBAlbum extends Album {
        public DBAlbum(int id, String name, String artist, int songCount, double duration, Integer year,
                int playCount, Instant lastPlayed, String comment, Instant created, Instant lastScanned,
                boolean present, String musicBrainzReleaseId, Integer mediaFileId, Genre genre) {
            super(id, name, artist, songCount, duration, year, playCount, lastPlayed, comment, created, lastScanned, present, musicBrainzReleaseId);
            this.mediaFileId = mediaFileId;
            this.genre = genre;
        }

        private Integer mediaFileId;
        private Genre genre;

        public Integer getMediaFileId() {
            return mediaFileId;
        }

        public Genre getGenre() {
            return genre;
        }
    }

    private static class DBAlbumMapper implements RowMapper<DBAlbum> {
        @Override
        public DBAlbum mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new DBAlbum(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("artist"),
                    rs.getInt("song_count"),
                    rs.getDouble("duration"),
                    rs.getInt("year") == 0 ? null : rs.getInt("year"),
                    rs.getInt("play_count"),
                    Optional.ofNullable(rs.getTimestamp("last_played")).map(x -> x.toInstant()).orElse(null),
                    rs.getString("comment"),
                    Optional.ofNullable(rs.getTimestamp("created")).map(x -> x.toInstant()).orElse(null),
                    Optional.ofNullable(rs.getTimestamp("last_scanned")).map(x -> x.toInstant()).orElse(null),
                    rs.getBoolean("present"),
                    rs.getString("mb_release_id"),
                    (Integer) rs.getObject("media_file_id"),
                    Optional.ofNullable((String) rs.getObject("genre")).map(Genre::new).orElse(null));
        }
    }
}
