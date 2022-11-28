package ftsdocs.controller;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;

import org.apache.commons.io.FileUtils;

import ftsdocs.SolrServer;
import ftsdocs.model.Document;

public class MainController implements Initializable {

    //region FXML fields

    @FXML
    private TextField searchTextField;
    @FXML
    private Button searchButton;
    @FXML
    private Button indexButton;

    @FXML
    private TableView<Document> documentTable;
    @FXML
    private TableColumn<Document,String> documentNameColumn;
    @FXML
    private TableColumn<Document,String> documentSizeColumn;
    @FXML
    private TableColumn<Document,String> documentLastModificationTime;

    //endregion

    private final ObservableList<Document> documents = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        documentTable.setItems(documents);
        documentTable.setEditable(false);
        documentNameColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().path()));
        documentSizeColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(FileUtils.byteCountToDisplaySize(c.getValue().fileSize())));
        documentLastModificationTime.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().modificationTime()));
    }

    @FXML
    private void searchButtonClicked(MouseEvent mouseEvent) {
        String query = searchTextField.getText();
        if (query == null || query.isEmpty()) {
            query = "*";
        }
        List<Document> result = SolrServer.getInstance().search(query);
        documents.setAll(result);
    }

    @FXML
    private void indexButtonClicked(MouseEvent mouseEvent) {
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(indexButton.getScene().getWindow());
        if (file != null && file.exists()) {
            SolrServer.getInstance().indexFile(file.getPath());
        }
    }
}
