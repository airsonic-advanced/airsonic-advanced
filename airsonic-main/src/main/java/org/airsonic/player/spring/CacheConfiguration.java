package org.airsonic.player.spring;

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.Playlist;
import org.airsonic.player.domain.User;
import org.airsonic.player.domain.UserSettings;
import org.airsonic.player.service.SettingsService;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ConfigurationBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.event.CacheEvent;
import org.ehcache.event.CacheEventListener;
import org.ehcache.event.EventType;
import org.ehcache.impl.config.event.DefaultCacheEventListenerConfiguration;
import org.ehcache.impl.config.persistence.DefaultPersistenceConfiguration;
import org.ehcache.jsr107.EhcacheCachingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.cache.Caching;
import javax.cache.spi.CachingProvider;

import java.time.Duration;
import java.util.EnumSet;

@Configuration
@EnableCaching(proxyTargetClass = true, mode = AdviceMode.ASPECTJ) // AspectJ used so classes calling methods on self can benefit from the cache
public class CacheConfiguration {
    @Bean
    public javax.cache.CacheManager jCacheCacheManager() {
        CachingProvider provider = Caching.getCachingProvider("org.ehcache.jsr107.EhcacheCachingProvider");
        EhcacheCachingProvider ehcacheCachingProvider = (EhcacheCachingProvider) provider;
        return ehcacheCachingProvider.getCacheManager(
                ehcacheCachingProvider.getDefaultURI(),
                createConfig(provider.getDefaultClassLoader()));
    }

    private org.ehcache.config.Configuration createConfig(final ClassLoader cl) {
        ResourcePoolsBuilder pools = ResourcePoolsBuilder.newResourcePoolsBuilder()
                .heap(1000L, EntryUnit.ENTRIES);
        // If needed, but will need to register serializers for the objects being stored
        // .offheap(10L, MemoryUnit.MB)
        // .disk(20, MemoryUnit.MB, false);

        DefaultCacheEventListenerConfiguration cacheLogging = new DefaultCacheEventListenerConfiguration(EnumSet.allOf(EventType.class), CacheLogger.class);

        return ConfigurationBuilder.newConfigurationBuilder()
                .withService(new DefaultPersistenceConfiguration(SettingsService.getAirsonicHome().resolve("cache").toFile()))
                .withCache("userCache",
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, User.class, pools)
                                .withClassLoader(cl)
                                .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofDays(2)))
                                .withService(cacheLogging))
                .withCache("userSettingsCache",
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, UserSettings.class, pools)
                                .withClassLoader(cl)
                                .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofDays(2)))
                                .withService(cacheLogging))
                .withCache("mediaFilePathCache",
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, MediaFile.class, pools)
                                .withClassLoader(cl)
                                .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofHours(2)))
                                .withService(cacheLogging))
                .withCache("mediaFileIdCache",
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(Integer.class, MediaFile.class, pools)
                                .withClassLoader(cl)
                                .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofHours(2)))
                                .withService(cacheLogging))
                .withCache("playlistCache",
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(Integer.class, Playlist.class, pools)
                                .withClassLoader(cl)
                                .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofDays(10)))
                                .withService(cacheLogging))
                .withCache("playlistUsersCache",
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(Integer.class, Object.class, pools)
                                .withClassLoader(cl)
                                .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofDays(10)))
                                .withService(cacheLogging))
                .build();
    }

    public static class CacheLogger implements CacheEventListener<Object, Object> {
        private static final Logger LOG = LoggerFactory.getLogger(CacheLogger.class);

        @Override
        public void onEvent(CacheEvent<?, ?> cacheEvent) {
            LOG.info("Cache Key: {} | EventType: {} | Old value: {} | New value: {}", cacheEvent.getKey(),
                    cacheEvent.getType(), cacheEvent.getOldValue(), cacheEvent.getNewValue());
        }
    }

}
