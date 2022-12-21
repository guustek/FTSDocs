package ftsdocs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javafx.concurrent.Task;

import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.HighlightParams;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Service;

import ftsdocs.model.Document;
import ftsdocs.model.FieldName;

@Slf4j
@Service
@Lazy
public class SolrService {

    private final SolrClient client;
    private final ContentExtractor contentExtractor;
    private final ExecutorService executor;

    public SolrService(ContentExtractor contentExtractor) {
        this.client = SolrServer.getServer().getClient();
        this.contentExtractor = contentExtractor;
        this.executor = Executors.newCachedThreadPool(
                new CustomizableThreadFactory("Indexing Thread-"));
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
                    DisplayUtils.toJson(solrQuery.toString()), e);
        }
        return documents;
    }

    private static SolrQuery prepareQuery(String query) {
        return new SolrQuery()
                .setParam(CommonParams.DF, FieldName.CONTENT)
                //.setParam(CommonParams.FL, "*", "score")
                .setParam(HighlightParams.SCORE_K1, "0")
                .setHighlight(true)
                .setHighlightFragsize(0)
                .setHighlightSnippets(Integer.MAX_VALUE - 1)
                .setHighlightRequireFieldMatch(true)
                .setRows(100)
                .setQuery(query);
    }

    public void indexFilesAsync(List<File> files) {
        Task<Void> indexingTask = new Task<>() {
            @Override
            protected Void call() {
                indexFiles(files);
                return null;
            }
        };
        executor.execute(indexingTask);
    }

    void indexFiles(List<File> files) {
        long start = System.currentTimeMillis();
        List<File> indexedFiles = new LinkedList<>();
        for (File file : files) {
            try {
                if (file.isDirectory()) {
                    try (Stream<Path> pathStream = Files.walk(file.toPath())) {
                        List<Document> documents = pathStream
                                .filter(path -> path.toFile().isFile())
                                .map(path -> contentExtractor.getDocumentFromFile(path.toFile()))
                                .filter(Objects::nonNull)
                                .toList();
                        client.addBeans(documents);
                        client.commit();
                        indexedFiles.addAll(
                                documents.stream().map(doc -> new File(doc.getPath())).toList());
                    }
                } else if (file.isFile()) {
                    Document document = contentExtractor.getDocumentFromFile(file);
                    if (document != null) {
                        client.addBean(document);
                        client.commit();
                        indexedFiles.add(file);
                    }
                }
            } catch (Exception e) {
                log.error("Error while indexing file: {}", file, e);
            }
        }

        long time = System.currentTimeMillis() - start;
        log.info("{} finished indexing {} {} in {} seconds",
                Thread.currentThread().

                        getName(),
                indexedFiles.size() == 1 ? "file" : "files",
                indexedFiles.size(), (double) time / 1000);
    }
}
