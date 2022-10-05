package ftsdocs;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Scanner;

import javafx.application.Application;
import javafx.application.Platform;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrXmlConfig;

public class Main {

    private static EmbeddedSolrServer server;
    private static final Scanner scanner = new Scanner(System.in);
    private static final File resourceSolr;

    static {
        try {
            resourceSolr = new File(Main.class.getResource("/solr").toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws IOException {
        CoreContainer coreContainer = new CoreContainer(
                SolrXmlConfig.fromSolrHome(resourceSolr.toPath(), System.getProperties()));
        coreContainer.load();
        server = new EmbeddedSolrServer(coreContainer, "test");

        boolean run = true;
        while (run) {
            System.out.println(
                    """
                                             
                              1 - Show files
                              2 - Index file
                              3 - Remove file
                              4 - Search
                              5 - Run GUI
                              6 - Quit
                            """);
            int input = scanner.nextInt();
            switch (input) {
                case 1 -> {
                    SolrDocumentList result = search("*");
                    if (result.isEmpty()) {
                        System.out.println("Not found");
                    }
                    else {
                        for (SolrDocument document : result) {
                            System.out.println(document.getFieldValue("id"));
                        }
                    }
                }
                case 2 -> {
                    System.out.println("Path to file: ");
                    String path = scanner.next();
                    indexFile(path);
                }
                case 3 -> {
                    System.out.println("File name: ");
                    String id = scanner.next();
                    removeFile(id);
                }
                case 4 -> {
                    System.out.println("Query: ");
                    String query = scanner.next();
                    SolrDocumentList result = search(query);
                    if (result.isEmpty()) {
                        System.out.println("Not found");
                    }
                    else {
                        for (SolrDocument document : result) {
                            System.out.println(document.getFieldValue("id"));
                        }
                    }
                }
                case 5 -> launchGui();
                case 6 -> {
                    run = false;
                    Platform.exit();
                }
                default -> System.out.println("Invalid option: " + input);
            }
        }
        coreContainer.shutdown();
        server.close();
    }

    public static void launchGui() {
        //Application.launch(FTSDocsApplication.class);
        Thread guiThread = new Thread(() -> Application.launch(FTSDocsApplication.class));
        guiThread.start();
    }


    public static void removeFile(String id) {
        try {
            server.deleteById(id);
            server.commit();
        } catch (Exception e) {
        }
    }

    public static void indexFile(String path) {
        File file = new File(path);
        if (! file.exists() || file.isDirectory()) {
            System.out.println("File does not exist or is a directory: " + path);
            return;
        }
        try {
            SolrInputDocument document = new SolrInputDocument();
            BasicFileAttributes attributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            document.addField("id", file.getName());
            document.addField("creation time", attributes.creationTime());
            int p = file.getName().lastIndexOf(".");
            if (p > 0) {
                String extension = file.getName().substring(p + 1);
                document.addField("extension", extension);
            }
            document.addField("content", Files.readString(file.toPath()));

            server.add(document);
            server.commit();
        } catch (Exception e) {
            System.out.println("Error indexing file: " + path);
            e.printStackTrace();
        }
    }

    public static SolrDocumentList search(String query) {
        SolrQuery solrQuery = new SolrQuery("content:" + query);
        try {
            QueryResponse response = server.query(solrQuery);
            return response.getResults();
        } catch (Exception e) {
            System.out.println("Error in quering files");
        }
        return new SolrDocumentList();
    }
}