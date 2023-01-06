package ftsdocs.solr;

import java.io.File;

import javafx.application.Platform;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import ftsdocs.Configuration;
import ftsdocs.server.FullTextSearchServer;

@Slf4j
public abstract class SolrServer implements FullTextSearchServer {

    protected static final String SOLR_DIR = "engine";
    protected static final String CORE_NAME = "core";

    SolrServer() {
        try {
            File engineFile = new File(SOLR_DIR);
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

    protected abstract void startServer();

}
