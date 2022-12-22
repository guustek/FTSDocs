package ftsdocs.controller;

import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ftsdocs.DisplayUtils;
import ftsdocs.FullTextSearchService;
import ftsdocs.model.Document;

@Slf4j
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
    private TableColumn<Document, String> documentPathColumn;
    @FXML
    private TableColumn<Document, String> documentSizeColumn;
    @FXML
    private TableColumn<Document, String> documentCreationTimeColumn;
    @FXML
    private TableColumn<Document, String> documentModificationTimeColumn;
    @FXML
    private TextFlow documentContentArea;

    @FXML
    private void searchButtonClicked(MouseEvent mouseEvent) {
        String query = this.searchTextField.getText();
        if (query.isBlank()) {
            query = "*";
        }
        Collection<Document> result = this.ftsService.searchDocuments(query);
        this.documents.setAll(result);
        documentContentArea.getChildren().clear();
    }

    @FXML
    private void indexFilesButtonClicked(MouseEvent mouseEvent) {
        FileChooser chooser = new FileChooser();
        List<File> files = chooser
                .showOpenMultipleDialog(this.root.getScene().getWindow());
        if (files != null) {
            this.ftsService.indexFiles(files);
        }
    }

    @FXML
    private void indexDirectoriesButtonClicked(MouseEvent mouseEvent) {
        DirectoryChooser chooser = new DirectoryChooser();
        File directory = chooser.showDialog(this.root.getScene().getWindow());
        if (directory != null) {
            this.ftsService.indexFiles(Collections.singletonList(directory));
        }
    }

    //endregion

    private static final Pattern HIGHLIGHT_PATTERN = Pattern.compile("(<em>(.*?)</em>)");

    private final ObservableList<Document> documents = FXCollections.observableArrayList();

    private final FullTextSearchService ftsService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        defineDocumentTableColumns();
        this.documentTable.setItems(documents);
        this.documentTable.setEditable(false);
        this.documentTable.setOnMouseClicked(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            Document selectedDocument = this.documentTable.getSelectionModel().getSelectedItem();
            if (selectedDocument == null) {
                return;
            }
            handleClick(event, selectedDocument);
        });
    }

    private void handleClick(MouseEvent event, Document selectedDocument) {
        if (event.getClickCount() == 1) {
            documentContentArea.getChildren().clear();
            String content = selectedDocument.getHighlight();
            if (content == null) {
                Text text = prepareTextNode(selectedDocument);
                documentContentArea.getChildren().add(text);
                return;
            }

            List<String> extracted = extractHighlights(content);
            Text[] textNodes = extracted.stream().map(txt -> {
                final Text text = new Text(txt);
                text.getStyleClass().add("document-content");
                text.wrappingWidthProperty()
                        .bind(documentContentArea.widthProperty().subtract(10));
                Matcher matcher = HIGHLIGHT_PATTERN.matcher(txt);
                if (matcher.matches()) {
                    text.setText(matcher.group(2));
                    //text.setFill() does not work for some reason
                    text.getStyleClass().add("highlight");
                }
                return text;
            }).toArray(Text[]::new);
            documentContentArea.getChildren().addAll(textNodes);


        } else {
            try {
                openDocument(Path.of(selectedDocument.getPath()));
            } catch (IOException e) {
                log.error("Failed opening file: {}", selectedDocument.getPath(), e);
            }
        }
    }

    private void defineDocumentTableColumns() {
        this.documentPathColumn.prefWidthProperty().bind(getPathColumnSize());
        this.documentPathColumn.setCellValueFactory(
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

    private Text prepareTextNode(Document selectedDocument) {
        Text text = new Text(selectedDocument.getContent());
        text.getStyleClass().add("document-content");
        text.wrappingWidthProperty()
                .bind(documentContentArea.widthProperty().subtract(10));
        return text;
    }

    private static List<String> extractHighlights(String content) {
        Matcher matcher = HIGHLIGHT_PATTERN.matcher(content);
        List<String> split = new LinkedList<>();
        int lastEnd = 0;
        while (matcher.find()) {
            split.add(content.substring(lastEnd, matcher.start()));
            split.add(matcher.group(1));
            lastEnd = matcher.end();
        }
        split.add(content.substring(lastEnd));
        return split;
    }

    private void openDocument(Path path) throws IOException {
        final Desktop desktop = Desktop.getDesktop();
        if (desktop.isSupported(Action.OPEN)) {
            File file = path.toFile();
            desktop.open(file);
        }
    }

    private ObservableValue<? extends Number> getPathColumnSize() {
        return documentTable.widthProperty()
                .subtract(documentSizeColumn.widthProperty())
                .subtract(documentCreationTimeColumn.widthProperty())
                .subtract(documentModificationTimeColumn.widthProperty())
                .subtract(10);
    }
}
