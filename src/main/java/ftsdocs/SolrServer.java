package ftsdocs;

import java.io.File;
import java.io.IOException;

import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrXmlConfig;

@Slf4j
public class SolrServer {

    private static SolrServer instance;
    private static final String CORE_NAME = "core";
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
        } catch (Exception e) {
            log.error("Error while initializing Solr server", e);
            throw new RuntimeException(e);
        }
    }
    public static SolrServer getServer(){
        if(instance == null){
            instance = new SolrServer();
        }
        return instance;
    }

    public void stop() throws IOException {
        log.info("Shutting down Solr");
        this.coreContainer.shutdown();
        this.server.close();
        log.info("Core container and server closed");
    }

    public SolrClient getClient() {
        return server;
    }
}
