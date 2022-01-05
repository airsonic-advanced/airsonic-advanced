package org.airsonic.player.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Service
public class TaskSchedulingService {
    private static final Logger LOG = LoggerFactory.getLogger(TaskSchedulingService.class);

    private TaskScheduler executor;
    private final Map<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();
    private final Map<WatchKey, Map<Kind<? extends Object>, BiConsumer<Path, WatchEvent<Path>>>> watchFunctions = new ConcurrentHashMap<>();
    private final Map<String, WatchKey> watchNames = new ConcurrentHashMap<>();
    // private final ExecutorService watchFunctionExecutor =
    // Executors.newCachedThreadPool(Util.getDaemonThreadfactory("path-watch-functions"));
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
                Optional.ofNullable(fs.get(event.kind()))
                        .ifPresent(f -> executor.schedule(
                                () -> f.accept((Path) key.watchable(), (WatchEvent<Path>) event), Instant.now()));
            }
            // To keep receiving events
            key.reset();
        }
    };

    @Autowired
    public TaskSchedulingService(TaskScheduler taskExecutor) throws IOException {
        this.executor = taskExecutor;
        this.watchService = FileSystems.getDefault().newWatchService();
        this.executor.schedule(watcherTask, Instant.now());
    }

    public void setSchedule(String name, Function<TaskScheduler, ScheduledFuture<?>> scheduledTask) {
        setSchedule(name, scheduledTask, true);
    }

    public void setSchedule(String name, Function<TaskScheduler, ScheduledFuture<?>> scheduledTask, boolean cancelIfExists) {
        tasks.compute(name, (k, v) -> {
            if (cancelIfExists && v != null) {
                v.cancel(true);
            }
            return scheduledTask.apply(executor);
        });
    }

    public ScheduledFuture<?> getScheduledTask(String name) {
        return tasks.get(name);
    }

    public void unscheduleTask(String name, boolean mayInterrupt) {
        ScheduledFuture<?> task = tasks.remove(name);
        if (task != null) {
            task.cancel(mayInterrupt);
        }
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

    public void setWatcher(String id, Path watchable,
            Map<Kind<? extends Object>, BiConsumer<Path, WatchEvent<Path>>> fnMap) throws IOException {
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

}
