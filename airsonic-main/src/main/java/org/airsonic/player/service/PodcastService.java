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

import org.airsonic.player.dao.PodcastDao;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.PodcastChannel;
import org.airsonic.player.domain.PodcastChannelRule;
import org.airsonic.player.domain.PodcastEpisode;
import org.airsonic.player.domain.PodcastExportOPML;
import org.airsonic.player.domain.PodcastStatus;
import org.airsonic.player.service.metadata.MetaData;
import org.airsonic.player.service.metadata.MetaDataParser;
import org.airsonic.player.service.metadata.MetaDataParserFactory;
import org.airsonic.player.util.FileUtil;
import org.airsonic.player.util.StringUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.airsonic.player.util.XMLUtil.createSAXBuilder;

/**
 * Provides services for Podcast reception.
 *
 * @author Sindre Mehus
 */
@Service
public class PodcastService {

    private static final Logger LOG = LoggerFactory.getLogger(PodcastService.class);

    private static final DateTimeFormatter ALTERNATIVE_RSS_DATE_FORMAT = DateTimeFormatter
            .ofPattern("[E, ]d MMM y HH:mm:ss z");
    private static final Namespace[] ITUNES_NAMESPACES = {Namespace.getNamespace("http://www.itunes.com/DTDs/Podcast-1.0.dtd"),
        Namespace.getNamespace("http://www.itunes.com/dtds/podcast-1.0.dtd")};

    private final ExecutorService refreshExecutor;
    private final ExecutorService downloadExecutor;
    private final ScheduledExecutorService scheduledExecutor;
    private final Map<Integer, ScheduledFuture<?>> scheduledRefreshes = new ConcurrentHashMap<>();
    @Autowired
    private PodcastDao podcastDao;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private MediaFileService mediaFileService;
    @Autowired
    private MetaDataParserFactory metaDataParserFactory;
    @Autowired
    private VersionService versionService;

    public PodcastService() {
        ThreadFactory threadFactory = r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            return t;
        };
        refreshExecutor = Executors.newFixedThreadPool(5, threadFactory);
        downloadExecutor = Executors.newFixedThreadPool(3, threadFactory);
        scheduledExecutor = Executors.newScheduledThreadPool(10, threadFactory);
    }

    @PostConstruct
    public void init() {
        try {
            // Clean up partial downloads.
            getAllChannels()
                .parallelStream()
                .map(PodcastChannel::getId)
                .map(this::getEpisodes)
                .flatMap(List::parallelStream)
                .filter(e -> e.getStatus() == PodcastStatus.DOWNLOADING)
                .forEach(e -> {
                    deleteEpisode(e, false);
                    LOG.info("Deleted Podcast episode '{}' since download was interrupted.", e.getTitle());
                });
            schedule();
        } catch (Throwable x) {
            LOG.error("Failed to initialize PodcastService", x);
        }
    }

    public synchronized void schedule() {
        // schedule for podcasts with rules
        podcastDao.getAllChannelRules().forEach(this::schedule);

        // default refresh for rest of the podcasts
        scheduleDefault();
    }

    private synchronized void schedule(PodcastChannelRule r) {
        unschedule(r.getId());

        int hoursBetween = r.getCheckInterval();

        if (hoursBetween == -1) {
            LOG.info("Automatic Podcast update disabled for podcast id {}", r.getId());
            return;
        }

        long periodMillis = hoursBetween * 60L * 60L * 1000L;
        long initialDelayMillis = 5L * 60L * 1000L;

        Runnable task = () -> {
            LOG.info("Starting scheduled Podcast refresh for podcast id {}.", r.getId());
            refreshChannel(r.getId(), true);
            LOG.info("Completed scheduled Podcast refresh for podcast id {}.", r.getId());
        };

        scheduledRefreshes.put(r.getId(), scheduledExecutor.scheduleAtFixedRate(task, initialDelayMillis, periodMillis, TimeUnit.MILLISECONDS));

        Instant firstTime = Instant.now().plusMillis(initialDelayMillis);
        LOG.info("Automatic Podcast update for podcast id {} scheduled to run every {} hour(s), starting at {}", r.getId(), hoursBetween, firstTime);
    }

    public synchronized void scheduleDefault() {
        unschedule(-1);

        int hoursBetween = settingsService.getPodcastUpdateInterval();

        if (hoursBetween == -1) {
            LOG.info("Automatic default Podcast update disabled for podcasts");
            return;
        }

        long periodMillis = hoursBetween * 60L * 60L * 1000L;
        long initialDelayMillis = 5L * 60L * 1000L;

        Runnable task = () -> {
            LOG.info("Starting scheduled default Podcast refresh.");
            Set<Integer> ruleIds = podcastDao.getAllChannelRules().parallelStream().map(r -> r.getId()).collect(toSet());
            List<PodcastChannel> channelsWithoutRules = podcastDao.getAllChannels().parallelStream().filter(c -> !ruleIds.contains(c.getId())).collect(toList());
            refreshChannels(channelsWithoutRules, true);
            LOG.info("Completed scheduled default Podcast refresh.");
        };

        scheduledRefreshes.put(-1, scheduledExecutor.scheduleAtFixedRate(task, initialDelayMillis, periodMillis, TimeUnit.MILLISECONDS));

        Instant firstTime = Instant.now().plusMillis(initialDelayMillis);
        LOG.info("Automatic default Podcast update scheduled to run every {} hour(s), starting at {}", hoursBetween, firstTime);
    }

    public void unschedule(Integer id) {
        ScheduledFuture<?> scheduledRefresh = scheduledRefreshes.remove(id);
        if (scheduledRefresh != null) {
            scheduledRefresh.cancel(true);
        }
    }

    public void createOrUpdateChannelRule(PodcastChannelRule r) {
        podcastDao.createOrUpdateChannelRule(r);
        schedule(r);
    }

    public void deleteChannelRule(Integer id) {
        podcastDao.deleteChannelRule(id);
        unschedule(id);
    }

    public PodcastChannelRule getChannelRule(Integer id) {
        return podcastDao.getChannelRule(id);
    }

    public List<PodcastChannelRule> getAllChannelRules() {
        return podcastDao.getAllChannelRules();
    }

    /**
     * Creates a new Podcast channel.
     *
     * @param url The URL of the Podcast channel.
     */
    public void createChannel(String url) {
        PodcastChannel channel = new PodcastChannel(sanitizeUrl(url, false));
        int channelId = podcastDao.createChannel(channel);

        refreshChannels(Collections.singletonList(getChannel(channelId)), true);
    }

    private static String sanitizeUrl(String url, boolean force) {
        if (!StringUtils.contains(url, "://") || force) {
            return URLDecoder.decode(url, StandardCharsets.UTF_8);
        }
        return url;
    }

    /**
     * Returns a single Podcast channel.
     */
    public PodcastChannel getChannel(int channelId) {
        PodcastChannel channel = podcastDao.getChannel(channelId);
        if (channel.getTitle() != null)
            addMediaFileIdToChannels(Collections.singletonList(channel));
        return channel;
    }

    /**
     * Returns all Podcast channels.
     *
     * @return Possibly empty list of all Podcast channels.
     */
    public List<PodcastChannel> getAllChannels() {
        return addMediaFileIdToChannels(podcastDao.getAllChannels());
    }

    private PodcastEpisode getEpisodeByUrl(String url) {
        PodcastEpisode episode = podcastDao.getEpisodeByUrl(url);
        if (episode == null) {
            return null;
        }
        List<PodcastEpisode> episodes = Collections.singletonList(episode);
        episodes = filterAllowed(episodes);
        addMediaFileIdToEpisodes(episodes);
        return episodes.isEmpty() ? null : episodes.get(0);
    }

    /**
     * Returns all Podcast episodes for a given channel.
     *
     * @param channelId      The Podcast channel ID.
     * @return Possibly empty list of all Podcast episodes for the given channel, sorted in
     *         reverse chronological order (newest episode first).
     */
    public List<PodcastEpisode> getEpisodes(int channelId) {
        List<PodcastEpisode> episodes = filterAllowed(podcastDao.getEpisodes(channelId));
        return addMediaFileIdToEpisodes(episodes);
    }

    /**
     * Returns the N newest episodes.
     *
     * @return Possibly empty list of the newest Podcast episodes, sorted in
     *         reverse chronological order (newest episode first).
     */
    public List<PodcastEpisode> getNewestEpisodes(int count) {
        return addMediaFileIdToEpisodes(podcastDao.getNewestEpisodes(count)).stream().filter(episode -> {
            Integer mediaFileId = episode.getMediaFileId();
            if (mediaFileId == null) {
                return false;
            }
            MediaFile mediaFile = mediaFileService.getMediaFile(mediaFileId);
            return mediaFile != null && mediaFile.isPresent();
        }).collect(Collectors.toList());
    }

    private List<PodcastEpisode> filterAllowed(List<PodcastEpisode> episodes) {
        return episodes.stream().filter(episode -> episode.getPath() == null || securityService.isReadAllowed(Paths.get(episode.getPath()))).collect(Collectors.toList());
    }

    public PodcastEpisode getEpisode(int episodeId, boolean includeDeleted) {
        PodcastEpisode episode = podcastDao.getEpisode(episodeId);
        if (episode == null) {
            return null;
        }
        if (episode.getStatus() == PodcastStatus.DELETED && !includeDeleted) {
            return null;
        }
        addMediaFileIdToEpisodes(Collections.singletonList(episode));
        return episode;
    }

    private List<PodcastEpisode> addMediaFileIdToEpisodes(List<PodcastEpisode> episodes) {
        return episodes.stream().map(episode -> {
            if (episode.getPath() != null) {
                MediaFile mediaFile = mediaFileService.getMediaFile(episode.getPath());
                if (mediaFile != null && mediaFile.isPresent()) {
                    episode.setMediaFileId(mediaFile.getId());
                }
            }

            return episode;
        }).collect(Collectors.toList());
    }

    private List<PodcastChannel> addMediaFileIdToChannels(List<PodcastChannel> channels) {
        return channels.stream().map(channel -> {
            try {
                if (channel.getTitle() == null) {
                    LOG.warn("Podcast channel id {} has null title", channel.getId());
                } else {
                    Path dir = getChannelDirectory(channel);
                    MediaFile mediaFile = mediaFileService.getMediaFile(dir);
                    if (mediaFile != null) {
                        channel.setMediaFileId(mediaFile.getId());
                    }
                }

            } catch (Exception x) {
                LOG.warn("Failed to resolve media file ID for podcast channel '{}'", channel.getTitle(), x);
            }

            return channel;
        }).collect(Collectors.toList());
    }

    public PodcastExportOPML export(List<PodcastChannel> channels) {
        PodcastExportOPML opml = new PodcastExportOPML();
        channels.forEach(c -> {
            PodcastExportOPML.Outline outline = new PodcastExportOPML.Outline();
            outline.setText(c.getTitle());
            outline.setXmlUrl(c.getUrl());
            opml.getBody().getOutline().get(0).getOutline().add(outline);
        });

        return opml;
    }

    public void refreshChannel(int channelId, boolean downloadEpisodes) {
        refreshChannels(Arrays.asList(getChannel(channelId)), downloadEpisodes);
    }

    public void refreshAllChannels(boolean downloadEpisodes) {
        refreshChannels(getAllChannels(), downloadEpisodes);
    }

    private void refreshChannels(final List<PodcastChannel> channels, final boolean downloadEpisodes) {
        for (final PodcastChannel channel : channels) {
            Runnable task = () -> doRefreshChannel(channel, downloadEpisodes);
            refreshExecutor.submit(task);
        }
    }

    private void doRefreshChannel(PodcastChannel channel, boolean downloadEpisodes) {
        channel.setStatus(PodcastStatus.DOWNLOADING);
        channel.setErrorMessage(null);
        podcastDao.updateChannel(channel);
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(2 * 60 * 1000) // 2 minutes
                .setSocketTimeout(10 * 60 * 1000) // 10 minutes
                .build();
        HttpGet method = new HttpGet(channel.getUrl());
        method.setConfig(requestConfig);
        method.addHeader("User-Agent", "Airsonic/" + versionService.getLocalVersion());
        try (CloseableHttpClient client = HttpClients.createDefault();
                CloseableHttpResponse response = client.execute(method);
                InputStream in = response.getEntity().getContent()) {

            Document document = createSAXBuilder().build(in);
            Element channelElement = document.getRootElement().getChild("channel");

            channel.setTitle(StringUtil.removeMarkup(channelElement.getChildTextTrim("title")));
            channel.setDescription(StringUtil.removeMarkup(channelElement.getChildTextTrim("description")));
            channel.setImageUrl(sanitizeUrl(getChannelImageUrl(channelElement), false));
            channel.setStatus(PodcastStatus.COMPLETED);
            channel.setErrorMessage(null);
            podcastDao.updateChannel(channel);

            downloadImage(channel);
            refreshEpisodes(channel, channelElement.getChildren("item"));
        } catch (Exception x) {
            LOG.warn("Failed to get/parse RSS file for Podcast channel {}", channel.getUrl(), x);
            channel.setStatus(PodcastStatus.ERROR);
            channel.setErrorMessage(getErrorMessage(x));
            podcastDao.updateChannel(channel);
        }

        if (downloadEpisodes) {
            getEpisodes(channel.getId())
                .parallelStream()
                .filter(episode -> episode.getStatus() == PodcastStatus.NEW && episode.getUrl() != null)
                .forEach(this::downloadEpisode);
        }
    }

    private void downloadImage(PodcastChannel channel) {
        String imageUrl = channel.getImageUrl();
        if (imageUrl == null) {
            return;
        }

        Path dir = getChannelDirectory(channel);
        MediaFile channelMediaFile = mediaFileService.getMediaFile(dir);
        Path existingCoverArt = mediaFileService.getCoverArt(channelMediaFile);
        boolean imageFileExists = existingCoverArt != null && mediaFileService.getMediaFile(existingCoverArt) == null;
        if (imageFileExists) {
            return;
        }

        HttpGet method = new HttpGet(imageUrl);
        method.addHeader("User-Agent", "Airsonic/" + versionService.getLocalVersion());
        try (CloseableHttpClient client = HttpClients.createDefault();
                CloseableHttpResponse response = client.execute(method);
                InputStream in = response.getEntity().getContent()) {
            Files.copy(in, dir.resolve("cover." + getCoverArtSuffix(response)), StandardCopyOption.REPLACE_EXISTING);
            mediaFileService.refreshMediaFile(channelMediaFile);
        } catch (Exception x) {
            LOG.warn("Failed to download cover art for podcast channel '{}'", channel.getTitle(), x);
        }
    }

    private String getCoverArtSuffix(HttpResponse response) {
        String result = null;
        Header contentTypeHeader = response.getEntity().getContentType();
        if (contentTypeHeader != null && contentTypeHeader.getValue() != null) {
            ContentType contentType = ContentType.parse(contentTypeHeader.getValue());
            String mimeType = contentType.getMimeType();
            result = StringUtil.getSuffix(mimeType);
        }
        return result == null ? "jpeg" : result;
    }

    private String getChannelImageUrl(Element channelElement) {
        String result = getITunesAttribute(channelElement, "image", "href");
        if (result == null) {
            Element imageElement = channelElement.getChild("image");
            if (imageElement != null) {
                result = imageElement.getChildTextTrim("url");
            }
        }
        return result;
    }

    private String getErrorMessage(Exception x) {
        return x.getMessage() != null ? x.getMessage() : x.toString();
    }

    public void downloadEpisode(final PodcastEpisode episode) {
        Runnable task = () -> doDownloadEpisode(episode);
        downloadExecutor.submit(task);
    }

    private void refreshEpisodes(PodcastChannel channel, List<Element> episodeElements) {
        // Create episodes in database, skipping the proper number of episodes.
        int downloadCount = Optional.ofNullable(podcastDao.getChannelRule(channel.getId()))
                .map(cr -> cr.getDownloadCount())
                .orElseGet(() -> settingsService.getPodcastEpisodeDownloadCount());
        if (downloadCount == -1) {
            downloadCount = Integer.MAX_VALUE;
        }

        AtomicInteger counter = new AtomicInteger(downloadCount);

        episodeElements.parallelStream()
                .map(episodeElement -> {
                    String title = StringUtil.removeMarkup(episodeElement.getChildTextTrim("title"));

                    Element enclosure = episodeElement.getChild("enclosure");
                    if (enclosure == null) {
                        LOG.info("No enclosure found for episode {}", title);
                        return null;
                    }

                    String url = sanitizeUrl(enclosure.getAttributeValue("url"), false);
                    if (url == null) {
                        LOG.info("No enclosure URL found for episode {}", title);
                        return null;
                    }

                    if (getEpisodeByUrl(url) != null) {
                        LOG.info("Episode already exists for episode {}", title);
                        return null;
                    }

                    String duration = formatDuration(getITunesElement(episodeElement, "duration"));
                    String description = StringUtil.removeMarkup(episodeElement.getChildTextTrim("description"));
                    if (StringUtils.isBlank(description)) {
                        description = getITunesElement(episodeElement, "summary");
                    }

                    Long length = null;
                    try {
                        length = Long.valueOf(enclosure.getAttributeValue("length"));
                    } catch (Exception x) {
                        LOG.warn("Failed to parse enclosure length.", x);
                    }

                    Instant date = parseDate(episodeElement.getChildTextTrim("pubDate"));
                    PodcastEpisode episode = new PodcastEpisode(null, channel.getId(), url, null, title, description, date,
                            duration, length, 0L, PodcastStatus.NEW, null);
                    LOG.info("Created Podcast episode {}", title);

                    return episode;
                })
                .filter(Objects::nonNull)
                // Sort episode in reverse chronological order (newest first)
                .sorted(Comparator.comparingLong(k -> k.getPublishDate() == null ? 0L : -k.getPublishDate().toEpochMilli()))
                .forEach(episode -> {
                    if (counter.decrementAndGet() < 0) {
                        episode.setStatus(PodcastStatus.SKIPPED);
                    }
                    podcastDao.createEpisode(episode);
                });
    }

    private Instant parseDate(String s) {
        try {
            return OffsetDateTime.parse(s, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
        } catch (Exception x) {
            try {
                return ZonedDateTime.parse(s, ALTERNATIVE_RSS_DATE_FORMAT).toInstant();
            } catch (Exception e) {
                LOG.warn("Failed to parse publish date: {}", s);
                return null;
            }
        }
    }

    private String formatDuration(String duration) {
        if (duration == null) return null;
        if (duration.matches("^\\d+$")) {
            long seconds = Long.valueOf(duration);
            return StringUtil.formatDuration(seconds * 1000);
        } else {
            return duration;
        }
    }

    private String getITunesElement(Element element, String childName) {
        for (Namespace ns : ITUNES_NAMESPACES) {
            String value = element.getChildTextTrim(childName, ns);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String getITunesAttribute(Element element, String childName, String attributeName) {
        for (Namespace ns : ITUNES_NAMESPACES) {
            Element elem = element.getChild(childName, ns);
            if (elem != null) {
                return StringUtils.trimToNull(elem.getAttributeValue(attributeName));
            }
        }
        return null;
    }

    private void doDownloadEpisode(PodcastEpisode episode) {
        if (isEpisodeDeleted(episode)) {
            LOG.info("Podcast {} was deleted. Aborting download.", episode.getUrl());
            return;
        }

        LOG.info("Starting to download Podcast from {}", episode.getUrl());

        PodcastChannel channel = getChannel(episode.getChannelId());
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(2 * 60 * 1000) // 2 minutes
                .setSocketTimeout(10 * 60 * 1000) // 10 minutes
                // Workaround HttpClient circular redirects, which some feeds use (with query parameters)
                .setCircularRedirectsAllowed(true)
                // Workaround HttpClient not understanding latest RFC-compliant cookie 'expires' attributes
                .setCookieSpec(CookieSpecs.STANDARD)
                .build();
        HttpGet method = new HttpGet(episode.getUrl());
        method.setConfig(requestConfig);
        method.addHeader("User-Agent", "Airsonic/" + versionService.getLocalVersion());
        Path file = getFile(channel, episode);

        try (CloseableHttpClient client = HttpClients.createDefault();
                CloseableHttpResponse response = client.execute(method);
                InputStream in = response.getEntity().getContent();
                OutputStream out = new BufferedOutputStream(Files.newOutputStream(file))) {

            episode.setStatus(PodcastStatus.DOWNLOADING);
            episode.setBytesDownloaded(0L);
            episode.setErrorMessage(null);
            episode.setPath(file.toString());
            podcastDao.updateEpisode(episode);

            byte[] buffer = new byte[8192];
            long bytesDownloaded = 0;
            int n;
            long nextLogCount = 30000L;

            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
                bytesDownloaded += n;

                if (bytesDownloaded > nextLogCount) {
                    episode.setBytesDownloaded(bytesDownloaded);
                    nextLogCount += 30000L;

                    // Abort download if episode was deleted by user.
                    if (isEpisodeDeleted(episode)) {
                        break;
                    }
                    podcastDao.updateEpisode(episode);
                }
            }

            if (isEpisodeDeleted(episode)) {
                LOG.info("Podcast {} was deleted. Aborting download.", episode.getUrl());
                FileUtil.closeQuietly(out);
                FileUtil.delete(file);
            } else {
                addMediaFileIdToEpisodes(Collections.singletonList(episode));
                episode.setBytesDownloaded(bytesDownloaded);
                podcastDao.updateEpisode(episode);
                LOG.info("Downloaded {} bytes from Podcast {}", bytesDownloaded, episode.getUrl());
                FileUtil.closeQuietly(out);
                updateTags(file, episode);
                episode.setStatus(PodcastStatus.COMPLETED);
                podcastDao.updateEpisode(episode);
                deleteObsoleteEpisodes(channel);
            }
        } catch (Exception x) {
            LOG.warn("Failed to download Podcast from {}", episode.getUrl(), x);
            episode.setStatus(PodcastStatus.ERROR);
            episode.setErrorMessage(getErrorMessage(x));
            podcastDao.updateEpisode(episode);
        }
    }

    private boolean isEpisodeDeleted(PodcastEpisode episode) {
        episode = podcastDao.getEpisode(episode.getId());
        return episode == null || episode.getStatus() == PodcastStatus.DELETED;
    }

    private void updateTags(Path file, PodcastEpisode episode) {
        try {
            MediaFile mediaFile = mediaFileService.getMediaFile(file, false);
            if (StringUtils.isNotBlank(episode.getTitle())) {
                MetaDataParser parser = metaDataParserFactory.getParser(mediaFile.getFile());
                if (!parser.isEditingSupported()) {
                    return;
                }
                MetaData metaData = parser.getRawMetaData(file);
                metaData.setTitle(episode.getTitle());
                parser.setMetaData(mediaFile, metaData);
                mediaFileService.refreshMediaFile(mediaFile);
            }
        } catch (Exception x) {
            LOG.warn("Failed to update tags for podcast {}", episode.getUrl(), x);
        }
    }

    private synchronized void deleteObsoleteEpisodes(PodcastChannel channel) {
        int episodeCount = Optional.ofNullable(podcastDao.getChannelRule(channel.getId()))
                .map(cr -> cr.getRetentionCount())
                .orElseGet(() -> settingsService.getPodcastEpisodeRetentionCount());
        if (episodeCount == -1) {
            return;
        }

        List<PodcastEpisode> episodes = getEpisodes(channel.getId());

        // Don't do anything if other episodes of the same channel is currently downloading.
        if (episodes.parallelStream().anyMatch(episode -> episode.getStatus() == PodcastStatus.DOWNLOADING)) {
            return;
        }

        int numEpisodes = episodes.size();
        int episodesToDelete = Math.max(0, numEpisodes - episodeCount);
        // Delete in reverse to get chronological order (oldest episodes first).
        for (int i = 0; i < episodesToDelete; i++) {
            deleteEpisode(episodes.get(numEpisodes - 1 - i), true);
            LOG.info("Deleted old Podcast episode {}", episodes.get(numEpisodes - 1 - i).getUrl());
        }
    }

    private synchronized Path getFile(PodcastChannel channel, PodcastEpisode episode) {

        Path channelDir = getChannelDirectory(channel);

        String filename = StringUtil.getUrlFile(sanitizeUrl(episode.getUrl(), true));
        if (filename == null) {
            filename = episode.getTitle();
        }
        filename = StringUtil.fileSystemSafe(filename);
        String extension = FilenameUtils.getExtension(filename);
        filename = FilenameUtils.removeExtension(filename);
        if (StringUtils.isBlank(extension)) {
            extension = "mp3";
        }

        Path file = channelDir.resolve(filename + "." + extension);
        for (int i = 0; Files.exists(file); i++) {
            file = channelDir.resolve(filename + i + "." + extension);
        }

        if (!securityService.isWriteAllowed(file)) {
            throw new SecurityException("Access denied to file " + file);
        }
        return file;
    }

    private Path getChannelDirectory(PodcastChannel channel) {
        Path podcastDir = Paths.get(settingsService.getPodcastFolder());
        Path channelDir = podcastDir.resolve(StringUtil.fileSystemSafe(channel.getTitle()));

        if (!Files.isWritable(podcastDir)) {
            throw new RuntimeException("The podcasts directory " + podcastDir + " isn't writeable.");
        }

        if (!Files.exists(channelDir)) {
            try {
                Files.createDirectories(channelDir);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create directory " + channelDir, e);
            }

            MediaFile mediaFile = mediaFileService.getMediaFile(channelDir);
            mediaFile.setComment(channel.getDescription());
            mediaFileService.updateMediaFile(mediaFile);
        }

        return channelDir;
    }

    /**
     * Deletes the Podcast channel with the given ID.
     *
     * @param channelId The Podcast channel ID.
     */
    public void deleteChannel(int channelId) {
        // Delete all associated episodes (in case they have files that need to be deleted).
        getEpisodes(channelId).parallelStream().forEach(ep -> deleteEpisode(ep, false));

        podcastDao.deleteChannel(channelId);
    }

    /**
     * Deletes the Podcast episode with the given ID.
     *
     * @param episodeId     The Podcast episode ID.
     * @param logicalDelete Whether to perform a logical delete by setting the
     *                      episode status to {@link PodcastStatus#DELETED}.
     */
    public void deleteEpisode(int episodeId, boolean logicalDelete) {
        deleteEpisode(podcastDao.getEpisode(episodeId), logicalDelete);
    }

    public void deleteEpisode(PodcastEpisode episode, boolean logicalDelete) {
        if (episode == null) {
            return;
        }

        // Delete file.
        if (episode.getPath() != null) {
            FileUtil.delete(Paths.get(episode.getPath()));
        }

        if (logicalDelete) {
            episode.setStatus(PodcastStatus.DELETED);
            episode.setErrorMessage(null);
            podcastDao.updateEpisode(episode);
        } else {
            podcastDao.deleteEpisode(episode.getId());
        }
    }

    public void setPodcastDao(PodcastDao podcastDao) {
        this.podcastDao = podcastDao;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

    public void setMetaDataParserFactory(MetaDataParserFactory metaDataParserFactory) {
        this.metaDataParserFactory = metaDataParserFactory;
    }
}
