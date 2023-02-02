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

import ftsdocs.configuration.Configuration;
import ftsdocs.IndexedFilesVisitor;
import ftsdocs.model.IndexLocation;
import ftsdocs.model.IndexStatus;
import ftsdocs.model.WatcherStatus;

@Slf4j
public class DirectoryWatcherManager {

    private final Configuration configuration;
    private final Map<IndexLocation, DirectoryWatcher> watchers;
    private final ExecutorService executor;
    private final FullTextSearchService ftsService;

    public DirectoryWatcherManager(FullTextSearchService ftsService, Configuration configuration) {
        this.ftsService = ftsService;
        this.configuration = configuration;
        this.watchers = new ConcurrentHashMap<>();
        this.executor = Executors.newCachedThreadPool(
                new CustomizableThreadFactory("File watcher thread-"));
    }

    public void updateWatchers(Collection<IndexLocation> indexLocations) {
        Task<Void> updateWatchersTask = new Task<>() {
            @Override
            protected Void call() {
                indexLocations.parallelStream()
                        .filter(loc -> configuration.isFileFormatSupported(loc.getRoot())
                                || loc.getRoot().isDirectory())
                        .forEach(loc -> updateWatcher(loc));
                return null;
            }
        };
        this.executor.execute(updateWatchersTask);
    }

    private void updateWatcher(IndexLocation location) {
        try {
            DirectoryWatcher directoryWatcher = this.watchers.get(location);
            if (directoryWatcher != null) {
                directoryWatcher.close();
            }
        } catch (IOException e) {
            log.error("Failed closing watcher for {}", location.getRoot());
        }

        try {
            location.setWatcherStatus(WatcherStatus.BUILDING_WATCHER);
            DirectoryWatcher watcher = DirectoryWatcher.builder()
                    .path(location.getRoot().toPath().getParent())
                    .listener(event -> handleFileChangeEvent(location.getRoot().toPath(), event))
                    .fileHasher(FileHasher.LAST_MODIFIED_TIME)
                    .fileTreeVisitor(
                            new IndexedFilesVisitor(location.getRoot().toPath(), configuration))
                    .build();
            this.watchers.put(location, watcher);
            watcher.watchAsync(executor);
            location.setWatcherStatus(WatcherStatus.WATCHING);
            log.info("{} started watcher for {} location",
                    Thread.currentThread().getName(), location.getRoot());
        } catch (Exception e) {
            log.error("Building directory watcher failed", e);
        }
    }

    // TODO: 29.01.2023 NEED TO TEST THIS BULLSHIT
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
            case CREATE, MODIFY -> {
                IndexLocation indexLocation = this.configuration.getIndexedLocations()
                        .get(sourcePath.toString());
                if (indexLocation != null) {
                    File file = event.path().toFile();
                    ftsService.updateLocation(indexLocation, file);
                    if (indexLocation.getIndexedFiles().stream()
                            .allMatch(loc -> loc.getIndexStatus() == IndexStatus.FAILED)) {
                        indexLocation.setIndexStatus(IndexStatus.FAILED);
                    } else {
                        indexLocation.setIndexStatus(IndexStatus.INDEXED);
                    }
                }
            }
            case DELETE -> {
                IndexLocation indexLocation = this.configuration.getIndexedLocations()
                        .get(sourcePath.toString());
                if (indexLocation != null) {
                    indexLocation.setIndexStatus(IndexStatus.UPDATING);
                    Path path = event.path();
                    this.configuration.getIndexedLocations().values()
                            .forEach(loc -> loc.getIndexedFiles().removeIf(file -> file.getRoot().equals(path.toFile())));
                    this.ftsService.deleteFromIndex(Collections.singletonList(path));
                    if (indexLocation.getIndexedFiles().stream()
                            .allMatch(loc -> loc.getIndexStatus() == IndexStatus.FAILED)) {
                        indexLocation.setIndexStatus(IndexStatus.FAILED);
                    } else {
                        indexLocation.setIndexStatus(IndexStatus.INDEXED);
                    }
                    this.configuration.writeToFile();
                }

            }
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
