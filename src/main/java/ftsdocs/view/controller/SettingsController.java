package ftsdocs.view.controller;

import java.net.URL;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.Set;

import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.paint.Color;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.PropertySheet;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ftsdocs.configuration.Category;
import ftsdocs.configuration.Configuration;
import ftsdocs.controls.BooleanPropertyEditor;
import ftsdocs.controls.CheckComboBoxEditor;
import ftsdocs.model.DocumentType;
import ftsdocs.model.NotificationTitle;
import ftsdocs.model.PropertyItem;
import ftsdocs.view.View;
import ftsdocs.view.ViewManager;

@Slf4j
@RequiredArgsConstructor
@Component
@Lazy
public class SettingsController implements Initializable {

    @FXML
    private PropertySheet propertySheet;

    private final Configuration configuration;

    private Configuration tempConfiguration;

    private final ViewManager viewManager;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        tempConfiguration = new Configuration(configuration);
        this.propertySheet.getStyleClass().clear();
        this.propertySheet.getStyleClass().add("background");
        initializeProperties();
    }

    @FXML
    private void resetClick() {
        tempConfiguration.copyFrom(configuration);
        viewManager.showNotification(NotificationTitle.INFORMATION, "Settings changes discarded", null);
        viewManager.changeScene(View.SETTINGS);
    }

    @FXML
    private void resetDefaultClick() {
        configuration.reset();
        configuration.writeToFile();
        tempConfiguration.copyFrom(configuration);
        viewManager.showNotification(NotificationTitle.INFORMATION, "Settings restarted to defaults", null);
        viewManager.changeScene(View.SETTINGS);
    }

    @FXML
    private void applyClick() {
        configuration.copyFrom(tempConfiguration);
        configuration.writeToFile();
        viewManager.showNotification(NotificationTitle.INFORMATION, "Settings saved", null);
        viewManager.changeScene(View.SETTINGS);
    }

    @SuppressWarnings("java:S2696")
    private void initializeProperties() {

        //region Appearance

        this.propertySheet.getItems()
                .add(new PropertyItem(
                        boolean.class,
                        "Enable dark mode",
                        "Enables/disables dark theme of application.",
                        Category.APPEARANCE.getDisplayName(),
                        BooleanPropertyEditor.class) {
                    @Override
                    public Object getValue() {
                        return tempConfiguration.isEnableDarkMode();
                    }

                    @Override
                    public void setValue(Object value) {
                        tempConfiguration.setEnableDarkMode((boolean) value);
                    }
                });

        this.propertySheet.getItems()
                .add(new PropertyItem(
                        Color.class,
                        "Phrase highlight color",
                        "Color that will be used to highlight found parts of document content in it's preview.",
                        Category.APPEARANCE.getDisplayName()) {
                    @Override
                    public Object getValue() {
                        return tempConfiguration.getHighlightColor();
                    }

                    @Override
                    public void setValue(Object value) {
                        tempConfiguration.setHighlightColor((Color) value);
                    }
                });

        this.propertySheet.getItems().add(
                new PropertyItem(
                        int.class,
                        "Document content font size",
                        "Font size of text displayed in document content preview.",
                        Category.APPEARANCE.getDisplayName()) {
                    @Override
                    public Object getValue() {
                        return tempConfiguration.getContentFontSize();
                    }

                    @Override
                    public void setValue(Object value) {
                        tempConfiguration.setContentFontSize((int) value);
                    }
                });

        //endregion Appearance

        //region Searching

        propertySheet.getItems().add(new PropertyItem(
                boolean.class,
                "Enable suggestions",
                "Enables display of autocompletion suggestions of possible search phrases",
                Category.SEARCHING.getDisplayName(),
                BooleanPropertyEditor.class) {
            @Override
            public Object getValue() {
                return tempConfiguration.isEnableSuggestions();
            }

            @Override
            public void setValue(Object value) {
                tempConfiguration.setEnableSuggestions((boolean) value);
            }
        });

        propertySheet.getItems()
                .add(new PropertyItem(
                        int.class,
                        "Max search results",
                        "Max number of documents returned from query.",
                        Category.SEARCHING.getDisplayName()) {
                    @Override
                    public Object getValue() {
                        return tempConfiguration.getMaxSearchResults();
                    }

                    @Override
                    public void setValue(Object value) {
                        tempConfiguration.setMaxSearchResults((int) value);
                    }
                });

        propertySheet.getItems()
                .add(new PropertyItem(
                        int.class,
                        "Max phrase highlights",
                        "Max number of found parts of document content to be highlighted in preview.",
                        Category.SEARCHING.getDisplayName()) {
                    @Override
                    public Object getValue() {
                        return tempConfiguration.getMaxPhraseHighlights();
                    }

                    @Override
                    public void setValue(Object value) {
                        tempConfiguration.setMaxPhraseHighlights((int) value);
                    }
                });

        //endregion Searching

        //region Indexing

        propertySheet.getItems().add(
                new PropertyItem(
                        boolean.class,
                        "Enable file watcher",
                        """
                                Enable/disable automatic updates of indices based on file system events.
                                Improves performance if disabled
                                Disable it if indexed locations and files are not expected to be modified after indexing.
                                Changes will apply after application restart
                                """,
                        Category.INDEXING.getDisplayName(),
                        BooleanPropertyEditor.class) {
                    @Override
                    public Object getValue() {
                        return tempConfiguration.isEnableFileWatcher();
                    }

                    @Override
                    public void setValue(Object value) {
                        tempConfiguration.setEnableFileWatcher(((boolean) value));
                    }
                }
        );

        propertySheet.getItems().add(
                new PropertyItem(
                        Set.class,
                        "Document types",
                        """
                                Set of document types to be indexed and searched upon.
                                Indexing a single file with different extension will still index that file but it won't show in the results.
                                """,
                        Category.INDEXING.getDisplayName(),
                        CheckComboBoxEditor.class) {

                    @Override
                    public Object getValue() {
                        return new SimpleListProperty<>(FXCollections.observableArrayList(
                                tempConfiguration.getDocumentTypes()));
                    }

                    @SuppressWarnings("unchecked")
                    @Override
                    public void setValue(Object value) {
                        Collection<DocumentType> selectedDocumentTypes = ((Collection<DocumentType>) value);
                        tempConfiguration.getDocumentTypes().forEach(
                                doc -> doc.setEnabled(selectedDocumentTypes.contains(doc)));
                    }
                });

        //endregion Indexing
    }
}
