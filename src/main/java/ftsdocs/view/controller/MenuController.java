package ftsdocs.view.controller;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ftsdocs.DisplayUtils;
import ftsdocs.model.Document;
import ftsdocs.service.FullTextSearchService;
import ftsdocs.view.ViewManager;
import ftsdocs.view.Views;

@Slf4j
@RequiredArgsConstructor
@Component
@Lazy
public class MenuController implements Initializable {

    private final FullTextSearchService ftsService;

    private final ViewManager viewManager;
    @FXML
    private VBox root;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    @FXML
    private void indexFilesButtonClicked(MouseEvent mouseEvent) {
        FileChooser chooser = new FileChooser();
        List<File> files = chooser
                .showOpenMultipleDialog(this.root.getScene().getWindow());
        if (files != null) {
            index(files);
        }
    }

    @FXML
    private void indexDirectoriesButtonClicked(MouseEvent mouseEvent) {
        DirectoryChooser chooser = new DirectoryChooser();
        File directory = chooser.showDialog(this.root.getScene().getWindow());
        if (directory != null) {
            index(Collections.singletonList(directory));
        }

    }

    @FXML
    private void homeClick(ActionEvent actionEvent) {
        viewManager.changeScene(Views.MAIN);
    }

    @FXML
    private void indexedFilesClick(ActionEvent actionEvent) {
        //viewManager.changeScene(Views.MAIN);
    }

    @FXML
    private void settingsClick(ActionEvent actionEvent) {
        viewManager.changeScene(Views.SETTINGS);
    }

    private void index(List<File> files) {
        this.ftsService.indexFiles(files, event -> {
            Collection<Document> value = ((Collection<Document>) event.getSource().getValue());
            int selectedCount = files.size();
            if (files.size() == 1) {
                File dir = files.get(0);
                if (dir.isDirectory()) {
                    try (Stream<Path> pathStream = Files.walk(dir.toPath())) {
                        selectedCount = (int) pathStream
                                .map(Path::toFile)
                                .filter(File::isFile)
                                .count();
                    } catch (Exception e) {
                        log.error("Error while reading directory tree", e);
                    }
                }
            }
            DisplayUtils.showNotification(this.root, "Information",
                    "Successfully indexed " + value.size() + " out of " + selectedCount
                            + " files selected. Check Indexed files for details");

        }, true);
    }
}
