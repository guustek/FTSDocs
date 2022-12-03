package ftsdocs.controller;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;

import ftsdocs.SolrService;
import ftsdocs.model.Document;
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
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Lazy
public class MainController implements Initializable {

    //region FXML fields

    @FXML
    private BorderPane root;
    @FXML
    private TextField searchTextField;
    @FXML
    private Button searchButton;
    @FXML
    private Button indexButton;

    @FXML
    private TableView<Document> documentTable;
    @FXML
    private TableColumn<Document, String> documentNameColumn;
    @FXML
    private TableColumn<Document, String> documentSizeColumn;
    @FXML
    private TableColumn<Document, String> documentLastModificationTime;

    //endregion

    private final ObservableList<Document> documents = FXCollections.observableArrayList();

    private final SolrService solrService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        documentTable.setItems(documents);
        documentTable.setEditable(false);
        documentNameColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().path()));
        documentSizeColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                FileUtils.byteCountToDisplaySize(c.getValue().fileSize())));
        documentLastModificationTime.setCellValueFactory(
                c -> new ReadOnlyStringWrapper(c.getValue().lastModifiedTime()));
    }

    @FXML
    private void searchButtonClicked(MouseEvent mouseEvent)
            throws SolrServerException, IOException {
        String query = searchTextField.getText();
        if (query.isBlank()) {
            query = "*";
        }
        Collection<Document> result = solrService.searchDocuments(query);
        documents.setAll(result);
    }

    @FXML
    private void indexButtonClicked(MouseEvent mouseEvent) throws IOException, SolrServerException {
        FileChooser fileChooser = new FileChooser();
        List<File> files = fileChooser
                .showOpenMultipleDialog(root.getScene().getWindow());
        if (files != null) {
            solrService.indexFiles(files);
        }

    }
}
