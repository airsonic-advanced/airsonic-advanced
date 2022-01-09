package org.airsonic.player.service;

import org.airsonic.player.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

import static java.util.stream.Collectors.toMap;

@Service
public class PathWatcherService {
    private static final Logger LOG = LoggerFactory.getLogger(PathWatcherService.class);

    private final ExecutorService watcherThread = Executors.newSingleThreadExecutor(Util.getDaemonThreadfactory("path-watcher-thread-"));
    private final ExecutorService watcherFunctionThreadPool = Executors.newCachedThreadPool(Util.getDaemonThreadfactory("path-watcher-function-thread-"));

    private final Map<WatchKey, Map<Kind<? extends Object>, BiConsumer<Path, WatchEvent<Path>>>> watchFunctions = new ConcurrentHashMap<>();
    private final Map<String, WatchKey> watchNames = new ConcurrentHashMap<>();
    private WatchService watchService;
    private final Runnable watcherTask = () -> {
        for (;;) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (Exception e) {
                LOG.warn("Watch service unable to poll. Directory monitoring may be off", e);
                continue;
            }
            Map<Kind<? extends Object>, BiConsumer<Path, WatchEvent<Path>>> fs = watchFunctions.get(key);
            if (fs == null) {
                continue;
            }
            for (WatchEvent<?> event : key.pollEvents()) {
                Optional.ofNullable(fs.get(event.kind())).ifPresent(f -> watcherFunctionThreadPool.submit(() -> f.accept((Path) key.watchable(), (WatchEvent<Path>) event)));
            }
            // To keep receiving events
            key.reset();
        }
    };

    public PathWatcherService() throws Exception {
        this.watchService = FileSystems.getDefault().newWatchService();
        watcherThread.submit(watcherTask);
    }

    public void setWatcher(Path watchable, BiConsumer<Path, WatchEvent<Path>> entryCreateConsumer,
            BiConsumer<Path, WatchEvent<Path>> entryDeleteConsumer,
            BiConsumer<Path, WatchEvent<Path>> entryModifyConsumer, BiConsumer<Path, WatchEvent<Path>> overflowConsumer)
            throws IOException {
        setWatcher(UUID.randomUUID().toString(), watchable, entryCreateConsumer, entryDeleteConsumer, entryModifyConsumer, overflowConsumer);
    }

    public void setWatcher(String id, Path watchable, BiConsumer<Path, WatchEvent<Path>> entryCreateConsumer,
            BiConsumer<Path, WatchEvent<Path>> entryDeleteConsumer,
            BiConsumer<Path, WatchEvent<Path>> entryModifyConsumer, BiConsumer<Path, WatchEvent<Path>> overflowConsumer)
            throws IOException {
        Map<Kind<? extends Object>, BiConsumer<Path, WatchEvent<Path>>> fnMap = new HashMap<>();
        fnMap.put(StandardWatchEventKinds.ENTRY_CREATE, entryCreateConsumer);
        fnMap.put(StandardWatchEventKinds.ENTRY_DELETE, entryDeleteConsumer);
        fnMap.put(StandardWatchEventKinds.ENTRY_MODIFY, entryModifyConsumer);
        fnMap.put(StandardWatchEventKinds.OVERFLOW, overflowConsumer);
        setWatcher(id, watchable, fnMap);
    }

    public void setWatcher(String id, Path watchable, Map<Kind<? extends Object>, BiConsumer<Path, WatchEvent<Path>>> fnMap) throws IOException {
        WatchKey key = watchable.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.OVERFLOW);
        watchFunctions.put(key, fnMap);
        watchNames.compute(id, (k, v) -> {
            if (!key.equals(v)) {
                invalidateWatcher(v);
            }
            return key;
        });
    }

    public void invalidateWatcher(String id) {
        WatchKey key = watchNames.remove(id);
        invalidateWatcher(key);
    }

    public void invalidateWatcher(Path watchable) {
        watchNames.entrySet().stream()
            .filter(e -> e.getValue().watchable().equals(watchable))
            .findAny()
            .ifPresent(e -> invalidateWatcher(e.getKey()));
    }

    public void invalidateWatcher(WatchKey key) {
        if (key != null) {
            key.cancel();
            watchFunctions.remove(key);
        }
    }

    @Component
    @Endpoint(id = "pathwatcher")
    public static class PathWatcherEndpoint {
        @Autowired
        private PathWatcherService pathWatcherService;

        @ReadOperation
        public WebEndpointResponse<Map<String, String>> info() {
            return new WebEndpointResponse<>(pathWatcherService.watchNames.entrySet().stream()
                    .collect(toMap(e -> e.getKey(), e -> e.getValue().watchable().toString())));
        }
    }

}
