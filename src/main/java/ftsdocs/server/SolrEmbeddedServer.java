package ftsdocs.server;

import java.io.File;
import java.io.IOException;

import javafx.application.Platform;

import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ftsdocs.FTSDocsApplication;
import ftsdocs.ServerBeanCondition;

@Slf4j
@Component
@Lazy
@Conditional(ServerBeanCondition.class)
public class SolrEmbeddedServer extends SolrServer {

    private EmbeddedSolrServer client;

    public SolrEmbeddedServer() {
        super();
    }

    @Override
    protected void startServer() {
        try {
            log.info("Starting Solr embedded server");
            log.info("Loading core");
            CoreContainer coreContainer = CoreContainer.createAndLoad(
                    new File(FTSDocsApplication.HOME_DIR, SOLR_DIR + "/server/solr").toPath());
            log.info("Core loaded");
            this.client = new EmbeddedSolrServer(coreContainer, CORE_NAME);
            log.info("Solr embedded server started");
        } catch (Exception e) {
            log.error("Error while starting embedded Solr server. Shutting down", e);
            stop();
            Platform.exit();
        }
    }

    public void stop() {
        if (this.client == null) {
            return;
        }
        try {
            log.info("Shutting down embedded Solr server");
            this.client.close();
            log.info("Solr embedded closed");
        } catch (IOException e) {
            log.error("Encountered an error when stopping embedded Solr server", e);
        } finally {
            log.info("Interrupting " + Thread.currentThread().getName());
            Thread.currentThread().interrupt();
        }
    }

    public SolrClient getClient() {
        return client;
    }
}
