package ftsdocs.view.controller;

import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.CssParser;
import javafx.css.Declaration;
import javafx.css.Rule;
import javafx.css.Stylesheet;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.controlsfx.control.MasterDetailPane;
import org.fxmisc.richtext.InlineCssTextArea;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ftsdocs.Configuration;
import ftsdocs.DisplayUtils;
import ftsdocs.model.Document;
import ftsdocs.service.FullTextSearchService;

@Slf4j
@RequiredArgsConstructor
@Component
@Lazy
public class MainController implements Initializable {

    private static final Set<String> DOCUMENT_STYLE_CLASSES = Set.of(
            "*.highlight",
            "*.document-content"
    );

    //region Autowired

    private final FullTextSearchService ftsService;

    private final Configuration configuration;

    //endregion

    private static final Pattern HIGHLIGHT_PATTERN = Pattern.compile("(<em>(.*?)</em>)");

    private final ObservableList<Document> documents = FXCollections.observableArrayList();

    private List<String> currentHighlights;

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
    private MasterDetailPane documentPreviewPane;
    @FXML
    private Label matchesCountLabel;
    @FXML
    private Button nextButton;
    @FXML
    private Button previousButton;
    @FXML
    private Button closePreview;
    @FXML
    private InlineCssTextArea documentContentTextArea;

    //endregion

    //region FXML methods

    @FXML
    private void searchButtonClicked(MouseEvent mouseEvent) {
        search();
    }

    @FXML
    private void searchKeyClick(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER) {
            search();
        }
    }

    @FXML
    private void previousHighlightClick(MouseEvent mouseEvent) {
        int caretPosition = documentContentTextArea.getCaretPosition();
        int lastIndex = documentContentTextArea.getLength();
        int i = currentHighlights.size() - 1;
        while (i >= 0) {
            String highlight = currentHighlights.get(i);
            int foundIndex = documentContentTextArea.getText().lastIndexOf(highlight, lastIndex);
            if (foundIndex < caretPosition - highlight.length()) {
                scrollAndSelectContentTo(foundIndex, highlight.length());
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
    private void nextHighlightClick(MouseEvent mouseEvent) {
        int caretPosition = documentContentTextArea.getCaretPosition();
        int lastIndex = 0;
        int i = 0;
        while (i < currentHighlights.size()) {
            String highlight = currentHighlights.get(i);
            int foundIndex = documentContentTextArea.getText().indexOf(highlight, lastIndex);
            if (foundIndex >= caretPosition) {
                scrollAndSelectContentTo(foundIndex, highlight.length());
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

    @FXML
    private void closePreviewClick(MouseEvent mouseEvent) {
        documentPreviewPane.setShowDetailNode(false);
    }

    // endregion

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Color color = configuration.isEnableDarkMode() ? Color.BLACK : Color.WHITE;
        documentContentTextArea.setBorder(
                new Border(new BorderStroke(color, BorderStrokeStyle.SOLID,
                        CornerRadii.EMPTY, new BorderWidths(0.5))));
        documentContentTextArea.setLineHighlighterFill(Color.RED);
        defineDocumentTableColumns();
        this.documentTable.setItems(documents);
        this.documentTable.setEditable(false);
        this.documentTable.setOnMouseClicked(this::handleDocumentClick);
    }

    private void search() {
        String query = this.searchTextField.getText();
        if (query.isBlank()) {
            query = "*";
        }
        Collection<Document> result = this.ftsService.searchDocuments(query);
        this.documents.setAll(result);
        clearContentArea();

        this.documentPreviewPane.setShowDetailNode(false);
        DisplayUtils.showNotification(this.root, "Information",
                "Found " + result.size() + (result.size() == 1 ? " document" : " documents"));
    }

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
            openDocument(Path.of(selectedDocument.getPath()));

        }
    }

    private static List<String> splitContentByHighlights(String content) {
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

    private void openDocument(Path path) {
        final Desktop desktop = Desktop.getDesktop();
        if (desktop.isSupported(Action.OPEN)) {
            try {
                File file = path.toFile();
                desktop.open(file);
            } catch (Exception e) {
                log.error("Failed opening file: {}", path, e);
                DisplayUtils.showNotification(this.root, "Error", e.getMessage());
            }
        }
    }

    private void scrollAndSelectContentTo(int start, int length) {
        documentContentTextArea.moveTo(start);
        documentContentTextArea.selectRange(start, start + length);
        documentContentTextArea.requestFollowCaret();
    }

    private void clearContentArea() {
        currentHighlights = null;
        documentContentTextArea.clear();
        matchesCountLabel.setText("");
    }

    private void fillContentArea(Document selectedDocument) {
        clearContentArea();
        String content = selectedDocument.getHighlight();

        Map<String, Collection<String>> styleStrings = extractDocumentStyles();

        if (content == null) {
            this.previousButton.setDisable(true);
            this.nextButton.setDisable(true);
            this.matchesCountLabel.setText("0 matches");

            Collection<String> finalStyles = new ArrayList<>(
                    styleStrings.get("*.document-content"));
            finalStyles.add(
                    "-fx-font-size: " + this.configuration.getContentFontSize() + "px !important");
            this.documentContentTextArea.append(selectedDocument.getContent(),
                    String.join(";", finalStyles));
        } else {
            List<String> contentParts = splitContentByHighlights(content);
            this.currentHighlights = new ArrayList<>();
            contentParts.forEach(txt -> {
                Collection<String> finalStyles = new ArrayList<>(
                        styleStrings.get("*.document-content"));
                finalStyles.add("-fx-font-size: " + this.configuration.getContentFontSize()
                        + "px !important");
                Matcher matcher = HIGHLIGHT_PATTERN.matcher(txt);
                if (matcher.matches()) {
                    txt = matcher.group(2);
                    finalStyles.add(
                            "-fx-fill: " + colorToCssRgb(this.configuration.getHighlightColor())
                                    + " !important");
                    finalStyles.addAll(styleStrings.get("*.highlight"));
                    this.currentHighlights.add(txt);
                }
                this.documentContentTextArea.append(txt, String.join(";", finalStyles));
            });
            this.matchesCountLabel.setText(
                    currentHighlights.size() + (currentHighlights.size() == 1 ? " match"
                            : " matches"));
            this.previousButton.setDisable(false);
            this.nextButton.setDisable(false);
        }
        this.documentPreviewPane.setShowDetailNode(true);
        scrollAndSelectContentTo(0, 0);
    }

    private Map<String, Collection<String>> extractDocumentStyles() {
        Map<String, Collection<String>> documentStyles = new HashMap<>();

        ObservableList<String> stylesheetUrls = root.getScene().getStylesheets();
        for (int i = stylesheetUrls.size() - 1; i >= 0; i--) {
            try {
                URL url = new URL(stylesheetUrls.get(i));
                CssParser cssParser = new CssParser();
                Stylesheet stylesheet = cssParser.parse(url);
                List<Rule> rules = stylesheet.getRules();
                rules.stream()
                        .filter(rule -> DOCUMENT_STYLE_CLASSES.contains(
                                rule.getSelectors().get(0).toString()))
                        .forEach(rule -> {
                            String styleClass = rule.getSelectors().get(0).toString();
                            Collection<String> styles = documentStyles.computeIfAbsent(styleClass,
                                    k -> new ArrayList<>());
                            styles.add(buildCssDeclaration(rule.getDeclarations().get(0)));
                        });
            } catch (Exception e) {
                log.error("Exception in extractDocumentStyleRules", e);
            }
        }
        return documentStyles;
    }

    private String buildCssDeclaration(Declaration declaration) {
        String property = declaration.getProperty();

        Object value = declaration.getParsedValue().getValue();

        String result;

        if (value instanceof Color color) {
            result = colorToCssRgb(color);
        } else {
            result = value.toString();
        }

        return property + " : " + result;

    }

    private static String colorToCssRgb(Color color) {
        return "rgb(" +
                (int) (color.getRed() * 255) + "," +
                (int) (color.getGreen() * 255) + "," +
                (int) (color.getBlue() * 255) + ") ";
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
                .subtract(20);
    }
}
