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

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides database services for ratings.
 *
 * @author Sindre Mehus
 */
@Repository("musicFileInfoDao")
public class RatingDao extends AbstractDao {

    /**
     * Returns paths for the highest rated albums.
     *
     * @param offset      Number of albums to skip.
     * @param count       Maximum number of albums to return.
     * @param musicFolders Only return albums in these folders.
     * @return Paths for the highest rated albums.
     */
    public List<Integer> getHighestRatedAlbums(final int offset, final int count, final List<MusicFolder> musicFolders) {
        if (count < 1 || musicFolders.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Object> args = new HashMap<>();
        args.put("type", MediaFile.MediaType.ALBUM.name());
        args.put("folders", MusicFolder.toIdList(musicFolders));
        args.put("count", count);
        args.put("offset", offset);

        String sql = "select user_rating.media_file_id from user_rating, media_file " +
                     "where user_rating.media_file_id=media_file.id and media_file.present and media_file.type = :type and media_file.folder_id in (:folders) " +
                     "group by user_rating.media_file_id " +
                     "order by avg(rating) desc, user_rating.media_file_id limit :count offset :offset";
        return namedQueryForTypes(sql, Integer.class, args);
    }

    /**
     * Sets the rating for a media file and a given user.
     *
     * @param username  The user name.
     * @param mediaFile The media file.
     * @param rating    The rating between 1 and 5, or <code>null</code> to remove the rating.
     */
    public void setRatingForUser(String username, MediaFile mediaFile, Integer rating) {
        if (rating != null && (rating < 1 || rating > 5)) {
            return;
        }

        update("delete from user_rating where username=? and media_file_id=?", username, mediaFile.getId());
        if (rating != null) {
            update("insert into user_rating(username, media_file_id, rating) values(?, ?, ?)", username, mediaFile.getId(), rating);
        }
    }

    /**
     * Returns the average rating for the given media file.
     *
     * @param mediaFile The media file.
     * @return The average rating, or <code>null</code> if no ratings are set.
     */
    public Double getAverageRating(MediaFile mediaFile) {
        return queryForDouble("select avg(rating) from user_rating where media_file_id=?", null, mediaFile.getId());
    }

    /**
     * Returns the rating for the given user and media file.
     *
     * @param username  The user name.
     * @param mediaFile The media file.
     * @return The rating, or <code>null</code> if no rating is set.
     */
    public Integer getRatingForUser(String username, MediaFile mediaFile) {
        return queryForInt("select rating from user_rating where username=? and media_file_id=?", null, username, mediaFile.getId());
    }

    public int getRatedAlbumCount(final String username, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return 0;
        }
        Map<String, Object> args = new HashMap<>();
        args.put("type", MediaFile.MediaType.ALBUM.name());
        args.put("folders", MusicFolder.toIdList(musicFolders));
        args.put("username", username);

        return namedQueryForInt("select count(*) from user_rating, media_file " +
                                "where media_file.id = user_rating.media_file_id " +
                                "and media_file.type = :type " +
                                "and media_file.present " +
                                "and media_file.folder_id in (:folders) " +
                                "and user_rating.username = :username",
                                0, args);
    }
}
