package ftsdocs.server;

import java.io.IOException;

import javafx.application.Platform;

import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.common.params.CoreAdminParams.CoreAdminAction;
import org.apache.solr.common.util.NamedList;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ftsdocs.ServerBeanCondition;

@Slf4j
@Component
@Lazy
@Conditional(ServerBeanCondition.class)
public class SolrWebServer extends SolrServer {

    private static final String BASE_URL = "http://localhost:8983/solr";
    private static final String SCRIPT_PATH = SERVER_DIR + "/bin/solr.cmd";

    public SolrWebServer() {
        super();
    }

    @Override
    protected void startServer() {
        try {
            log.info("Starting Solr web server");
            ProcessBuilder processBuilder = new ProcessBuilder()
                    .command(SCRIPT_PATH, "start")
                    .inheritIO();
            Process process = processBuilder.start();
            process.waitFor();
            log.info("Loading core");
            waitForCoreToLoad();
            log.info("Core loaded");
            this.client = new Http2SolrClient.Builder(BASE_URL + "/" + CORE_NAME).build();
            log.info("Solr web server started");
        } catch (Exception e) {
            log.error("Error while starting Solr web server. Shutting down", e);
            stop();
            Platform.exit();
        }
    }

    private void waitForCoreToLoad() throws SolrServerException, IOException {
        Http2SolrClient baseClient = new Http2SolrClient.Builder(BASE_URL).build();
        NamedList<Object> coreStatus = null;
        while (coreStatus == null) {
            CoreAdminRequest request = new CoreAdminRequest();
            request.setAction(CoreAdminAction.STATUS);
            CoreAdminResponse response = request.process(baseClient);
            coreStatus = response.getCoreStatus(CORE_NAME);
        }
    }

    @Override
    public void stop() {
        try {
            log.info("Shutting Solr web server");
            ProcessBuilder processBuilder = new ProcessBuilder()
                    .command(SCRIPT_PATH, "stop", "-all")
                    .inheritIO();
            Process process = processBuilder
                    .start();
            process.waitFor();
            log.info("Solr web server stopped");
        } catch (Exception e) {
            log.error("Encountered an error when stopping embedded Solr server");
        } finally {
            log.info("Interrupting " + Thread.currentThread().getName());
            Thread.currentThread().interrupt();
        }
    }
}
