package ftsdocs.view.controller;

import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ftsdocs.configuration.Configuration;
import ftsdocs.model.IndexLocation;

@Slf4j
@RequiredArgsConstructor
@Component
@Lazy
public class IndicesController implements Initializable {

    @FXML
    private TreeTableView<IndexLocation> indicesTable;
    @FXML
    private TreeTableColumn<IndexLocation, String> locationColumn;
    @FXML
    private TreeTableColumn<IndexLocation, String> indexStatusColumn;
    @FXML
    private TreeTableColumn<IndexLocation, String> watcherStatusColumn;

    private final Configuration configuration;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        defineLocationsTable();
    }

    private void defineLocationsTable() {
        this.indicesTable.setRoot(new TreeItem<>());
        this.indicesTable.setEditable(false);
        this.locationColumn.prefWidthProperty().bind(getLocationColumnSize());
        this.locationColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getValue().getRoot().getPath()));
        this.indexStatusColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().getValue().getIndexStatus().getDisplayName()));
        this.watcherStatusColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().getValue().getWatcherStatus().getDisplayName()));
        populateTable();
    }

    private void populateTable() {
        TreeItem<IndexLocation> root = this.indicesTable.getRoot();
        Map<String, IndexLocation> indexedLocations = this.configuration.getIndexedLocations();
        indexedLocations.values().forEach(loc -> {
            TreeItem<IndexLocation> locationItem = new TreeItem<>(loc);
            root.getChildren().add(locationItem);

            if (loc.getRoot().isDirectory() && !loc.getIndexedFiles().isEmpty()) {
                loc.getIndexedFiles().forEach(file -> {
                    TreeItem<IndexLocation> fileItem = new TreeItem<>(new IndexLocation(file));
                    locationItem.getChildren().add(fileItem);
                });
            }

        });
    }

    private ObservableValue<? extends Number> getLocationColumnSize() {
        return this.indicesTable.widthProperty()
                .subtract(this.indexStatusColumn.widthProperty())
                .subtract(this.watcherStatusColumn.widthProperty())
                .subtract(20);
    }
}
