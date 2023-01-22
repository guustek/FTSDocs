package ftsdocs.view.controller;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

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
import ftsdocs.view.View;
import ftsdocs.view.ViewManager;

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
        viewManager.changeScene(View.MAIN);
    }

    @FXML
    private void indexedFilesClick(ActionEvent actionEvent) {
        //viewManager.changeScene(Views.MAIN);
    }

    @FXML
    private void settingsClick(ActionEvent actionEvent) {
        viewManager.changeScene(View.SETTINGS);
    }

    private void index(List<File> files) {
        this.ftsService.indexFiles(files, event -> {
            Collection<Document> value = ((Collection<Document>) event.getSource().getValue());

            DisplayUtils.showNotification(this.root, "Information",
                    "Indexed " + value.size() + " files. Check Indexed files for details");

        }, true);
    }
}
