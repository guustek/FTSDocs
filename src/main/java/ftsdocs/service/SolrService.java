package ftsdocs.service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.CommonParams;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Service;

import ftsdocs.Configuration;
import ftsdocs.FTSDocsApplication;
import ftsdocs.model.Document;
import ftsdocs.model.FieldName;
import ftsdocs.server.FullTextSearchServer;

@Slf4j
@Service
@Lazy
public class SolrService implements FullTextSearchService {

    private final SolrClient client;
    private final ContentExtractor contentExtractor;
    private final ExecutorService executor;
    private final Configuration configuration;
    private final DirectoryWatcherManager watcherManager;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public SolrService(
            ContentExtractor contentExtractor,
            FullTextSearchServer server,
            Configuration configuration) {
        this.client = server.getClient();
        this.contentExtractor = contentExtractor;
        this.configuration = configuration;
        this.executor = Executors.newCachedThreadPool(
                new CustomizableThreadFactory("Indexing thread-"));
        this.watcherManager = new DirectoryWatcherManager(this, this.configuration);

        List<File> indexLocations = this.configuration.getIndexedLocations().stream()
                .map(File::new)
                .toList();

        updateIndices();
        if (this.configuration.isEnableFileWatcher()) {
            this.watcherManager.updateWatchers(indexLocations);
        }

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
            this.client.deleteById(path.toString());
            this.client.commit();
        } catch (Exception e) {
            log.error("Failed deleting {}", path);
        }
    }

    public void deleteFromIndex(Collection<Path> paths) {
        if (paths.isEmpty()) {
            log.info("Nothing to delete");
            return;
        }
        try {
            this.client.deleteById(paths.stream().map(Path::toString).toList());
            this.client.commit();
        } catch (Exception e) {
            log.error("Failed deleting {}", paths);
        }
    }

    public void indexFiles(
            Collection<File> indexLocations,
            boolean updateWatcher,
            EventHandler<WorkerStateEvent> successHandler) {

        Task<Collection<Document>> indexingTask = new Task<>() {
            @Override
            protected Collection<Document> call() {
                log.info("{} started indexing task for locations: {}",
                        Thread.currentThread().getName(),
                        FTSDocsApplication.GSON.toJson(
                                indexLocations.stream().map(File::getAbsolutePath).toList()));

                Collection<File> actualFiles = indexLocations.stream()
                        .flatMap(file -> readFileTree(file).stream())
                        .filter(configuration::isFileFormatSupported)
                        .toList();

                Collection<Document> documents = doIndexing(actualFiles);

                log.info("{} finished indexing task of {} documents",
                        Thread.currentThread().getName(),
                        documents.size());
                return documents;
            }
        };
        indexingTask.setOnSucceeded(successHandler);
        executor.execute(indexingTask);

        if (updateWatcher) {
            log.info("Starting watchers update task for locations: {}",
                    FTSDocsApplication.GSON.toJson(
                            indexLocations.stream().map(File::getAbsolutePath)));
            this.watcherManager.updateWatchers(indexLocations);
        }
    }

    private void updateIndices() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                log.info("{} started updating task", Thread.currentThread().getName());
                Collection<File> actualFiles = configuration.getIndexedLocations().stream()
                        .flatMap(location -> readFileTree(new File(location)).stream())
                        .filter(configuration::isFileFormatSupported)
                        .toList();
                deleteFilesNotInCollection(actualFiles);
                doIndexing(actualFiles);
                return null;
            }
        };
        executor.execute(task);
    }

    private void deleteFilesNotInCollection(Collection<File> actualFiles) {
        // TODO need to show that these dont exist anymore instead of deleting
        this.configuration.getIndexedLocations().removeIf(path -> !new File(path).exists());
        this.configuration.writeToFile();

        List<Path> toBeDeleted = searchDocuments("*").stream()
                .map(doc -> Path.of(doc.getPath()))
                .filter(path -> !actualFiles.contains(path.toFile()))
                .toList();
        deleteFromIndex(toBeDeleted);
    }

    private Collection<File> readFileTree(File file) {
        Collection<File> result = Collections.emptyList();

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        if (!file.isDirectory()) {
            result = Collections.singletonList(file);
        }
        try (Stream<Path> pathStream = Files.walk(file.toPath())) {
            result = pathStream
                    .map(Path::toFile)
                    .filter(File::isFile)
                    .toList();
        } catch (Exception e) {
            log.error("Error while reading directory tree", e);
        }
        stopWatch.stop();
        log.info("{} finished reading file tree of {} in {}, Found {} files",
                Thread.currentThread().getName(),
                file,
                stopWatch,
                result.size());
        return result;
    }

    private Collection<Document> doIndexing(Collection<File> actualFiles) {

        Collection<Document> documents = new ArrayList<>();
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            documents = actualFiles.parallelStream()
                    .map(contentExtractor::getDocumentFromFile)
                    .filter(Objects::nonNull)
                    .toList();

            stopWatch.stop();
            log.info("Finished reading content of {} files in {}, {} files were skipped",
                    documents.size(), stopWatch,
                    actualFiles.size() - documents.size());

            stopWatch = new StopWatch();
            stopWatch.start();

            client.addBeans(documents);
            client.commit();

            stopWatch.stop();
            log.info("Finished indexing {} files in {}",
                    documents.size(), stopWatch);

        } catch (Exception e) {
            log.error("Error while indexing locations: {}",
                    FTSDocsApplication.GSON.toJson(actualFiles), e);
        }
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
}
