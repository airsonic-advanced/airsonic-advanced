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
package org.airsonic.player.controller;

import org.airsonic.player.command.SearchCommand;
import org.airsonic.player.dao.AlbumDao;
import org.airsonic.player.domain.*;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.service.MediaFolderService;
import org.airsonic.player.service.PlayerService;
import org.airsonic.player.service.SearchService;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.search.IndexType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;

/**
 * Controller for the search page.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/search")
public class SearchController {

    @Autowired
    private SecurityService securityService;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private SearchService searchService;
    @Autowired
    private MediaFolderService mediaFolderService;
    @Autowired
    private MediaFileService mediaFileService;
    @Autowired
    private AlbumDao albumDao;

    @GetMapping
    protected String displayForm() {
        return "search";
    }

    @ModelAttribute
    protected void formBackingObject(HttpServletRequest request, Model model) {
        model.addAttribute("command",new SearchCommand());
    }

    @PostMapping
    protected String onSubmit(HttpServletRequest request, HttpServletResponse response,@ModelAttribute("command") SearchCommand command, Model model) throws Exception {

        User user = securityService.getCurrentUser(request);
        UserSettings userSettings = settingsService.getUserSettings(user.getUsername());
        command.setUser(user);
        command.setPartyModeEnabled(userSettings.getPartyModeEnabled());

        List<MusicFolder> musicFolders = mediaFolderService.getMusicFoldersForUser(user.getUsername());
        String query = StringUtils.trimToNull(command.getQuery());

        if (query != null) {

            SearchCriteria criteria = new SearchCriteria();
            criteria.setCount(userSettings.getSearchCount());
            criteria.setQuery(query);

            SearchResult artists = searchService.search(criteria, musicFolders, IndexType.ARTIST);
            SearchResult artistsId3 = searchService.search(criteria, musicFolders, IndexType.ARTIST_ID3);
            artistsId3.getArtists().stream().map(Artist::getName).flatMap(ar -> albumDao.getAlbumsForArtist(ar, musicFolders).stream().map(al -> mediaFileService.getMediaFile(al.getPath(), al.getFolderId())).filter(Objects::nonNull).map(MediaFile::getId).map(m -> Pair.of(ar, m))).collect(groupingBy(p -> p.getKey(), mapping(p -> p.getValue(), toSet())));
            artists.getMediaFiles().stream().map(m -> Pair.of(Optional.ofNullable(m.getArtist()).or(() -> Optional.ofNullable(m.getAlbumArtist())).orElse("(Unknown)"), m.getId()));
            command.setArtists(Stream.concat(
                    artistsId3.getArtists().stream()
                        .map(Artist::getName)
                        .flatMap(ar -> albumDao.getAlbumsForArtist(ar, musicFolders).stream()
                                .map(al -> mediaFileService.getMediaFile(al.getPath(), al.getFolderId()))
                                    .filter(Objects::nonNull)
                                .map(MediaFile::getId)
                                .map(m -> Pair.of(ar, m))),
                    artists.getMediaFiles().stream()
                        .map(m -> Pair.of(
                                Optional.ofNullable(m.getArtist())
                                    .or(() -> Optional.ofNullable(m.getAlbumArtist()))
                                    .orElse("(Unknown)"),
                                m.getId())))
                .collect(groupingBy(p -> p.getKey(), mapping(p -> p.getValue(), toSet()))));

            SearchResult albums = searchService.search(criteria, musicFolders, IndexType.ALBUM);
            SearchResult albumsId3 = searchService.search(criteria, musicFolders, IndexType.ALBUM_ID3);
            command.setAlbums(Stream
                    .concat(albumsId3.getAlbums().stream()
                            .map(a -> Optional.ofNullable(mediaFileService.getMediaFile(a.getPath(), a.getFolderId()))
                                    .map(m -> Pair.of(Pair.of(a.getName(), a.getArtist()), m.getId())).orElse(null))
                            .filter(Objects::nonNull),
                            albums.getMediaFiles().stream()
                                    .map(m -> Pair.of(Pair.of(m.getAlbumName(), m.getAlbumArtist()), m.getId())))
                    .collect(groupingBy(p -> p.getKey(), mapping(p -> p.getValue(), toSet()))));

            SearchResult songs = searchService.search(criteria, musicFolders, IndexType.SONG);
            command.setSongs(songs.getMediaFiles());

            command.setPlayer(playerService.getPlayer(request, response));
        }

        return "search";
    }
}
