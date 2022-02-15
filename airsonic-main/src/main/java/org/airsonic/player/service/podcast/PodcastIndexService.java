package org.airsonic.player.service.podcast;

import org.airsonic.player.domain.UserCredential;
import org.airsonic.player.domain.UserCredential.App;
import org.airsonic.player.domain.UserSettings;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.VersionService;
import org.airsonic.player.util.Util;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class PodcastIndexService {
    private static final Logger LOG = LoggerFactory.getLogger(PodcastIndexService.class);

    @Autowired
    private SettingsService settingsService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private VersionService versionService;

    private static final String DEFAULT_URL = "https://api.podcastindex.org/api/1.0/search/byterm";

    public List<PodcastIndexResponse.PodcastIndexResult> search(String username, String search) throws Exception {
        UserSettings userSettings = settingsService.getUserSettings(username);

        if (!userSettings.getPodcastIndexEnabled() || StringUtils.isBlank(search)) {
            return Collections.emptyList();
        }

        Map<App, UserCredential> creds = securityService.getDecodableCredsForApps(username, App.PODCASTINDEX);

        UserCredential cred = creds.get(App.PODCASTINDEX);
        if (cred != null) {
            String decoded = SecurityService.decodeCredentials(cred);
            if (decoded != null) {
                HttpUriRequest req = createRequest(
                        UriComponentsBuilder
                                .fromHttpUrl(StringUtils.isBlank(userSettings.getPodcastIndexUrl()) ? DEFAULT_URL
                                        : userSettings.getPodcastIndexUrl())
                                .queryParam("q", search).toUriString(),
                        cred.getAppUsername(), decoded);
                PodcastIndexResponse resp = executeRequest(req);
                if (req != null) {
                    return resp.getFeeds();
                }
            }
        }

        return Collections.emptyList();
    }

    private HttpUriRequest createRequest(String url, String apiKey, String apiSecret) {
        HttpPost request = new HttpPost(url);
        request.setHeader("User-Agent", "Airsonic/" + versionService.getLocalVersion());
        request.setHeader("X-Auth-Key", apiKey);
        String now = String.valueOf(Instant.now().getEpochSecond());
        request.setHeader("X-Auth-Date", now);
        request.setHeader("Authorization", DigestUtils.sha1Hex(apiKey + apiSecret + now));
        request.setHeader("Content-Type", "application/json; charset=utf-8");

        return request;
    }

    private PodcastIndexResponse executeRequest(HttpUriRequest request) throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
                CloseableHttpResponse resp = client.execute(request);) {
            boolean ok = resp.getStatusLine().getStatusCode() == 200;
            if (!ok) {
                LOG.warn("Failed to execute PodcastIndex request: {}", resp.getEntity().toString());
                return null;
            } else {
                return Util.getObjectMapper().readValue(resp.getEntity().getContent(), PodcastIndexResponse.class);
            }
        }
    }

    // from https://podcastindex-org.github.io/docs-api/#get-/search/byterm
    public static class PodcastIndexResponse {
        private String status;
        private Integer count;
        private List<PodcastIndexResult> feeds;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Integer getCount() {
            return count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }

        public List<PodcastIndexResult> getFeeds() {
            return feeds;
        }

        public void setFeeds(List<PodcastIndexResult> feeds) {
            this.feeds = feeds;
        }

        public static class PodcastIndexResult {
            private Integer id;
            private String title;
            private String url;
            private String link;
            private String description;
            private String author;
            private String artwork;
            private Instant lastUpdateTime;
            private Integer type;
            private Integer dead;
            private Integer locked;
            private String language;
            private Map<Integer, String> categories;
            private String imageUrlHash;

            public Integer getId() {
                return id;
            }

            public void setId(Integer id) {
                this.id = id;
            }

            public String getTitle() {
                return title;
            }

            public void setTitle(String title) {
                this.title = title;
            }

            public String getUrl() {
                return url;
            }

            public void setUrl(String url) {
                this.url = url;
            }

            public String getLink() {
                return link;
            }

            public void setLink(String link) {
                this.link = link;
            }

            public String getDescription() {
                return description;
            }

            public void setDescription(String description) {
                this.description = description;
            }

            public String getAuthor() {
                return author;
            }

            public void setAuthor(String author) {
                this.author = author;
            }

            public String getArtwork() {
                return artwork;
            }

            public void setArtwork(String artwork) {
                this.artwork = artwork;
            }

            public Instant getLastUpdateTime() {
                return lastUpdateTime;
            }

            public void setLastUpdateTime(Instant lastUpdateTime) {
                this.lastUpdateTime = lastUpdateTime;
            }

            public Integer getType() {
                return type;
            }

            public void setType(Integer type) {
                this.type = type;
            }

            public Integer getDead() {
                return dead;
            }

            public void setDead(Integer dead) {
                this.dead = dead;
            }

            public Integer getLocked() {
                return locked;
            }

            public void setLocked(Integer locked) {
                this.locked = locked;
            }

            public String getLanguage() {
                return language;
            }

            public void setLanguage(String language) {
                this.language = language;
            }

            public Map<Integer, String> getCategories() {
                return categories;
            }

            public void setCategories(Map<Integer, String> categories) {
                this.categories = categories;
            }

            public String getImageUrlHash() {
                return imageUrlHash;
            }

            public void setImageUrlHash(String imageUrlHash) {
                this.imageUrlHash = imageUrlHash;
            }

        }
    }
}
