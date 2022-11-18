package ftsdocs.controller;

import java.io.File;

import ftsdocs.SolrServer;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

public class MainController {

    public TableView<SolrDocument> fileTable;
    public Button indexButton;
    public Button searchButton;

    public void indexButton_MouseClicked(MouseEvent event) {
        var server = SolrServer.getInstance();
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(indexButton.getScene().getWindow());
        if(file != null && file.exists()){
            server.indexFile(file.getPath());
        }
    }

    public void searchButton_MouseClicked(MouseEvent event) {
        var server = SolrServer.getInstance();
        SolrDocumentList documents = server.search("*");
        fileTable.setItems(FXCollections.observableList(documents));
    }
}
