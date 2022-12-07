package ftsdocs.controller;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import ftsdocs.DisplayUtils;
import ftsdocs.SolrService;
import ftsdocs.model.Document;
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
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
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
    private Button indexFilesButton;
    @FXML
    private Button indexDirectoriesButton;

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
    private TextFlow textFlow;
    //endregion

    private final ObservableList<Document> documents = FXCollections.observableArrayList();

    private final SolrService solrService;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        documentTable.setItems(documents);
        documentTable.setEditable(false);
        documentTable.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                String content = documentTable.getSelectionModel().getSelectedItem().getContent();
                Text text = new Text(content);
                text.wrappingWidthProperty().bind(textFlow.widthProperty().subtract(10));
                textFlow.getChildren().clear();
                textFlow.getChildren().add(text);
            }
        });
        documentNameColumn.prefWidthProperty().bind(getPathColumnSize());
        documentNameColumn.setCellValueFactory(
                c -> new ReadOnlyStringWrapper(c.getValue().getPath()));
        documentSizeColumn.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        FileUtils.byteCountToDisplaySize(c.getValue().getFileSize())));
        documentCreationTimeColumn.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        DisplayUtils.dateTimeFormatter.format(
                                c.getValue().getCreationTime().toInstant())));
        documentModificationTimeColumn.setCellValueFactory(c ->
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
        String query = searchTextField.getText();
        if (query.isBlank()) {
            query = "*";
        }
        Collection<Document> result = solrService.searchDocuments(query);
        documents.setAll(result);
    }

    @FXML
    private void indexFilesButtonClicked(MouseEvent mouseEvent) {
        FileChooser chooser = new FileChooser();
        List<File> files = chooser
                .showOpenMultipleDialog(root.getScene().getWindow());
        if (files != null) {
            solrService.indexFiles(files);
        }
    }

    @FXML
    private void indexDirectoriesButtonClicked(MouseEvent mouseEvent) {
        DirectoryChooser chooser = new DirectoryChooser();
        File directory = chooser.showDialog(root.getScene().getWindow());
        if (directory != null) {
            solrService.indexFiles(Collections.singletonList(directory));
        }
    }
}
