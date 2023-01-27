package ftsdocs.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.concurrent.Task;

import io.methvin.watcher.DirectoryChangeEvent;
import io.methvin.watcher.DirectoryChangeEvent.EventType;
import io.methvin.watcher.DirectoryWatcher;
import io.methvin.watcher.hashing.FileHasher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import ftsdocs.Configuration;
import ftsdocs.IndexedFilesVisitor;

@Slf4j
public class DirectoryWatcherManager {

    private final Configuration configuration;
    private final Map<Path, DirectoryWatcher> watchers;
    private final ExecutorService executor;
    private final FullTextSearchService ftsService;

    public DirectoryWatcherManager(FullTextSearchService ftsService, Configuration configuration) {
        this.ftsService = ftsService;
        this.configuration = configuration;
        this.watchers = new ConcurrentHashMap<>();
        this.executor = Executors.newCachedThreadPool(
                new CustomizableThreadFactory("File watcher thread-"));
    }

    public void updateWatchers(Collection<File> indexLocations) {
        Task<Void> updateWatchersTask = new Task<>() {
            @Override
            protected Void call() {
                indexLocations.parallelStream()
                        .filter(file -> configuration.isFileFormatSupported(file) || file.isDirectory())
                        .forEach(file -> updateWatcher(file.toPath()));
                return null;
            }
        };
        this.executor.execute(updateWatchersTask);
    }

    private void updateWatcher(Path sourcePath) {
        try {
            DirectoryWatcher directoryWatcher = this.watchers.get(sourcePath);
            if (directoryWatcher != null) {
                directoryWatcher.close();
            }
        } catch (IOException e) {
            log.error("Failed closing watcher for {}", sourcePath);
        }

        try {
            DirectoryWatcher watcher = DirectoryWatcher.builder()
                    .path(sourcePath.getParent())
                    .listener(event -> handleFileChangeEvent(sourcePath, event))
                    .fileHasher(FileHasher.LAST_MODIFIED_TIME)
                    .fileTreeVisitor(new IndexedFilesVisitor(sourcePath, configuration))
                    .build();
            this.watchers.put(sourcePath, watcher);
            watcher.watchAsync(executor);
            log.info("{} started watcher for {} location",
                    Thread.currentThread().getName(), sourcePath);
        } catch (Exception e) {
            log.error("Building directory watcher failed", e);
        }
    }

    private void handleFileChangeEvent(Path sourcePath, DirectoryChangeEvent event) {
        if (event.eventType() == EventType.OVERFLOW) {
            log.info("FileSystemEvent - Type: {}, source path: {}, root path: {}, path: {}",
                    event.eventType(), sourcePath, event.rootPath(), event.path());
        }
        if (!shouldHandleFileSystemEvent(sourcePath, event)) {
            return;
        }

        log.info("FileSystemEvent - Type: {}, source path: {}, root path: {}, path: {}",
                event.eventType(), sourcePath, event.rootPath(), event.path());

        switch (event.eventType()) {
            case CREATE, MODIFY ->
                    this.ftsService.indexFiles(Collections.singletonList(event.path().toFile()),
                            false, null);
            case DELETE -> this.ftsService.deleteFromIndex(Collections.singletonList(event.path()));
            default -> throw new IllegalStateException("Unexpected value: " + event.eventType());
        }
    }

    private boolean shouldHandleFileSystemEvent(Path sourcePath,
            DirectoryChangeEvent event) {

        if (!this.configuration.isFileFormatSupported(event.path().toFile())) {
            return false;
        }

        //Single file indexed
        if (sourcePath.equals(event.path())) {
            return true;
        }

        //Directory indexed
        return event.path().startsWith(sourcePath);
    }
}
