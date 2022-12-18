package ftsdocs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import ftsdocs.model.Document;
import ftsdocs.model.FieldName;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.params.CommonParams;
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
        Collection<Document> documents = Collections.emptyList();
        SolrQuery solrQuery = new SolrQuery()
                .setParam(CommonParams.DF, FieldName.CONTENT)
                .setHighlight(true)
                .setHighlightFragsize(100)
                .setHighlightSnippets(Integer.MAX_VALUE - 1)
                .setHighlightSimplePre("<b><FONT COLOR=\"RED\">")
                .setHighlightSimplePost("</FONT></b>")
                .setRows(100)
                .setQuery(query);
        try {
            QueryResponse response = this.client.query(solrQuery);

            documents = response.getBeans(Document.class);
            Map<String, Map<String, List<String>>> highlighting = response.getHighlighting();

            log.info("Found {} documents: {}", documents.size(),
                    DisplayUtils.toJson(documents.stream().map(Document::getPath).toList()));
            log.info("Found highlight: {}", DisplayUtils.toUnescapedWhiteSpacesJson(highlighting));

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

    public void indexFiles(List<File> files) {
        for (File file : files) {
            try {
                if (file.isDirectory()) {
                    Supplier<Stream<Path>> pathStreamSupplier = () -> {
                        try {
                            return Files.walk(file.toPath());
                        } catch (IOException e) {
                            log.error("Error while walking directory tree of {}: ", file, e);
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
