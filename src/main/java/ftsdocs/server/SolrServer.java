package ftsdocs.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javafx.application.Platform;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrClient;

import ftsdocs.FTSDocsApplication;

@Slf4j
public abstract class SolrServer implements FullTextSearchServer {

    protected static final String SERVER_DIR = "server";
    protected static final String CORE_NAME = "core";
    protected SolrClient client;

    SolrServer() {
        try {
            File engineFile = new File(FTSDocsApplication.HOME_DIR, SERVER_DIR);
            if (!engineFile.exists()) {
                String solrResource = "/server/solr/";
                File solrDir = new File(engineFile, "solr");
                solrDir.mkdirs();

                copyResourceToFile(solrResource + "solr.xml", new File(solrDir, "solr.xml"));

                File coreDir = new File(solrDir, "core");
                File coreConfDir = new File(coreDir, "conf");
                File coreConfLangDir = new File(coreConfDir, "lang");
                coreConfLangDir.mkdirs();
                File stopwordsDir = new File(coreConfLangDir, "stopwords");
                stopwordsDir.mkdir();
                File synonymsDir = new File(coreConfLangDir, "synonyms");
                synonymsDir.mkdir();

                copyResourceToFile(solrResource + "core/core.properties", new File(coreDir, "core.properties"));

                copyResourceToFile(solrResource + "core/conf/schema.xml", new File(coreConfDir, "schema.xml"));
                copyResourceToFile(solrResource + "core/conf/managed-schema.xml", new File(coreConfDir, "managed-schema.xml"));
                copyResourceToFile(solrResource + "core/conf/solrconfig.xml", new File(coreConfDir, "solrconfig.xml"));
                copyResourceToFile(solrResource + "core/conf/lang/stopwords/stopwords_pl.txt", new File(stopwordsDir, "stopwords_pl.txt"));
                copyResourceToFile(solrResource + "core/conf/lang/synonyms/synonyms_pl.txt", new File(synonymsDir, "synonyms_pl.txt"));
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

    private static void copyResourceToFile(String resource, File targetFile) throws IOException {
        try (final var outputStream = new FileOutputStream(targetFile)) {
            IOUtils.copy(SolrServer.class.getResourceAsStream(resource), outputStream);
        }catch (Exception e){
            log.error("XXD");
        }
    }

}
