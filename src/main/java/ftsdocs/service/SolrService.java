package ftsdocs.service;

import ftsdocs.FTSDocsApplication;
import ftsdocs.FileSystemUtils;
import ftsdocs.model.*;
import ftsdocs.model.configuration.Configuration;
import ftsdocs.server.FullTextSearchServer;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SuggesterResponse;
import org.apache.solr.client.solrj.response.Suggestion;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.CursorMarkParams;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@Lazy
public class SolrService implements FullTextSearchService {

    private final SolrClient client;
    private final ContentExtractor contentExtractor;
    private final ExecutorService executor;
    private final Configuration configuration;
    private DirectoryWatcherManager watcherManager;

    private static final String HIGHLIGHT_PREFIX = "<b>";
    private static final String HIGHLIGHT_POSTFIX = "</b>";
    private static final Pattern SUGGESTION_HIGHLIGHT_PATTERN = Pattern.compile(
            HIGHLIGHT_PREFIX + "(.*?)" + HIGHLIGHT_POSTFIX);

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

        updateIndices();
        if (this.configuration.isEnableFileWatcher()) {
            this.watcherManager = new DirectoryWatcherManager(this, this.configuration);
            this.watcherManager.updateWatchers(this.configuration.getIndexedLocations().values());
        } else {
            this.configuration.getIndexedLocations().values()
                    .forEach(loc -> loc.setWatcherStatus(WatcherStatus.DISABLED));
            this.configuration.writeToFile();
        }

    }

    public Collection<Document> searchDocuments(String query) {
        Collection<Document> documents = Collections.emptyList();
        SolrQuery solrQuery = prepareSearchQuery(query);
        try {
            QueryResponse response = this.client.query(solrQuery);
            documents = response.getBeans(Document.class);

            Map<String, Map<String, List<String>>> highlighting = response.getHighlighting();
            for (Document document : documents) {
                Map<String, List<String>> highlightsForPath = highlighting.get(document.getPath());
                List<String> highlightsForField = highlightsForPath.getOrDefault(
                        configuration.isEnableSynonymSearch() ? FieldName.CONTENT_SYNONYMS : FieldName.CONTENT,
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

    @Override
    public Collection<String> getSuggestions(String searchPhrase) {
        //String suggesterName = configuration.isEnableSynonymSearch() ? "suggester_synonyms" : "suggester";
        String suggesterName = "suggester";
        SolrQuery query = new SolrQuery()
                .setRequestHandler("/suggest")
                .setParam("suggest", "true")
                .setParam("suggest.q", searchPhrase)
                .setParam("suggest.dictionary", suggesterName);
        Set<String> result = new HashSet<>();
        try {
            QueryResponse response = this.client.query(query);
            SuggesterResponse suggesterResponse = response.getSuggesterResponse();
            List<Suggestion> suggestions = suggesterResponse.getSuggestions().getOrDefault(suggesterName, List.of());
            suggestions.forEach(
                    suggestion -> result.addAll(extractTermsFromHighlights(suggestion.getTerm())));
        } catch (SolrServerException | IOException e) {
            log.error("Error while fetching suggestions for phrase: {}", searchPhrase, e);
        }
        return result;
    }

    private List<String> extractTermsFromHighlights(String term) {
        List<String> highlightedWords = new ArrayList<>();
        Matcher matcher = SUGGESTION_HIGHLIGHT_PATTERN.matcher(term);

        while (matcher.find()) {
            String highlighted = matcher.group(1);
            int start = matcher.end();
            Set<Character> wordEndCharacters = Set.of(
                    ' ',
                    '\n',
                    '\r',
                    '.',
                    ','
            );
            int end = start;
            for (; end < term.length(); end++) {
                if (wordEndCharacters.contains(term.charAt(end))) {
                    break;
                }
            }
            String substring = term.substring(start, end);
            String word = highlighted + substring;
            word = word.replace(HIGHLIGHT_PREFIX, "");
            word = word.replace(HIGHLIGHT_POSTFIX, "");
            highlightedWords.add(word.toLowerCase());
        }
        return highlightedWords;
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
                            List<IndexLocation> files = extractChildLocations(loc);
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

        if (configuration.isEnableFileWatcher()) {
            log.info("Starting watchers update task for locations: {}",
                    FTSDocsApplication.GSON.toJson(
                            locations.stream()
                                    .map(loc -> loc.getRoot().getAbsolutePath())
                                    .toList()));
            this.watcherManager.updateWatchers(locations);
        } else {
            configuration.getIndexedLocations().values().forEach(loc -> loc.setWatcherStatus(WatcherStatus.DISABLED));
        }
    }

    @Override
    public void updateFileWatcher() {
        if (this.configuration.isEnableFileWatcher()) {
            if (watcherManager == null) {
                this.watcherManager = new DirectoryWatcherManager(this, this.configuration);
            }
            this.watcherManager.updateWatchers(this.configuration.getIndexedLocations().values());
        } else {
            this.watcherManager = null;
            this.configuration.getIndexedLocations().values().forEach(loc -> loc.setWatcherStatus(WatcherStatus.DISABLED));
        }
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
                            Collection<IndexLocation> files = extractChildLocations(location);
                            location.setIndexedFiles(new ArrayList<>(files));
                            return files.stream();
                        })
                        .toList();
                deleteFilesNotInCollection(actualFiles);
                doIndexing(configuration.getIndexedLocations().values(), actualFiles);
                configuration.writeToFile();
                return null;
            }
        };
        executor.execute(task);
    }

    private List<IndexLocation> extractChildLocations(IndexLocation location) {
        return FileSystemUtils.readFileTree(
                        location.getRoot()).stream()
                .filter(configuration::isFileFormatSupported)
                .map(file -> new IndexLocation(
                        file,
                        false,
                        IndexStatus.EXTRACTING_CONTENT,
                        WatcherStatus.UNKNOWN))
                .toList();
    }

    private void deleteFilesNotInCollection(Collection<IndexLocation> actualFiles) {
        Collection<Document> allIndexedDocs = new LinkedList<>();
        try {
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.setRows(100);
            solrQuery.setQuery("*:*");
            solrQuery.addSort(FieldName.PATH, ORDER.asc);
            String cursorMark = CursorMarkParams.CURSOR_MARK_START;
            boolean done = false;
            while (!done) {
                solrQuery.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
                QueryResponse rsp = client.query(solrQuery);
                String nextCursorMark = rsp.getNextCursorMark();
                allIndexedDocs.addAll(rsp.getBeans(Document.class));
                if (cursorMark.equals(nextCursorMark)) {
                    done = true;
                }
                cursorMark = nextCursorMark;
            }
        } catch (Exception ignored) {
        }

        List<Path> toBeDeleted = allIndexedDocs.stream()
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

    private SolrQuery prepareSearchQuery(String query) {
        return new SolrQuery()
                .setParam(CommonParams.DF, configuration.isEnableSynonymSearch()
                        ? FieldName.CONTENT_SYNONYMS
                        : FieldName.CONTENT)
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
