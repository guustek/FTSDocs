package ftsdocs;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrXmlConfig;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SolrServer {

    private static final String CORE_NAME = "core";

    private static final SolrServer INSTANCE = new SolrServer();
    private final EmbeddedSolrServer server;
    private final CoreContainer coreContainer;

    private SolrServer() {
        try {
            log.info("Initializing Solr server");
            File resourceSolr = new File(getClass().getResource("/solr").toURI());
            this.coreContainer = new CoreContainer(
                    SolrXmlConfig.fromSolrHome(resourceSolr.toPath(), System.getProperties()));
            this.coreContainer.load();
            this.server = new EmbeddedSolrServer(this.coreContainer, CORE_NAME);
            log.info("Solr server initialized");
        } catch (URISyntaxException e) {
            log.error("Error while initializing Solr server", e);
            throw new RuntimeException(e);
        }
    }

    public static SolrServer getInstance() {
        return INSTANCE;
    }

    public void removeAllFiles() {
        try {
            this.server.deleteByQuery("*:*");
            this.server.commit();
        } catch (SolrServerException | IOException e) {
            log.error("Error while removing all files", e);
        }
    }

    public void removeFile(String id) {
        try {
            this.server.deleteById(id);
            this.server.commit();
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void indexFile(String path) {
        File file = new File(path);
        if (!file.exists() || file.isDirectory()) {
            log.info("File does not exist or is a directory: {}", path);
            return;
        }
        try {
            SolrInputDocument document = new SolrInputDocument();
            BasicFileAttributes attributes = Files.readAttributes(file.toPath(),
                    BasicFileAttributes.class);
            document.addField("path", file.getAbsolutePath());
            document.addField("creationTime", attributes.creationTime().toString());
            document.addField("fileSize", attributes.size());
            int p = file.getName().lastIndexOf(".");
            if (p > 0) {
                String extension = file.getName().substring(p + 1);
                document.addField("extension", extension);
            }
            document.addField("content", Files.readString(file.toPath()));
            log.info("Indexing document: \n {}", GsonUtils.toUnescapedWhiteSpacesJson(document));
            this.server.add(document);
            this.server.commit();
        } catch (Exception e) {
            log.error("Error while indexing file {}: ", file.getAbsolutePath(), e);
        }
    }

    public List<Document> search(String query) {
        SolrQuery solrQuery = new SolrQuery("content:" + query);
        try {
            log.info("Searching for documents, query: {}", solrQuery);
            QueryResponse response = this.server.query(solrQuery);
            log.info("Found documents: \n {}",
                    GsonUtils.toUnescapedWhiteSpacesJson(response.getResults()));
            return response.getResults().stream().map(Document::fromSolrDocument).toList();
        } catch (Exception e) {
            log.error("Error while searching for documents, query {}", solrQuery, e);
        }
        return Collections.emptyList();
    }

    public void stop() throws IOException {
        log.info("Shutting down Solr");
        this.coreContainer.shutdown();
        this.server.close();
        log.info("Core container and server closed");
    }
}
