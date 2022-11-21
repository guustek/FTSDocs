package ftsdocs.controller;

import java.io.File;
import java.util.List;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;

import org.apache.commons.io.FileUtils;

import ftsdocs.Document;
import ftsdocs.SolrServer;

public class MainController {

    //region FXML fields
    @FXML
    private Button indexButton;
    @FXML
    private Button searchButton;
    @FXML
    public Button deleteAllButton;

    @FXML
    private TableView<Document> documentTable;
    @FXML
    private TableColumn<Document, String> pathColumn;
    @FXML
    private TableColumn<Document, String> fileSizeColumn;
//endregion

    private ObservableList<Document> documents;

    @FXML
    public void initialize() {
        documents = FXCollections.observableArrayList();
        documentTable.setItems(documents);
        documentTable.setEditable(false);
        documentTable.getSelectionModel().setCellSelectionEnabled(true);
        documentTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        pathColumn.setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().path()));
        fileSizeColumn.setCellValueFactory(p -> new ReadOnlyStringWrapper(
                FileUtils.byteCountToDisplaySize(p.getValue().fileSize())));
    }

    public void indexButtonClick() {
        var server = SolrServer.getInstance();
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(indexButton.getScene().getWindow());
        if (file != null && file.exists()) {
            server.indexFile(file.getPath());
        }
    }

    public void searchButtonClick() {
        var server = SolrServer.getInstance();
        List<Document> result = server.search("*");
        documents.addAll(result);
    }

    public void deleteAllButtonClick() {
        SolrServer.getInstance().removeAllFiles();
        documents.clear();
    }
}
