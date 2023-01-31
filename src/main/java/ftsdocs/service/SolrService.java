package ftsdocs.service;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

import ftsdocs.FTSDocsApplication;
import ftsdocs.FileSystemUtils;
import ftsdocs.configuration.Configuration;
import ftsdocs.model.Document;
import ftsdocs.model.FieldName;
import ftsdocs.model.IndexLocation;
import ftsdocs.model.IndexStatus;
import ftsdocs.model.WatcherStatus;
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

        updateIndices();
        if (this.configuration.isEnableFileWatcher()) {
            this.watcherManager.updateWatchers(this.configuration.getIndexedLocations().values());
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

    public void indexLocations(
            Collection<IndexLocation> locations,
            boolean updateWatcher,
            EventHandler<WorkerStateEvent> successHandler) {

        Task<Collection<Document>> indexingTask = new Task<>() {
            @Override
            protected Collection<Document> call() {
                log.info("{} started indexing task for locations: {}",
                        Thread.currentThread().getName(),
                        FTSDocsApplication.GSON.toJson(
                                locations.stream()
                                        .map(loc -> loc.getRoot().getAbsolutePath())
                                        .toList()));

                List<IndexLocation> actualFiles = locations.parallelStream()
                        .flatMap(loc -> {
                            loc.setIndexStatus(IndexStatus.READING_FILE_TREE);
                            List<IndexLocation> files = FileSystemUtils.readFileTree(loc.getRoot())
                                    .stream()
                                    .filter(configuration::isFileFormatSupported)
                                    .map(file -> new IndexLocation(
                                            file,
                                            false,
                                            IndexStatus.EXTRACTING_CONTENT,
                                            WatcherStatus.UNKNOWN))
                                    .toList();
                            loc.setIndexedFiles(new ArrayList<>(files));
                            return files.stream();
                        })
                        .toList();

                Collection<Document> documents = doIndexing(locations, actualFiles);

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
                            locations.stream()
                                    .map(loc -> loc.getRoot().getAbsolutePath())
                                    .toList()));
            this.watcherManager.updateWatchers(locations);
        }
    }

    public void updateLocation(IndexLocation indexLocation, File file) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                try {
                    indexLocation.setIndexStatus(IndexStatus.UPDATING);
                    IndexLocation location = new IndexLocation(file, false);
                    indexLocation.getIndexedFiles().add(location);
                    Document document = contentExtractor.getDocumentFromFile(file);
                    if (document != null) {
                        location.setIndexStatus(IndexStatus.INDEXED);
                        client.addBean(document);
                        client.commit();
                    } else {
                        location.setIndexStatus(IndexStatus.FAILED);
                    }
                } catch (Exception e) {
                    log.error("Error when updating {}", file, e);
                }
                return null;
            }
        };
        executor.execute(task);
    }

    private void updateIndices() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                log.info("{} started updating task", Thread.currentThread().getName());
                Collection<IndexLocation> actualFiles = configuration.getIndexedLocations().values()
                        .parallelStream()
                        .flatMap(location -> {
                            location.setIndexStatus(IndexStatus.UPDATING);
                            List<IndexLocation> files = FileSystemUtils.readFileTree(
                                            location.getRoot()).stream()
                                    .filter(configuration::isFileFormatSupported)
                                    .map(file -> new IndexLocation(
                                            file,
                                            false,
                                            IndexStatus.EXTRACTING_CONTENT,
                                            WatcherStatus.UNKNOWN))
                                    .toList();
                            location.setIndexedFiles(new ArrayList<>(files));
                            return files.stream();
                        })
                        .toList();
                deleteFilesNotInCollection(actualFiles);
                doIndexing(configuration.getIndexedLocations().values(), actualFiles);
                return null;
            }
        };
        executor.execute(task);
    }

    private void deleteFilesNotInCollection(Collection<IndexLocation> actualFiles) {
        List<Path> toBeDeleted = searchDocuments("*").stream()
                .map(doc -> new IndexLocation(new File(doc.getPath()), false))
                .filter(path -> !actualFiles.contains(path))
                .map(loc -> loc.getRoot().toPath())
                .toList();
        deleteFromIndex(toBeDeleted);
    }

    private Collection<Document> doIndexing(
            Collection<IndexLocation> indexLocations,
            Collection<IndexLocation> actualFiles) {

        Collection<Document> documents = new ArrayList<>();
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            indexLocations.forEach(loc -> {
                loc.setIndexStatus(IndexStatus.EXTRACTING_CONTENT);
                loc.getIndexedFiles()
                        .forEach(file -> file.setIndexStatus(IndexStatus.EXTRACTING_CONTENT));
            });
            documents = actualFiles.parallelStream()
                    .map(loc -> {
                        Document documentFromFile = contentExtractor.getDocumentFromFile(
                                loc.getRoot());
                        if (documentFromFile == null) {
                            loc.setIndexStatus(IndexStatus.FAILED);
                        } else {
                            loc.setIndexStatus(IndexStatus.INDEXING);
                        }
                        return documentFromFile;
                    })
                    .filter(Objects::nonNull)
                    .toList();

            stopWatch.stop();
            log.info("Finished reading content of {} files in {}, {} files were skipped",
                    documents.size(), stopWatch,
                    actualFiles.size() - documents.size());

            stopWatch = new StopWatch();
            stopWatch.start();

            indexLocations.forEach(loc -> loc.setIndexStatus(IndexStatus.INDEXING));

            client.addBeans(documents);
            client.commit();

            indexLocations.forEach(loc -> {
                if (loc.getIndexedFiles().stream()
                        .allMatch(file -> file.getIndexStatus() == IndexStatus.FAILED)) {
                    loc.setIndexStatus(IndexStatus.FAILED);
                } else {
                    loc.setIndexStatus(IndexStatus.INDEXED);
                }

                loc.getIndexedFiles().forEach(file -> {
                    if (file.getIndexStatus() != IndexStatus.FAILED) {
                        file.setIndexStatus(IndexStatus.INDEXED);
                    }

                });
            });
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
