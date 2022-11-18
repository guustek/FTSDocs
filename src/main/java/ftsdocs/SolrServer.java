package ftsdocs;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrXmlConfig;

public class SolrServer {

    private static final SolrServer INSTANCE = new SolrServer();
    private final EmbeddedSolrServer server;
    private final CoreContainer coreContainer;

    private SolrServer() {
        try {
            File resourceSolr = new File(getClass().getResource("/solr").toURI());
            this.coreContainer = new CoreContainer(
                    SolrXmlConfig.fromSolrHome(resourceSolr.toPath(), System.getProperties()));
            this.coreContainer.load();
            this.server = new EmbeddedSolrServer(this.coreContainer, "test");
        } catch (URISyntaxException e) {
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
            throw new RuntimeException(e);
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
        if (! file.exists() || file.isDirectory()) {
            System.out.println("File does not exist or is a directory: " + path);
            return;
        }
        try {
            SolrInputDocument document = new SolrInputDocument();
            BasicFileAttributes attributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            document.addField("id", file.getAbsolutePath());
            document.addField("creation time", attributes.creationTime());
            int p = file.getName().lastIndexOf(".");
            if (p > 0) {
                String extension = file.getName().substring(p + 1);
                document.addField("extension", extension);
            }
            document.addField("content", Files.readString(file.toPath()));

            this.server.add(document);
            this.server.commit();
        } catch (Exception e) {
            System.out.println("Error indexing file: " + path);
            e.printStackTrace();
        }
    }

    public SolrDocumentList search(String query) {
        SolrQuery solrQuery = new SolrQuery("content:" + query);
        try {
            QueryResponse response = this.server.query(solrQuery);
            return response.getResults();
        } catch (Exception e) {
            System.out.println("Error in quering files");
        }
        return new SolrDocumentList();
    }

    public void stop() throws IOException {
        this.coreContainer.shutdown();
        this.server.close();
    }
}
