package ftsdocs.view.controller;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.PropertySheet;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ftsdocs.Configuration;
import ftsdocs.Configuration.Categories;
import ftsdocs.model.PropertyItem;
import ftsdocs.view.ViewManager;

@Slf4j
@RequiredArgsConstructor
@Component
@Lazy
public class SettingsController implements Initializable {

    @FXML
    private BorderPane root;
    @FXML
    private PropertySheet propertySheet;

    private final Configuration configuration;

    private final ViewManager viewManager;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.propertySheet.getStyleClass().clear();
        this.propertySheet.getStyleClass().add("background");
        initializeProperties();
    }

    private void initializeProperties() {
        //region Appearance

        this.propertySheet.getItems()
                .add(new PropertyItem(
                        boolean.class,
                        "Enable dark mode",
                        Categories.APPEARANCE) {
                    @Override
                    public Object getValue() {
                        return configuration.isEnableDarkMode();
                    }

                    @Override
                    public void setValue(Object value) {
                        configuration.setEnableDarkMode((boolean) value);
                        configuration.writeToFile();
                    }
                });

        this.propertySheet.getItems()
                .add(new PropertyItem(
                        Color.class,
                        "Phrase highlight color",
                        Categories.APPEARANCE) {
                    @Override
                    public Object getValue() {
                        return configuration.getHighlightColor();
                    }

                    @Override
                    public void setValue(Object value) {
                        configuration.setHighlightColor((Color) value);
                        configuration.writeToFile();
                    }
                });

        this.propertySheet.getItems().add(
                new PropertyItem(
                        int.class,
                        "Document content font size",
                        Categories.APPEARANCE) {
                    @Override
                    public Object getValue() {
                        return configuration.getContentFontSize();
                    }

                    @Override
                    public void setValue(Object value) {
                        configuration.setContentFontSize((int) value);
                        configuration.writeToFile();
                    }
                });

        //endregion Appearance

        //region Searching

        propertySheet.getItems()
                .add(new PropertyItem(
                        int.class,
                        "Max search results",
                        Categories.SEARCHING) {
                    @Override
                    public Object getValue() {
                        return configuration.getMaxSearchResults();
                    }

                    @Override
                    public void setValue(Object value) {
                        configuration.setMaxSearchResults((int) value);
                        configuration.writeToFile();
                    }
                });

        propertySheet.getItems()
                .add(new PropertyItem(
                        int.class,
                        "Max phrase highlights",
                        Categories.SEARCHING) {
                    @Override
                    public Object getValue() {
                        return configuration.getMaxPhraseHighlights();
                    }

                    @Override
                    public void setValue(Object value) {
                        configuration.setMaxPhraseHighlights((int) value);
                        configuration.writeToFile();
                    }
                });

        //endregion Searching
    }
}
