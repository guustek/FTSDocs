package ftsdocs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import ftsdocs.model.Document;
import ftsdocs.model.FieldName;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.SortClause;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.params.CursorMarkParams;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Lazy
public class SolrService {

    private final SolrClient client;
    private final ContentExtractor contentExtractor;

    public SolrService(ContentExtractor contentExtractor) {
        this.client = SolrServer.getServer().getClient();
        this.contentExtractor = contentExtractor;
    }

    public Collection<Document> searchDocuments(String query) {
        List<Document> results = new LinkedList<>();
        SolrQuery solrQuery = new SolrQuery()
                .setQuery(FieldName.CONTENT + ":" + query)
                .setRows(Integer.MAX_VALUE)
                .setSort(SortClause.asc(FieldName.PATH));
        String cursorMark = CursorMarkParams.CURSOR_MARK_START;
        boolean isDone = false;
        try {
            while (!isDone) {
                solrQuery.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
                QueryResponse response = this.client.query(solrQuery);
                String nextCursorMark = response.getNextCursorMark();
                results.addAll(response.getBeans(Document.class));
                if (cursorMark.equals(nextCursorMark)) {
                    isDone = true;
                }
                cursorMark = nextCursorMark;
            }
        } catch (Exception e) {
            log.error("Error while searching with query: {}",
                    DisplayUtils.toJson(solrQuery.toString()));
        }
        log.info("Found {} documents: {}", results.size(),
                DisplayUtils.toUnescapedWhiteSpacesJson(results));
        return results;
    }

    public void indexFiles(List<File> files) {
        for (File file : files) {
            try {
                if (file.isDirectory()) {
                    Supplier<Stream<Path>> pathStreamSupplier = () -> {
                        try {
                            return Files.walk(file.toPath());
                        } catch (IOException e) {
                            log.error("Error while walking directory tree: ", e);
                        }
                        return Stream.of();
                    };
                    List<Document> documents = pathStreamSupplier.get()
                            .filter(path -> path.toFile().isFile())
                            .map(path -> contentExtractor.getDocumentFromFile(path.toFile()))
                            .filter(Objects::nonNull)
                            .toList();
                    UpdateResponse updateResponse = client.addBeans(documents);
                    client.commit();
                    log.info("Update response: {}", DisplayUtils.toJson(updateResponse));
                    log.info("Successfully indexed {} documents: {}", documents.size(),
                            DisplayUtils.toUnescapedWhiteSpacesJson(documents));
                } else if (file.isFile()) {
                    Document document = contentExtractor.getDocumentFromFile(file);
                    if (document != null) {
                        UpdateResponse updateResponse = client.addBean(document);
                        client.commit();
                        log.info("Update response: {}", DisplayUtils.toJson(updateResponse));
                        log.info("Successfully indexed document: {}",
                                DisplayUtils.toUnescapedWhiteSpacesJson(document));
                    }
                }
            } catch (Exception e) {
                log.error("Error while indexing file: {}", file, e);
            }
        }
    }
}
