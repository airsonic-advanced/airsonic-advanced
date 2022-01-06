package org.airsonic.player.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.config.FixedDelayTask;
import org.springframework.scheduling.config.FixedRateTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.config.TriggerTask;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Service
public class TaskSchedulingService {
    private static final Logger LOG = LoggerFactory.getLogger(TaskSchedulingService.class);
    @Lazy
    @Autowired
    private ScheduledTaskRegistrar registrar;
    private final Map<String, ScheduledTask> tasks = new ConcurrentHashMap<>();
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
                Optional.ofNullable(fs.get(event.kind()))
                        .ifPresent(f -> scheduleOnce("path-watcher-function-execution",
                                () -> f.accept((Path) key.watchable(), (WatchEvent<Path>) event),
                                Instant.now(),
                                false));
            }
            // To keep receiving events
            key.reset();
        }
    };

    // @Autowired
    public TaskSchedulingService() throws IOException {
        // this.registrar = new ScheduledTaskRegistrar();
        this.watchService = FileSystems.getDefault().newWatchService();
    }

    @PostConstruct
    public void init() {
        this.registrar.scheduleTriggerTask(new TriggerTask(watcherTask, new RunOnceTrigger(0L)));
    }


    public void scheduleTask(String name, Function<ScheduledTaskRegistrar, ScheduledTask> scheduledTask) {
        scheduleTask(name, scheduledTask, true);
    }

    public void scheduleTask(String name, Function<ScheduledTaskRegistrar, ScheduledTask> scheduledTask, boolean cancelIfExists) {
        tasks.compute(name, (k, v) -> {
            if (cancelIfExists && v != null) {
                v.cancel();
            }
            return scheduledTask.apply(registrar);
        });
    }

    public void scheduleFixedDelayTask(String name, Runnable task, Instant firstTime, Duration period, boolean cancelIfExists) {
        scheduleTask(name,
                r -> r.scheduleFixedDelayTask(new FixedDelayTask(task, period.toMillis(), ChronoUnit.MILLIS.between(Instant.now(), firstTime))),
                cancelIfExists);
    }

    public void scheduleAtFixedRate(String name, Runnable task, Instant firstTime, Duration period, boolean cancelIfExists) {
        scheduleTask(name,
                r -> r.scheduleFixedRateTask(new FixedRateTask(task, period.toMillis(), ChronoUnit.MILLIS.between(Instant.now(), firstTime))),
                cancelIfExists);
    }

    public void scheduleOnce(String name, Runnable task, Instant firstTime, boolean cancelIfExists) {
        scheduleTask(name,
                r -> r.scheduleTriggerTask(new TriggerTask(task, new RunOnceTrigger(ChronoUnit.MILLIS.between(Instant.now(), firstTime)))),
                cancelIfExists);
    }

    public ScheduledTask getScheduledTask(String name) {
        return tasks.get(name);
    }

    public void unscheduleTask(String name) {
        ScheduledTask task = tasks.remove(name);
        if (task != null) {
            task.cancel();
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

    public static class RunOnceTrigger extends PeriodicTrigger {
        public RunOnceTrigger(long initialDelay) {
            super(0);
            setInitialDelay(initialDelay);
        }

        @Override
        public Date nextExecutionTime(TriggerContext triggerContext) {
            if (triggerContext.lastCompletionTime() == null) { // hasn't executed yet
                return super.nextExecutionTime(triggerContext);
            }
            return null;
        }
    }

}
