package ftsdocs.server;

import ftsdocs.FTSDocsApplication;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrClient;

import java.io.File;

@Slf4j
public abstract class SolrServer implements FullTextSearchServer {

    protected static final String SOLR_DIR = "engine";
    protected static final String CORE_NAME = "core";
    protected SolrClient client;

    SolrServer() {
        try {
            File engineFile = new File(FTSDocsApplication.HOME_DIR, SOLR_DIR);
            if (!engineFile.exists()) {
                File solrResourceDir = new File(this.getClass().getResource("/solr").toURI());
                FileUtils.copyDirectory(solrResourceDir, engineFile);
            }
            startServer();
        } catch (Exception e) {
            log.error("Could not copy engine files", e);
            Platform.exit();
        }
    }

    @Override
    public SolrClient getClient() {
        return this.client;
    }

    protected abstract void startServer();

}
