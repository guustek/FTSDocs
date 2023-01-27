package ftsdocs.view.controller;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ftsdocs.Configuration;
import ftsdocs.model.Document;
import ftsdocs.service.FullTextSearchService;
import ftsdocs.view.View;
import ftsdocs.view.ViewManager;

@Slf4j
@RequiredArgsConstructor
@Component
@Lazy
public class MenuController implements Initializable {

    private final FullTextSearchService ftsService;

    private final Configuration configuration;

    private final ViewManager viewManager;
    @FXML
    private VBox root;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    @FXML
    private void indexFilesButtonClicked() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().addAll(getExtensionFilters());
        List<File> files = chooser
                .showOpenMultipleDialog(this.root.getScene().getWindow());
        if (files != null) {
            index(files);
        }
    }

    @FXML
    private void indexDirectoriesButtonClicked() {
        DirectoryChooser chooser = new DirectoryChooser();
        File directory = chooser.showDialog(this.root.getScene().getWindow());
        if (directory != null) {
            index(Collections.singletonList(directory));
        }
    }

    private Collection<ExtensionFilter> getExtensionFilters() {
        Collection<ExtensionFilter> filters = new ArrayList<>();
        filters.add(new ExtensionFilter("All types", "*.*"));

        List<String> allExtensions = this.configuration.getDocumentTypes().stream()
                .flatMap(type -> type.getExtensions().stream())
                .toList();
        filters.add(new ExtensionFilter("All enabled types", allExtensions));

        filters.addAll(this.configuration.getDocumentTypes().stream()
                .map(type -> new ExtensionFilter(
                        type.getName(),
                        type.getExtensions().stream().toList()))
                .toList());
        return filters;
    }

    @FXML
    private void homeClick() {
        viewManager.changeScene(View.MAIN);
    }

    @FXML
    private void indexedFilesClick() {
        //viewManager.changeScene(Views.MAIN);
    }

    @FXML
    private void settingsClick() {
        viewManager.changeScene(View.SETTINGS);
    }

    @SuppressWarnings("unchecked")
    private void index(List<File> files) {
        this.configuration.getIndexedLocations()
                .addAll(files.stream()
                        .map(File::getAbsolutePath)
                        .toList());

        this.configuration.writeToFile();
        this.ftsService.indexFiles(
                files,
                this.configuration.isEnableFileWatcher(),
                event -> {
                    Collection<Document> value = ((Collection<Document>) event.getSource()
                            .getValue());

                    this.viewManager.showNotification("Information",
                            "Indexed " + value.size() + " files. Check Indexed files for details");

                });
    }
}
