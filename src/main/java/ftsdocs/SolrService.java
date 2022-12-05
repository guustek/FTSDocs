package ftsdocs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import ftsdocs.model.Document;
import ftsdocs.model.Document.FieldName;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
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

    public Collection<Document> searchDocuments(String query)
            throws SolrServerException, IOException {
        SolrQuery solrQuery = new SolrQuery(FieldName.CONTENT + ":" + query);
        QueryResponse response = this.client.query(solrQuery);
        List<Document> results = response.getResults().stream()
                .map(Document::getDocumentFromSolrDocument)
                .toList();
        log.info("Found documents: {}", DisplayUtils.toUnescapedWhiteSpacesJson(results));
        return results;
    }

    public void indexFiles(List<File> files) throws IOException, SolrServerException {
        for (File file : files) {
            if (file.isDirectory()) {
                try (Stream<Path> pathStream = Files.walk(file.toPath())
                        .filter(path -> path.toFile().isFile())) {
                    List<SolrInputDocument> documents = pathStream
                            .map(path -> contentExtractor.getDocumentFromFile(path.toFile()))
                            .filter(Objects::nonNull)
                            .map(Document::getSolrInputDocument).toList();
                    client.add(documents);
                    client.commit();
                    log.info("Successfully indexed documents: {}", DisplayUtils.toJson(documents));
                }
            } else if (file.isFile()) {
                Document document = contentExtractor.getDocumentFromFile(file);
                if (document != null) {
                    SolrInputDocument solrInputDocument = document.getSolrInputDocument();
                    client.add(solrInputDocument);
                    client.commit();
                    log.info("Successfully indexed documents: {}",
                            DisplayUtils.toUnescapedWhiteSpacesJson(document));
                }
            }
        }
    }



}
