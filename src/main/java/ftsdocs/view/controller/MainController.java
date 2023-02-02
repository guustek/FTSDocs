package ftsdocs.view.controller;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.beans.binding.Bindings;
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
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
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

import impl.org.controlsfx.skin.AutoCompletePopup;
import impl.org.controlsfx.skin.AutoCompletePopupSkin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.controlsfx.control.MasterDetailPane;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;
import org.fxmisc.richtext.InlineCssTextArea;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ftsdocs.FTSDocsApplication;
import ftsdocs.FileSystemUtils;
import ftsdocs.configuration.Configuration;
import ftsdocs.controls.FileContextMenu;
import ftsdocs.model.Document;
import ftsdocs.model.HighlightSnippet;
import ftsdocs.model.NotificationTitle;
import ftsdocs.service.FullTextSearchService;
import ftsdocs.view.ViewManager;

@Slf4j
@RequiredArgsConstructor
@Component
@Lazy
public class MainController implements Initializable {

    private static final String HIGHLIGHT_STYLE_CLASS = "*.highlight";
    private static final String DOCUMENT_CONTENT_STYLE_CLASS = "*.document-content";

    private static final Set<String> DOCUMENT_STYLE_CLASSES = Set.of(
            HIGHLIGHT_STYLE_CLASS,
            DOCUMENT_CONTENT_STYLE_CLASS
    );

    //region Autowired

    private final FullTextSearchService ftsService;

    private final Configuration configuration;

    private final ViewManager viewManager;

    //endregion

    private static final Pattern HIGHLIGHT_PATTERN = Pattern.compile("(<em>(.*?)</em>)");

    private final ObservableList<Document> documents = FXCollections.observableArrayList();

    private List<HighlightSnippet> currentHighlights;

    private int currentHighlightIndex;

    //region FXML fields

    @FXML
    private BorderPane root;
    @FXML
    private TextField searchTextField;
    @FXML
    private TableView<Document> documentTable;
    @FXML
    private TableColumn<Document, Integer> indexColumn;
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
    private InlineCssTextArea documentContentTextArea;

    //endregion

    //region FXML methods

    @FXML
    private void searchButtonClicked() {
        search();
    }

    @FXML
    private void searchKeyClick(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER) {
            search();
        }
    }

    @FXML
    private void previousHighlightClick() {
        this.currentHighlightIndex--;
        if (this.currentHighlightIndex < 0) {
            this.currentHighlightIndex = this.currentHighlights.size() - 1;
        }
        HighlightSnippet highlight = this.currentHighlights.get(this.currentHighlightIndex);
        scrollToAndSelect(highlight);
        this.matchesCountLabel.setText(
                this.currentHighlightIndex + 1 + "/" + this.currentHighlights.size());
    }

    @FXML
    private void nextHighlightClick() {
        this.currentHighlightIndex++;
        if (this.currentHighlightIndex > this.currentHighlights.size() - 1) {
            this.currentHighlightIndex = 0;
        }
        HighlightSnippet highlight = this.currentHighlights.get(this.currentHighlightIndex);
        scrollToAndSelect(highlight);
        this.matchesCountLabel.setText(
                this.currentHighlightIndex + 1 + "/" + this.currentHighlights.size());
    }

    @FXML
    private void closePreviewClick() {
        documentPreviewPane.setShowDetailNode(false);
    }

    // endregion

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Color color = this.configuration.isEnableDarkMode()
                ? Color.rgb(23, 23, 23)
                : Color.rgb(233, 233, 233);
        double border = 1.5;
        this.documentContentTextArea.setBorder(
                new Border(new BorderStroke(color, BorderStrokeStyle.SOLID,
                        CornerRadii.EMPTY, new BorderWidths(-1, border, border, border))));
        defineDocumentTable();

        if (this.configuration.isEnableSuggestions()) {
            bindAutocompletion();
        }
    }

    private void search() {
        String query = this.searchTextField.getText();
        if (query.isBlank()) {
            query = "*";
        }
        Collection<Document> result = this.ftsService.searchDocuments(query);
        this.documents.setAll(result);
        this.documentTable.refresh();
        clearContentArea();

        this.documentPreviewPane.setShowDetailNode(false);
        this.viewManager.showNotification(NotificationTitle.INFORMATION,
                "Found " + result.size() + (result.size() == 1 ? " document" : " documents"), null);
    }

    private void handleDocumentClick(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }
        @SuppressWarnings("unchecked")
        Document selectedDocument = ((TableRow<Document>) event.getSource()).getItem();
        if (selectedDocument == null) {
            return;
        }

        if (event.getClickCount() == 1) {
            fillContentArea(selectedDocument);
        } else {
            try {
                FileSystemUtils.openDocument(Path.of(selectedDocument.getPath()));
            } catch (IOException e) {
                log.error("Failed opening: {}", selectedDocument.getPath(), e);
                this.viewManager.showNotification(NotificationTitle.ERROR, e.getMessage(), null);
            }

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

    private void scrollToAndSelect(int start, int length) {
        documentContentTextArea.moveTo(start);
        documentContentTextArea.selectRange(start, start + length);
        documentContentTextArea.requestFollowCaret();
    }

    private void scrollToAndSelect(HighlightSnippet highlightSnippet) {
        scrollToAndSelect(highlightSnippet.index(), highlightSnippet.text().length());
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
            this.matchesCountLabel.setText("");

            Collection<String> finalStyles = new ArrayList<>(
                    styleStrings.get(DOCUMENT_CONTENT_STYLE_CLASS));
            finalStyles.add(
                    "-fx-font-size: " + this.configuration.getContentFontSize() + "px !important");
            this.documentContentTextArea.append(selectedDocument.getContent(),
                    String.join(";", finalStyles));
        } else {
            List<String> contentParts = splitContentByHighlights(content);
            this.currentHighlights = new ArrayList<>();
            this.currentHighlightIndex = -1;
            contentParts.forEach(txt -> {
                Collection<String> finalStyles = new ArrayList<>(
                        styleStrings.get(DOCUMENT_CONTENT_STYLE_CLASS));
                finalStyles.add("-fx-font-size: " + this.configuration.getContentFontSize()
                        + "px !important");
                Matcher matcher = HIGHLIGHT_PATTERN.matcher(txt);
                if (matcher.matches()) {
                    txt = matcher.group(2);
                    finalStyles.add(
                            "-fx-fill: " + colorToCssRgb(this.configuration.getHighlightColor())
                                    + " !important");
                    finalStyles.addAll(styleStrings.get(HIGHLIGHT_STYLE_CLASS));
                    this.currentHighlights.add(
                            new HighlightSnippet(txt,
                                    documentContentTextArea.getContent().getLength())
                    );
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
        scrollToAndSelect(0, 0);
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

    @SuppressWarnings("java:S110")
    private void defineDocumentTable() {
        this.documentTable.setItems(documents);
        this.documentTable.setEditable(false);
        this.documentTable.setRowFactory(param -> {
            TableRow<Document> row = new TableRow<>() {
                @Override
                protected void updateItem(Document item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setContextMenu(null);
                    } else {
                        setContextMenu(new FileContextMenu(new File(item.getPath())));
                    }
                }
            };
            row.setOnMouseClicked(this::handleDocumentClick);
            return row;
        });

        this.indexColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty) {
                    setText(String.valueOf(getIndex() + 1));
                }

            }
        });
        this.documentPathColumn.prefWidthProperty().bind(getFileNameColumnSize());
        this.documentPathColumn.setCellValueFactory(
                c -> new ReadOnlyStringWrapper(c.getValue().getPath()));
        this.documentSizeColumn.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        FileUtils.byteCountToDisplaySize(c.getValue().getFileSize())));
        this.documentCreationTimeColumn.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        FTSDocsApplication.DATE_TIME_FORMATTER.format(
                                c.getValue().getCreationTime().toInstant())));
        this.documentModificationTimeColumn.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        FTSDocsApplication.DATE_TIME_FORMATTER.format(
                                c.getValue().getLastModifiedTime().toInstant())));
    }

    @SuppressWarnings({"unchecked", "java:S3011"})
    private void bindAutocompletion() {
        AutoCompletionBinding<String> completionBinding = TextFields.bindAutoCompletion(
                this.searchTextField, param -> {
                    String userText = param.getUserText();
                    if (!userText.isBlank()) {
                        return ftsService.getSuggestions(userText);
                    }
                    return Collections.emptyList();
                });

        AutoCompletePopup<String> autoCompletionPopup = completionBinding.getAutoCompletionPopup();
        AutoCompletePopupSkin<String> skin = new AutoCompletePopupSkin<>(autoCompletionPopup);
        autoCompletionPopup.prefWidthProperty().bind(this.searchTextField.widthProperty());

        try {
            Field suggestionListField = skin.getClass().getDeclaredField("suggestionList");
            suggestionListField.setAccessible(true);
            ListView<String> suggestionList = (ListView<String>) suggestionListField.get(skin);
            suggestionList.prefHeightProperty().bind(
                    Bindings.min(autoCompletionPopup.visibleRowCountProperty(),
                                    Bindings.size(suggestionList.getItems()))
                            .multiply(24));
            suggestionListField.setAccessible(false);
            autoCompletionPopup.setSkin(skin);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.error("Failed overriding suggestionList height binding", e);
        }

        if (this.configuration.isEnableDarkMode()) {
            autoCompletionPopup.setStyle("""
                    -fx-control-inner-background:#333;
                    -fx-selection-bar-non-focused:#515151;
                    """);
        }
    }

    private ObservableValue<? extends Number> getFileNameColumnSize() {
        return documentTable.widthProperty()
                .subtract(indexColumn.widthProperty())
                .subtract(documentSizeColumn.widthProperty())
                .subtract(documentCreationTimeColumn.widthProperty())
                .subtract(documentModificationTimeColumn.widthProperty())
                .subtract(20);
    }
}
