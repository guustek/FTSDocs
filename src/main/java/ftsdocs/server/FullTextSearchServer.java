package ftsdocs.server;

import org.apache.solr.client.solrj.SolrClient;

public interface FullTextSearchServer {
    void stop();

    SolrClient getClient();
}
