package ftsdocs.solr;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

import io.methvin.watcher.DirectoryChangeEvent;
import io.methvin.watcher.DirectoryChangeEvent.EventType;
import io.methvin.watcher.DirectoryWatcher;
import io.methvin.watcher.hashing.FileHasher;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.CommonParams;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Service;

import ftsdocs.Configuration;
import ftsdocs.FTSDocsApplication;
import ftsdocs.IndexedFilesVisitor;
import ftsdocs.model.Document;
import ftsdocs.model.FieldName;
import ftsdocs.server.FullTextSearchServer;
import ftsdocs.service.ContentExtractor;
import ftsdocs.service.FullTextSearchService;

@Slf4j
@Service
@Lazy
public class SolrService implements FullTextSearchService {

    private final SolrClient client;
    private final ContentExtractor contentExtractor;
    private final ExecutorService executor;
    private final Configuration configuration;
    private final Map<Path, DirectoryWatcher> watchers;

    public SolrService(ContentExtractor contentExtractor, FullTextSearchServer server,
            Configuration configuration) {
        this.client = server.getClient();
        this.contentExtractor = contentExtractor;
        this.configuration = configuration;
        this.executor = Executors.newCachedThreadPool(
                new CustomizableThreadFactory("Index monitor thread-"));
        this.watchers = new ConcurrentHashMap<>();

        deleteNonExistingFilesFromIndex();
        List<File> indexedFiles = this.configuration.getIndexedLocations().stream()
                .map(File::new)
                .toList();
        indexFiles(indexedFiles, null, true);

    }

    public Collection<Document> searchDocuments(String query) {
        Collection<Document> documents = Collections.emptyList();
        SolrQuery solrQuery = prepareQuery(query);
        try {
            QueryResponse response = this.client.query(solrQuery);
            documents = response.getBeans(Document.class);
            Map<String, Map<String, List<String>>> highlighting = response.getHighlighting();
            for (Document document : documents) {
                Map<String, List<String>> highlightsForPath = highlighting.get(document.getPath());
                List<String> highlightsForField = highlightsForPath.getOrDefault(FieldName.CONTENT,
                        Collections.singletonList(null));
                document.setHighlight(highlightsForField.get(0));
            }
            return documents;
        } catch (Exception e) {
            log.error("Error while searching with query: {}",
                    FTSDocsApplication.GSON.toJson(solrQuery.toString()), e);
        }
        return documents;
    }

    public void deleteFromIndex(Path path) {
        try {
            this.client.deleteById(String.valueOf(path));
            this.client.commit();
        } catch (Exception e) {
            log.error("Failed deleting {}", path);
        }
    }

    public void indexFiles(Collection<File> files, EventHandler<WorkerStateEvent> successHandler,
            boolean updateWatcher) {
        if (updateWatcher) {
            files.parallelStream().forEach(file -> {
                updateWatcher(file.toPath());
                this.configuration.getIndexedLocations().add(file.getPath());
                this.configuration.writeToFile();
            });
        }

        Collection<File> actualFiles = files.stream()
                .flatMap(file -> readFileTree(file).stream())
                .toList();

        Task<Collection<Document>> indexingTask = new Task<>() {
            @Override
            protected Collection<Document> call() {
                return doIndexing(actualFiles);
            }
        };
        indexingTask.setOnSucceeded(successHandler);
        executor.execute(indexingTask);
    }

    private void deleteNonExistingFilesFromIndex() {
        this.configuration.getIndexedLocations().removeIf(path -> !new File(path).exists());
        this.configuration.writeToFile();

        Collection<File> actualFiles = this.configuration.getIndexedLocations().stream()
                .flatMap(file -> readFileTree(new File(file)).stream())
                .toList();

        searchDocuments("*").stream()
                .map(doc -> Path.of(doc.getPath()))
                .filter(path -> !actualFiles.contains(path.toFile()))
                .forEach(this::deleteFromIndex);
    }

    private List<File> readFileTree(File file) {
        if (!file.isDirectory()) {
            return Collections.singletonList(file);
        }
        try (Stream<Path> pathStream = Files.walk(file.toPath())) {
            return pathStream
                    .map(Path::toFile)
                    .filter(File::isFile)
                    .toList();
        } catch (Exception e) {
            log.error("Error while reading directory tree", e);
        }
        return Collections.emptyList();
    }

    private Collection<Document> doIndexing(Collection<File> files) {
        long start = System.currentTimeMillis();
        List<Document> documents = files.parallelStream()
                .map(contentExtractor::getDocumentFromFile)
                .filter(Objects::nonNull)
                .toList();
        try {
            client.addBeans(documents);
            client.commit();
        } catch (Exception e) {
            log.error("Error while indexing files: {}",
                    FTSDocsApplication.GSON.toJson(documents.stream().map(Document::getPath).toList()), e);
        }
        long time = System.currentTimeMillis() - start;
        log.info("{} finished indexing {} {} in {} seconds",
                Thread.currentThread().
                        getName(),
                documents.size() == 1 ? "file" : "files",
                documents.size(), (double) time / 1000);
        return documents;
    }

    private SolrQuery prepareQuery(String query) {
        return new SolrQuery()
                .setParam(CommonParams.DF, FieldName.CONTENT)
                //.setParam(CommonParams.FL, "*", "score")
                //.setParam(HighlightParams.SCORE_K1, "0")
                .setHighlight(true)
                .setHighlightFragsize(0)
                .setHighlightSnippets(this.configuration.getMaxPhraseHighlights())
                .setHighlightRequireFieldMatch(true)
                .setRows(this.configuration.getMaxSearchResults())
                .setQuery(query);
    }

    private void handleFileChangeEvent(Path sourcePath, DirectoryChangeEvent event) {
        if(event.eventType() == EventType.OVERFLOW){
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
                    indexFiles(Collections.singletonList(event.path().toFile()), null, false);
            case DELETE -> deleteFromIndex(event.path());
            default -> throw new IllegalStateException("Unexpected value: " + event.eventType());
        }
    }

    private static boolean shouldHandleFileSystemEvent(Path sourcePath,
            DirectoryChangeEvent event) {

        //Single file indexed
        if(sourcePath.equals(event.path()))
            return true;

        //Directory indexed
        if(event.path().startsWith(sourcePath))
            return true;
        return false;
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
                    .logger(log)
                    .path(sourcePath.getParent())
                    .listener(event -> handleFileChangeEvent(sourcePath, event))
                    .fileHasher(FileHasher.LAST_MODIFIED_TIME)
                    .fileTreeVisitor(new IndexedFilesVisitor(sourcePath))
                    .build();
            this.watchers.put(sourcePath, watcher);
            watcher.watchAsync(executor);
        } catch (Exception e) {
            log.error("Building directory watcher failed", e);
        }
    }
}
