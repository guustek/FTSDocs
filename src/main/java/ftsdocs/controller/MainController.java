package ftsdocs.controller;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ftsdocs.DisplayUtils;
import ftsdocs.SolrService;
import ftsdocs.model.Document;

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
    private TableView<Document> documentTable;
    @FXML
    private TableColumn<Document, String> documentNameColumn;
    @FXML
    private TableColumn<Document, String> documentSizeColumn;
    @FXML
    private TableColumn<Document, String> documentCreationTimeColumn;
    @FXML
    private TableColumn<Document, String> documentModificationTimeColumn;

    @FXML
    private WebView documentContentArea;
    //endregion

    private final ObservableList<Document> documents = FXCollections.observableArrayList();

    private final SolrService solrService;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.documentTable.setItems(documents);
        this.documentTable.setEditable(false);
        this.documentTable.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                Document selectedDocument = this.documentTable.getSelectionModel()
                        .getSelectedItem();
                if (selectedDocument == null) {
                    return;
                }
                String text = selectedDocument.getHighlight();
                if (text == null) {
                    text = selectedDocument.getContent();
                }
                this.documentContentArea.getEngine().loadContent("<pre>" + text + "</pre>");
            }
        });
        this.documentNameColumn.prefWidthProperty().bind(getPathColumnSize());
        this.documentNameColumn.setCellValueFactory(
                c -> new ReadOnlyStringWrapper(c.getValue().getPath()));
        this.documentSizeColumn.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        FileUtils.byteCountToDisplaySize(c.getValue().getFileSize())));
        this.documentCreationTimeColumn.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        DisplayUtils.dateTimeFormatter.format(
                                c.getValue().getCreationTime().toInstant())));
        this.documentModificationTimeColumn.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        DisplayUtils.dateTimeFormatter.format(
                                c.getValue().getLastModifiedTime().toInstant())));
    }

    private ObservableValue<? extends Number> getPathColumnSize() {
        return documentTable.widthProperty()
                .subtract(documentSizeColumn.widthProperty())
                .subtract(documentCreationTimeColumn.widthProperty())
                .subtract(documentModificationTimeColumn.widthProperty());
    }

    @FXML
    private void searchButtonClicked(MouseEvent mouseEvent) {
        String query = this.searchTextField.getText();
        if (query.isBlank()) {
            query = "*";
        }
        Collection<Document> result = this.solrService.searchDocuments(query);
        this.documents.setAll(result);
        this.documentContentArea.getEngine().loadContent("");
    }

    @FXML
    private void indexFilesButtonClicked(MouseEvent mouseEvent) {
        FileChooser chooser = new FileChooser();
        List<File> files = chooser
                .showOpenMultipleDialog(this.root.getScene().getWindow());
        if (files != null) {
            this.solrService.indexFiles(files);
        }
    }

    @FXML
    private void indexDirectoriesButtonClicked(MouseEvent mouseEvent) {
        DirectoryChooser chooser = new DirectoryChooser();
        File directory = chooser.showDialog(this.root.getScene().getWindow());
        if (directory != null) {
            this.solrService.indexFiles(Collections.singletonList(directory));
        }
    }
}
