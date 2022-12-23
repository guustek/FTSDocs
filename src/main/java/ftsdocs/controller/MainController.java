package ftsdocs.controller;

import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
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
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import jfxtras.styles.jmetro.Style;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.controlsfx.control.MasterDetailPane;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.richtext.model.Paragraph;
import org.fxmisc.richtext.model.StyledSegment;
import org.reactfx.collection.LiveList;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ftsdocs.Configuration;
import ftsdocs.DisplayUtils;
import ftsdocs.service.FullTextSearchService;
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
    private Label matchesCountLabel;
    @FXML
    private Button nextButton;
    @FXML
    private Button previousButton;
    @FXML
    private MasterDetailPane documentContentPane;
    @FXML
    private StyleClassedTextArea documentContentTextArea;

    //endregion

    private static final Pattern HIGHLIGHT_PATTERN = Pattern.compile("(<em>(.*?)</em>)");

    private final ObservableList<Document> documents = FXCollections.observableArrayList();

    private final FullTextSearchService ftsService;

    private int currentOccurrenceIndex = 0;
    private List<String> currentHighlights;

    //region FXML methods

    @FXML
    private void searchButtonClicked(MouseEvent mouseEvent) {
        String query = this.searchTextField.getText();
        if (query.isBlank()) {
            query = "*";
        }
        Collection<Document> result = this.ftsService.searchDocuments(query);
        this.documents.setAll(result);
        clearContentArea();
        this.documentContentPane.setShowDetailNode(false);
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

    // endregion

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Color color = Configuration.style == Style.DARK ? Color.WHITE : Color.BLACK;
        documentContentTextArea.setBorder(
                new Border(new BorderStroke(color, BorderStrokeStyle.SOLID,
                        CornerRadii.EMPTY, new BorderWidths(0.5))));
        documentContentTextArea.setLineHighlighterFill(Color.RED);
        defineDocumentTableColumns();
        this.documentTable.setItems(documents);
        this.documentTable.setEditable(false);
        this.documentTable.setOnMouseClicked(this::handleDocumentClick);
    }

    //region Table methods

    private void handleDocumentClick(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }
        Document selectedDocument = this.documentTable.getSelectionModel().getSelectedItem();
        if (selectedDocument == null) {
            return;
        }

        if (event.getClickCount() == 1) {
            fillContentArea(selectedDocument);
        } else {
            try {
                openDocument(Path.of(selectedDocument.getPath()));
            } catch (IOException e) {
                log.error("Failed opening file: {}", selectedDocument.getPath(), e);
            }
        }
    }

    private void fillContentArea(Document selectedDocument) {
        clearContentArea();
        String documentContentStyleClass =
                Configuration.style == Style.DARK ? "document-content-dark"
                        : "document-content-light";
        String content = selectedDocument.getHighlight();
        if (content == null) {
            documentContentTextArea.append(selectedDocument.getContent(),
                    documentContentStyleClass);
            previousButton.setDisable(true);
            nextButton.setDisable(true);
        } else {
            List<String> contentParts = splitDocumentContent(content);
            currentHighlights = new ArrayList<>();
            contentParts.forEach(txt -> {
                Collection<String> styleClasses = new ArrayList<>();
                styleClasses.add(documentContentStyleClass);
                Matcher matcher = HIGHLIGHT_PATTERN.matcher(txt);
                if (matcher.matches()) {
                    styleClasses.add("highlight");
                    txt = matcher.group(2);
                    currentHighlights.add(txt);
                }
                documentContentTextArea.append(txt, styleClasses);
            });
            matchesCountLabel.setText(
                    currentHighlights.size() + (currentHighlights.size() == 1 ? " match"
                            : " matches"));
            previousButton.setDisable(false);
            nextButton.setDisable(false);
        }
        documentContentPane.setShowDetailNode(true);
        scrollAndSelectContentTo(0, 0);
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

    private ObservableValue<? extends Number> getPathColumnSize() {
        return documentTable.widthProperty()
                .subtract(documentSizeColumn.widthProperty())
                .subtract(documentCreationTimeColumn.widthProperty())
                .subtract(documentModificationTimeColumn.widthProperty())
                .subtract(10);
    }

//endregion

    private static List<String> splitDocumentContent(String content) {
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

    private void scrollAndSelectContentTo(int start, int length) {
        documentContentTextArea.moveTo(start);
        documentContentTextArea.selectRange(start, start + length);
        documentContentTextArea.requestFollowCaret();
    }

    private void clearContentArea() {
        currentHighlights = null;
        currentOccurrenceIndex = 0;
        documentContentTextArea.clear();
    }

    @FXML
    private void previousOccurrenceClick(MouseEvent mouseEvent) {
        int caretPosition = documentContentTextArea.getCaretPosition();
        int lastIndex = documentContentTextArea.getLength();
        int i = currentHighlights.size() - 1;
        while (i >= 0) {
            String highlight = currentHighlights.get(i);
            int foundIndex = documentContentTextArea.getText().lastIndexOf(highlight, lastIndex);
            if (foundIndex < caretPosition - highlight.length()) {
                scrollAndSelectContentTo(foundIndex, highlight.length());
                currentOccurrenceIndex = i;
                matchesCountLabel.setText(i + 1 + "/" + currentHighlights.size());
                break;
            }
            lastIndex = foundIndex - 1;
            if (i == 0) {
                caretPosition = documentContentTextArea.getLength() - 1;
                lastIndex = documentContentTextArea.getLength() - 1;
                i = currentHighlights.size();
            }
            i--;
        }
    }

    @FXML
    private void nextOccurrenceClick(MouseEvent mouseEvent) {
        int caretPosition = documentContentTextArea.getCaretPosition();
        int lastIndex = 0;
        int i = 0;
        while (i < currentHighlights.size()) {
            String highlight = currentHighlights.get(i);
            int foundIndex = documentContentTextArea.getText().indexOf(highlight, lastIndex);
            if (foundIndex > caretPosition) {
                scrollAndSelectContentTo(foundIndex, highlight.length());
                currentOccurrenceIndex = i;
                matchesCountLabel.setText(i + 1 + "/" + currentHighlights.size());
                break;
            }
            lastIndex = foundIndex + 1;
            if (i == currentHighlights.size() - 1) {
                caretPosition = 0;
                lastIndex = 0;
                i = -1;
            }
            i++;
        }
    }
}
