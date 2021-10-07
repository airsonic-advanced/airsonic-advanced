/*
 * This file is part of Airsonic.
 *
 *  Airsonic is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Airsonic is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Copyright 2015 (C) Sindre Mehus
 */

package org.airsonic.player.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sonos.services._1.*;
import com.sonos.services._1_1.CustomFault;
import com.sonos.services._1_1.SonosSoap;
import org.airsonic.player.dao.SonosLinkDao;
import org.airsonic.player.domain.AlbumListType;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.Playlist;
import org.airsonic.player.domain.SonosLink;
import org.airsonic.player.service.search.IndexType;
import org.airsonic.player.service.sonos.SonosHelper;
import org.airsonic.player.service.sonos.SonosLinkSecurityInterceptor;
import org.airsonic.player.service.sonos.SonosServiceRegistration;
import org.airsonic.player.service.sonos.SonosSoapFault;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.Holder;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;

import static org.airsonic.player.service.sonos.SonosServiceRegistration.AuthenticationType;

/**
 * For manual testing of this service:
 * curl -s -X POST -H "Content-Type: text/xml;charset=UTF-8" -H 'SOAPACTION: "http://www.sonos.com/Services/1.1#getSessionId"' -d @getSessionId.xml http://localhost:4040/ws/Sonos | xmllint --format -
 *
 * @author Sindre Mehus
 * @version $Id$
 */
@Service
public class SonosService implements SonosSoap {

    private static final Logger LOG = LoggerFactory.getLogger(SonosService.class);

    public static final String ID_ROOT = "root";
    public static final String ID_SHUFFLE = "shuffle";
    public static final String ID_ALBUMLISTS = "albumlists";
    public static final String ID_PLAYLISTS = "playlists";
    public static final String ID_PODCASTS = "podcasts";
    public static final String ID_LIBRARY = "library";
    public static final String ID_STARRED = "starred";
    public static final String ID_STARRED_ARTISTS = "starred-artists";
    public static final String ID_STARRED_ALBUMS = "starred-albums";
    public static final String ID_STARRED_SONGS = "starred-songs";
    public static final String ID_SEARCH = "search";
    public static final String ID_SHUFFLE_MUSICFOLDER_PREFIX = "shuffle-musicfolder:";
    public static final String ID_SHUFFLE_ARTIST_PREFIX = "shuffle-artist:";
    public static final String ID_SHUFFLE_ALBUMLIST_PREFIX = "shuffle-albumlist:";
    public static final String ID_RADIO_ARTIST_PREFIX = "radio-artist:";
    public static final String ID_MUSICFOLDER_PREFIX = "musicfolder:";
    public static final String ID_PLAYLIST_PREFIX = "playlist:";
    public static final String ID_ALBUMLIST_PREFIX = "albumlist:";
    public static final String ID_PODCAST_CHANNEL_PREFIX = "podcast-channel:";
    public static final String ID_DECADE_PREFIX = "decade:";
    public static final String ID_GENRE_PREFIX = "genre:";
    public static final String ID_SIMILAR_ARTISTS_PREFIX = "similarartists:";

    // Note: These must match the values in presentationMap.xml
    public static final String ID_SEARCH_ARTISTS = "search-artists";
    public static final String ID_SEARCH_ALBUMS = "search-albums";
    public static final String ID_SEARCH_SONGS = "search-songs";

    @Autowired
    private SonosHelper sonosHelper;
    @Autowired
    private MediaFileService mediaFileService;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private PlaylistService playlistService;
    @Autowired
    private UPnPService upnpService;
    @Autowired
    private SonosServiceRegistration registration;
    @Autowired
    private SonosLinkDao sonosLinkDao;

    /**
     * The context for the request. This is used to get the Auth information
     * form the headers as well as using the request url to build the correct
     * media resource url.
     */
    @Resource
    private WebServiceContext context;

    /**
     * Try to enable/disable the Sonos link.
     *
     * @return list of message codes for user return
     */
    public List<String> updateMusicServiceRegistration() {
        boolean enabled = settingsService.isSonosEnabled();
        String baseUrl = settingsService.getSonosCallbackHostAddress();
        List<String> messagesCodes = new ArrayList<>();

        List<String> sonosControllers = upnpService.getSonosControllerHosts();
        if (sonosControllers.isEmpty()) {
            LOG.info("No Sonos controller found");
            messagesCodes.add("sonossettings.controller.notfound");
            return messagesCodes;
        }
        LOG.info("Found Sonos controllers: {}", sonosControllers);

        String sonosServiceName = settingsService.getSonosServiceName();
        int sonosServiceId = settingsService.getSonosServiceId();
        AuthenticationType authenticationType = AuthenticationType.valueOf(settingsService.getSonosLinkMethod());

        for (String sonosController : sonosControllers) {
            try {
                if (registration.setEnabled(baseUrl, sonosController, enabled, sonosServiceName, sonosServiceId, authenticationType)) {

                    messagesCodes.add("sonossettings.sonoslink.success");
                    // Remove old links.
                    if (!enabled) {
                        sonosLinkDao.removeAll();
                        messagesCodes.add("sonossettings.sonoslink.removed");
                    }
                    break;
                }
            } catch (IOException x) {
                messagesCodes.add("sonossettings.exception");
                LOG.warn("Failed to enable/disable music service in Sonos controller {}", sonosController, x);
            }
            messagesCodes.add("sonossettings.sonoslink.fail");
        }

        return messagesCodes;
    }


    @Override
    public LastUpdate getLastUpdate() {
        LastUpdate result = new LastUpdate();
        // Effectively disabling caching
        result.setCatalog(RandomStringUtils.randomAlphanumeric(8));
        result.setFavorites(RandomStringUtils.randomAlphanumeric(8));
        return result;
    }

    @Override
    public GetMetadataResponse getMetadata(GetMetadata parameters) {
        String id = parameters.getId();
        int index = parameters.getIndex();
        int count = parameters.getCount();
        String username = getUsername();
        HttpServletRequest request = getRequest();

        LOG.debug("getMetadata: id={} index={} count={} recursive={}", id, index, count, parameters.isRecursive());

        List<? extends AbstractMedia> media = null;
        MediaList mediaList = null;

        if (ID_ROOT.equals(id)) {
            media = sonosHelper.forRoot();
        } else {
            if (ID_SHUFFLE.equals(id)) {
                media = sonosHelper.forShuffle(count, username, request);
            } else if (ID_LIBRARY.equals(id)) {
                media = sonosHelper.forLibrary(username, request);
            } else if (ID_PLAYLISTS.equals(id)) {
                media = sonosHelper.forPlaylists(username, request);
            } else if (ID_ALBUMLISTS.equals(id)) {
                media = sonosHelper.forAlbumLists();
            } else if (ID_PODCASTS.equals(id)) {
                media = sonosHelper.forPodcastChannels();
            } else if (ID_STARRED.equals(id)) {
                media = sonosHelper.forStarred();
            } else if (ID_STARRED_ARTISTS.equals(id)) {
                media = sonosHelper.forStarredArtists(username, request);
            } else if (ID_STARRED_ALBUMS.equals(id)) {
                media = sonosHelper.forStarredAlbums(username, request);
            } else if (ID_STARRED_SONGS.equals(id)) {
                media = sonosHelper.forStarredSongs(username, request);
            } else if (ID_SEARCH.equals(id)) {
                media = sonosHelper.forSearchCategories();
            } else if (id.startsWith(ID_PLAYLIST_PREFIX)) {
                int playlistId = Integer.parseInt(id.replace(ID_PLAYLIST_PREFIX, ""));
                media = sonosHelper.forPlaylist(playlistId, username, request);
            } else if (id.startsWith(ID_DECADE_PREFIX)) {
                int decade = Integer.parseInt(id.replace(ID_DECADE_PREFIX, ""));
                media = sonosHelper.forDecade(decade, username, request);
            } else if (id.startsWith(ID_GENRE_PREFIX)) {
                int genre = Integer.parseInt(id.replace(ID_GENRE_PREFIX, ""));
                media = sonosHelper.forGenre(genre, username, request);
            } else if (id.startsWith(ID_ALBUMLIST_PREFIX)) {
                AlbumListType albumListType = AlbumListType.fromId(id.replace(ID_ALBUMLIST_PREFIX, ""));
                mediaList = sonosHelper.forAlbumList(albumListType, index, count, username, request);
            } else if (id.startsWith(ID_PODCAST_CHANNEL_PREFIX)) {
                int channelId = Integer.parseInt(id.replace(ID_PODCAST_CHANNEL_PREFIX, ""));
                media = sonosHelper.forPodcastChannel(channelId, username, request);
            } else if (id.startsWith(ID_MUSICFOLDER_PREFIX)) {
                int musicFolderId = Integer.parseInt(id.replace(ID_MUSICFOLDER_PREFIX, ""));
                media = sonosHelper.forMusicFolder(musicFolderId, username, request);
            } else if (id.startsWith(ID_SHUFFLE_MUSICFOLDER_PREFIX)) {
                int musicFolderId = Integer.parseInt(id.replace(ID_SHUFFLE_MUSICFOLDER_PREFIX, ""));
                media = sonosHelper.forShuffleMusicFolder(musicFolderId, count, username, request);
            } else if (id.startsWith(ID_SHUFFLE_ARTIST_PREFIX)) {
                int mediaFileId = Integer.parseInt(id.replace(ID_SHUFFLE_ARTIST_PREFIX, ""));
                media = sonosHelper.forShuffleArtist(mediaFileId, count, username, request);
            } else if (id.startsWith(ID_SHUFFLE_ALBUMLIST_PREFIX)) {
                AlbumListType albumListType = AlbumListType.fromId(id.replace(ID_SHUFFLE_ALBUMLIST_PREFIX, ""));
                media = sonosHelper.forShuffleAlbumList(albumListType, count, username, request);
            } else if (id.startsWith(ID_RADIO_ARTIST_PREFIX)) {
                int mediaFileId = Integer.parseInt(id.replace(ID_RADIO_ARTIST_PREFIX, ""));
                media = sonosHelper.forRadioArtist(mediaFileId, count, username, request);
            } else if (id.startsWith(ID_SIMILAR_ARTISTS_PREFIX)) {
                int mediaFileId = Integer.parseInt(id.replace(ID_SIMILAR_ARTISTS_PREFIX, ""));
                media = sonosHelper.forSimilarArtists(mediaFileId, username, request);
            } else {
                media = sonosHelper.forDirectoryContent(Integer.parseInt(id), username, request);
            }
        }

        if (mediaList == null) {
            mediaList = SonosHelper.createSubList(index, count, media);
        }

        LOG.debug("getMetadata result: id={} index={} count={} total={}", id, mediaList.getIndex(),
                mediaList.getCount(), mediaList.getTotal());

        GetMetadataResponse response = new GetMetadataResponse();
        response.setGetMetadataResult(mediaList);
        return response;
    }

    @Override
    public GetExtendedMetadataResponse getExtendedMetadata(GetExtendedMetadata parameters) {
        LOG.debug("getExtendedMetadata: {}", parameters.getId());

        int id = Integer.parseInt(parameters.getId());
        MediaFile mediaFile = mediaFileService.getMediaFile(id);
        AbstractMedia abstractMedia = sonosHelper.forMediaFile(mediaFile, getUsername(), getRequest());

        ExtendedMetadata extendedMetadata = new ExtendedMetadata();
        if (abstractMedia instanceof MediaCollection) {
            extendedMetadata.setMediaCollection((MediaCollection) abstractMedia);
        } else {
            extendedMetadata.setMediaMetadata((MediaMetadata) abstractMedia);
        }

        RelatedBrowse relatedBrowse = new RelatedBrowse();
        relatedBrowse.setType("RELATED_ARTISTS");
        relatedBrowse.setId(ID_SIMILAR_ARTISTS_PREFIX + id);
        extendedMetadata.getRelatedBrowse().add(relatedBrowse);

        GetExtendedMetadataResponse response = new GetExtendedMetadataResponse();
        response.setGetExtendedMetadataResult(extendedMetadata);
        return response;
    }


    @Override
    public SearchResponse search(Search parameters) {
        String id = parameters.getId();

        IndexType indexType;
        if (ID_SEARCH_ARTISTS.equals(id)) {
            indexType = IndexType.ARTIST;
        } else if (ID_SEARCH_ALBUMS.equals(id)) {
            indexType = IndexType.ALBUM;
        } else if (ID_SEARCH_SONGS.equals(id)) {
            indexType = IndexType.SONG;
        } else {
            throw new IllegalArgumentException("Invalid search category: " + id);
        }

        MediaList mediaList = sonosHelper.forSearch(parameters.getTerm(), parameters.getIndex(),
                                                    parameters.getCount(), indexType, getUsername(), getRequest());
        SearchResponse response = new SearchResponse();
        response.setSearchResult(mediaList);
        return response;
    }

    @Override
    public GetSessionIdResponse getSessionId(GetSessionId parameters) {
        LOG.debug("getSessionId: {}", parameters.getUsername());
        // deprecated and not supported
        throw new SonosSoapFault.LoginInvalid();
    }

    @Override
    public GetMediaMetadataResponse getMediaMetadata(GetMediaMetadata parameters) {
        LOG.debug("getMediaMetadata: {}", parameters.getId());
        GetMediaMetadataResponse response = new GetMediaMetadataResponse();

        try {
            // This method is called whenever a playlist is modified. Don't know why.
            // Return an empty response to avoid ugly log message.
            if (parameters.getId().startsWith(ID_PLAYLIST_PREFIX)) {
                return response;
            }

            int id = Integer.parseInt(parameters.getId());
            MediaFile song = mediaFileService.getMediaFile(id);

            response.setGetMediaMetadataResult(sonosHelper.forSong(song, getUsername(), getRequest()));
        } catch (SecurityException e) {
            LOG.debug("Login denied", e);
            throw new SonosSoapFault.LoginUnauthorized();
        }

        return response;
    }

    @Override
    public void getMediaURI(String id, MediaUriAction action, Integer secondsSinceExplicit, Holder<String> deviceSessionToken,
                            Holder<String> result, Holder<EncryptionContext> deviceSessionKey, Holder<EncryptionContext> contentKey,
                            Holder<HttpHeaders> httpHeaders, Holder<Integer> uriTimeout, Holder<PositionInformation> positionInformation,
                            Holder<String> privateDataFieldName
    ) throws CustomFault {
        result.value = sonosHelper.getMediaURI(Integer.parseInt(id), getUsername(), getRequest());

        LOG.debug("getMediaURI: {} -> {}", id, result.value);
    }

    @Override
    public CreateContainerResult createContainer(String containerType, String title, String parentId, String seedId) {
        Instant now = Instant.now();
        Playlist playlist = new Playlist();
        playlist.setName(title);
        playlist.setUsername(getUsername());
        playlist.setCreated(now);
        playlist.setChanged(now);
        playlist.setShared(false);

        playlistService.createPlaylist(playlist);
        CreateContainerResult result = new CreateContainerResult();
        result.setId(ID_PLAYLIST_PREFIX + playlist.getId());
        addItemToPlaylist(playlist.getId(), seedId, -1);

        return result;
    }

    @Override
    public DeleteContainerResult deleteContainer(String id) {
        if (id.startsWith(ID_PLAYLIST_PREFIX)) {
            int playlistId = Integer.parseInt(id.replace(ID_PLAYLIST_PREFIX, ""));
            Playlist playlist = playlistService.getPlaylist(playlistId);
            if (playlist != null && playlist.getUsername().equals(getUsername())) {
                playlistService.deletePlaylist(playlistId);
            }
        }
        return new DeleteContainerResult();
    }

    @Override
    public RenameContainerResult renameContainer(String id, String title) {
        if (id.startsWith(ID_PLAYLIST_PREFIX)) {
            int playlistId = Integer.parseInt(id.replace(ID_PLAYLIST_PREFIX, ""));
            Playlist playlist = playlistService.getPlaylist(playlistId);
            if (playlist != null && playlist.getUsername().equals(getUsername())) {
                // create a copy to update
                playlist = new Playlist(playlist);
                playlist.setName(title);
                playlistService.updatePlaylist(playlist);
            }
        }
        return new RenameContainerResult();
    }

    @Override
    public AddToContainerResult addToContainer(String id, String parentId, int index, String updateId) {
        if (parentId.startsWith(ID_PLAYLIST_PREFIX)) {
            int playlistId = Integer.parseInt(parentId.replace(ID_PLAYLIST_PREFIX, ""));
            Playlist playlist = playlistService.getPlaylist(playlistId);
            if (playlist != null && playlist.getUsername().equals(getUsername())) {
                addItemToPlaylist(playlistId, id, index);
            }
        }
        return new AddToContainerResult();
    }

    private void addItemToPlaylist(int playlistId, String id, int index) {
        if (StringUtils.isBlank(id)) {
            return;
        }

        GetMetadata parameters = new GetMetadata();
        parameters.setId(id);
        parameters.setIndex(0);
        parameters.setCount(Integer.MAX_VALUE);
        GetMetadataResponse metadata = getMetadata(parameters);
        List<MediaFile> newSongs = new ArrayList<MediaFile>();

        for (AbstractMedia media : metadata.getGetMetadataResult().getMediaCollectionOrMediaMetadata()) {
            if (StringUtils.isNumeric(media.getId())) {
                MediaFile mediaFile = mediaFileService.getMediaFile(Integer.parseInt(media.getId()));
                if (mediaFile != null && mediaFile.isFile()) {
                    newSongs.add(mediaFile);
                }
            }
        }
        List<MediaFile> existingSongs = playlistService.getFilesInPlaylist(playlistId);
        if (index == -1) {
            index = existingSongs.size();
        }

        existingSongs.addAll(index, newSongs);
        playlistService.setFilesInPlaylist(playlistId, existingSongs);
    }

    @Override
    public ReorderContainerResult reorderContainer(String id, String from, int to, String updateId) {
        if (id.startsWith(ID_PLAYLIST_PREFIX)) {
            int playlistId = Integer.parseInt(id.replace(ID_PLAYLIST_PREFIX, ""));
            Playlist playlist = playlistService.getPlaylist(playlistId);
            if (playlist != null && playlist.getUsername().equals(getUsername())) {

                SortedMap<Integer, MediaFile> indexToSong = new ConcurrentSkipListMap<Integer, MediaFile>();
                List<MediaFile> songs = playlistService.getFilesInPlaylist(playlistId);
                for (int i = 0; i < songs.size(); i++) {
                    indexToSong.put(i, songs.get(i));
                }

                List<MediaFile> movedSongs = new ArrayList<MediaFile>();
                for (Integer i : parsePlaylistIndices(from)) {
                    movedSongs.add(indexToSong.remove(i));
                }

                List<MediaFile> updatedSongs = new ArrayList<MediaFile>();
                updatedSongs.addAll(indexToSong.headMap(to).values());
                updatedSongs.addAll(movedSongs);
                updatedSongs.addAll(indexToSong.tailMap(to).values());

                playlistService.setFilesInPlaylist(playlistId, updatedSongs);
            }
        }
        return new ReorderContainerResult();
    }

    @Override
    public RemoveFromContainerResult removeFromContainer(String id, String indices, String updateId) {
        if (id.startsWith(ID_PLAYLIST_PREFIX)) {
            int playlistId = Integer.parseInt(id.replace(ID_PLAYLIST_PREFIX, ""));
            Playlist playlist = playlistService.getPlaylist(playlistId);
            if (playlist != null && playlist.getUsername().equals(getUsername())) {
                SortedSet<Integer> indicesToRemove = parsePlaylistIndices(indices);
                List<MediaFile> songs = playlistService.getFilesInPlaylist(playlistId);
                List<MediaFile> updatedSongs = new ArrayList<MediaFile>();
                for (int i = 0; i < songs.size(); i++) {
                    if (!indicesToRemove.contains(i)) {
                        updatedSongs.add(songs.get(i));
                    }
                }
                playlistService.setFilesInPlaylist(playlistId, updatedSongs);
            }
        }
        return new RemoveFromContainerResult();
    }

    protected SortedSet<Integer> parsePlaylistIndices(String indices) {
        // Comma-separated, may include ranges:  1,2,4-7
        SortedSet<Integer> result = new TreeSet<Integer>();

        for (String part : StringUtils.split(indices, ',')) {
            if (StringUtils.isNumeric(part)) {
                result.add(Integer.parseInt(part));
            } else {
                int dashIndex = part.indexOf('-');
                int from = Integer.parseInt(part.substring(0, dashIndex));
                int to = Integer.parseInt(part.substring(dashIndex + 1));
                for (int i = from; i <= to; i++) {
                    result.add(i);
                }
            }
        }
        return result;
    }

    @Override
    public String createItem(String favorite) {
        int id = Integer.parseInt(favorite);
        sonosHelper.star(id, getUsername());
        return favorite;
    }

    @Override
    public void deleteItem(String favorite) {
        int id = Integer.parseInt(favorite);
        sonosHelper.unstar(id, getUsername());
    }

    private HttpServletRequest getRequest() {
        MessageContext messageContext = context == null ? null : context.getMessageContext();

        return messageContext == null ? null : (HttpServletRequest) messageContext.get(AbstractHTTPDestination.HTTP_REQUEST);
    }

    private SoapMessage getMessage() {
        MessageContext messageContext = context == null ? null : context.getMessageContext();
        if (messageContext == null || !(messageContext instanceof WrappedMessageContext)) {
            return null;
        }
        return (SoapMessage) ((WrappedMessageContext) messageContext).getWrappedMessage();
    }

    private String getUsername() {
        return context.getUserPrincipal().getName();
    }

    public void setSonosHelper(SonosHelper sonosHelper) {
        this.sonosHelper = sonosHelper;
    }

    @Override
    public RateItemResponse rateItem(RateItem parameters) {
        return null;
    }

    @Override
    public SegmentMetadataList getStreamingMetadata(String id, XMLGregorianCalendar startTime, int duration) {
        return null;
    }

    @Override
    public GetExtendedMetadataTextResponse getExtendedMetadataText(GetExtendedMetadataText parameters) {
        return null;
    }

    @Override
    public DeviceLinkCodeResult getDeviceLinkCode(String householdId) {
        return null;
    }

    private Cache<String, Triple<String, String, Instant>> sonosLinkCache = CacheBuilder.newBuilder().expireAfterWrite(7, TimeUnit.MINUTES).<String, Triple<String, String, Instant>>build();

    // The link code must be exactly 32 characters long
    private String createLinkCode() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    // The refresh token must be up to 2048 chars
    private String createRefreshToken() {
        return UUID.randomUUID().toString();
    }

    private String generateLinkCode(String householdId, String sonosAppName, Instant initiated) {
        String linkCode = createLinkCode();
        sonosLinkCache.put(linkCode, Triple.of(householdId, sonosAppName, initiated));
        return linkCode;
    }

    public Triple<String, String, Instant> getInitiatedLinkCodeData(String linkCode) {
        return sonosLinkCache.getIfPresent(linkCode);
    }

    public boolean addSonosAuthorization(String username, String linkcode, String householdId, String sonosApp, Instant initiated) {
        if (sonosLinkDao.findByLinkcode(linkcode) != null) {
            return false;
        }

        sonosLinkDao.create(new SonosLink(username, linkcode, householdId, sonosApp, initiated));
        sonosLinkCache.invalidate(linkcode);
        return true;
    }

    public List<SonosLink> getExistingSonosLinks() {
        return sonosLinkDao.getAll();
    }

    public Map<String, Triple<String, String, Instant>> getPendingSonosLinks() {
        return sonosLinkCache.asMap();
    }

    @Override
    public AppLinkResult getAppLink(String householdId, String hardware, String osVersion, String sonosAppName, String callbackPath) throws CustomFault {
        AppLinkResult result = new AppLinkResult();

        result.setAuthorizeAccount(new AppLinkInfo());
        result.getAuthorizeAccount().setAppUrlStringId("appUrlStringId");
        DeviceLinkCodeResult linkCodeResult = new DeviceLinkCodeResult();

        String linkCode = generateLinkCode(householdId, sonosAppName, Instant.now());
        linkCodeResult.setLinkCode(linkCode);
        linkCodeResult.setRegUrl(settingsService.getSonosCallbackHostAddress() + "sonoslink?linkCode=" + linkCode);
        linkCodeResult.setShowLinkCode(false);

        result.getAuthorizeAccount().setDeviceLink(linkCodeResult);

        return result;
    }

    @Override
    public void reportAccountAction(String type) throws CustomFault {

    }

    @Override
    public void setPlayedSeconds(String id, int seconds, String contextId, String privateData, Integer offsetMillis) throws CustomFault {

    }

    @Override
    public ReportPlaySecondsResult reportPlaySeconds(String id, int seconds, String contextId, String privateData, Integer offsetMillis) throws CustomFault {
        return null;
    }

    @Override
    public DeviceAuthTokenResult getDeviceAuthToken(String householdId, String linkCode, String linkDeviceId, String callbackPath) throws CustomFault {
        LOG.debug("Get device auth token for householdid {} and linkcode {}.", householdId, linkCode);

        SonosLink sonosLink = sonosLinkDao.findByLinkcode(linkCode);
        if (sonosLink != null && householdId.equals(sonosLink.getHouseholdId())) {
            return createAuthToken(sonosLink, getRequest());
        } else {
            throw new SonosSoapFault.NotLinkedRetry();
        }
    }

    @Override
    public void reportStatus(String id, int errorCode, String message) {
    }

    @Override
    public String getScrollIndices(String id) {
        return null;
    }

    @Override
    public void reportPlayStatus(String id, String status, String contextId, Integer offsetMillis) throws CustomFault {

    }

    @Override
    public ContentKey getContentKey(String id, String uri, String deviceSessionToken) throws CustomFault {
        return null;
    }

    @Override
    public DeviceAuthTokenResult refreshAuthToken() throws CustomFault {
        try {
            Credentials expiredCreds = SonosLinkSecurityInterceptor.getCredentials(getMessage());
            return refreshAuthToken(expiredCreds, getRequest());
        } catch (Exception e) {
            throw new SonosSoapFault.LoginInvalid();
        }
    }

    public DeviceAuthTokenResult refreshAuthToken(Credentials expiredCreds, HttpServletRequest request) {
        Pair<SonosLink, String> jwtSonosLink = sonosHelper.getSonosLinkFromJWT(expiredCreds.getLoginToken().getToken());
        if (StringUtils.equals(jwtSonosLink.getRight(), expiredCreds.getLoginToken().getKey())
                && StringUtils.equals(jwtSonosLink.getLeft().getHouseholdId(), expiredCreds.getLoginToken().getHouseholdId())
                && jwtSonosLink.getLeft().equals(sonosLinkDao.findByLinkcode(jwtSonosLink.getLeft().getLinkcode()))) {
            return createAuthToken(jwtSonosLink.getLeft(), request);
        } else {
            throw new SonosSoapFault.LoginInvalid();
        }
    }

    public DeviceAuthTokenResult createAuthToken(SonosLink sonosLink, HttpServletRequest req) {
        String refreshToken = createRefreshToken();
        String authToken = sonosHelper.createJwt(sonosLink, req.getRequestURI().substring(req.getContextPath().length() + 1) + "?" + req.getQueryString(), refreshToken);

        DeviceAuthTokenResult authTokenResult = new DeviceAuthTokenResult();
        authTokenResult.setAuthToken(authToken);
        authTokenResult.setPrivateKey(refreshToken);

        authTokenResult.setUserInfo(new UserInfo());
        authTokenResult.getUserInfo().setNickname(sonosLink.getUsername());

        return authTokenResult;
    }

    @Override
    public UserInfo getUserInfo() throws CustomFault {
        UserInfo info = new UserInfo();
        info.setNickname(getUsername());
        return info;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setUpnpService(UPnPService upnpService) {
        this.upnpService = upnpService;
    }

    public void setPlaylistService(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }
}
