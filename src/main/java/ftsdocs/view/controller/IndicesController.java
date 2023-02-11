package ftsdocs.view.controller;

import ftsdocs.configuration.Configuration;
import ftsdocs.controls.FileContextMenu;
import ftsdocs.model.*;
import ftsdocs.service.FullTextSearchService;
import ftsdocs.view.View;
import ftsdocs.view.ViewManager;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.HyperlinkLabel;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.ResourceBundle;

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
    private final ViewManager viewManager;
    private final FullTextSearchService ftsService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        defineLocationsTable();
    }

    @SuppressWarnings("java:S110")
    private void defineLocationsTable() {
        this.indicesTable.setRoot(new TreeItem<>());
        this.indicesTable.setEditable(false);
        this.indicesTable.setRowFactory(param -> new TreeTableRow<>() {
            @Override
            protected void updateItem(IndexLocation item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setContextMenu(null);
                } else {
                    styleProperty().bind(
                            Bindings.when(new SimpleBooleanProperty(item.isRoot()))
                                    .then("-fx-font-weight: bold;")
                                    .otherwise(""));
                    FileContextMenu menu = new FileContextMenu(item.getRoot());

                    if (item.isRoot()) {
                        MenuItem menuItem = new MenuItem("Re-index location");
                        menuItem.setOnAction(event -> {
                            ftsService.indexLocations(
                                    Collections.singleton(item),
                                    configuration.isEnableFileWatcher(),
                                    ev -> {
                                        Collection<Document> value = ((Collection<Document>) ev.getSource()
                                                .getValue());

                                        HyperlinkLabel indicesViewLink = new HyperlinkLabel(
                                                "Indexed " + value.size() + " files. "
                                                        + "Check ["
                                                        + StringUtils.capitalize(View.INDICES.name().toLowerCase())
                                                        + "] for details");
                                        indicesViewLink.setFocusTraversable(false);
                                        indicesViewLink.setOnAction(linkEvent -> {
                                            Hyperlink source = ((Hyperlink) linkEvent.getSource());
                                            viewManager.changeScene(View.valueOf(source.getText().toUpperCase()));
                                        });

                                        viewManager.showNotification(NotificationTitle.INFORMATION, null, indicesViewLink);
                                    });
                            indicesTable.getRoot().getChildren().clear();
                            populateTable();
                        });
                        menu.getItems().add(menuItem);

                        menuItem = new MenuItem("Delete location");
                        menuItem.setOnAction(event -> {
                            Collection<Path> toDelete = item.getIndexedFiles().stream()
                                    .map(file -> file.getRoot().toPath())
                                    .toList();
                            ftsService.deleteFromIndex(toDelete);
                            configuration.getIndexedLocations().remove(item.getRoot().getPath());
                            configuration.writeToFile();
                            indicesTable.getRoot().getChildren().clear();
                            populateTable();
                        });
                        menu.getItems().add(menuItem);
                    }

                    setContextMenu(menu);
                }
            }
        });

        this.locationColumn.prefWidthProperty().bind(getLocationColumnSize());
        this.locationColumn.setCellValueFactory(
                c -> new ReadOnlyStringWrapper(c.getValue().getValue().getRoot().getPath()));

        this.indexStatusColumn.setCellFactory(param -> new TreeTableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty) {
                    setText(item);
                    IndexStatus status = item.equals("")
                            ? IndexStatus.UNKNOWN
                            : IndexStatus.valueOf(item.replace(" ", "_").toUpperCase());
                    setTooltip(new Tooltip(status.getDescription()));
                } else {
                    setText(null);
                    setTooltip(null);
                }
            }
        });
        this.indexStatusColumn.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        c.getValue().getValue().getIndexStatus().getDisplayName()));

        this.watcherStatusColumn.setCellFactory(param -> new TreeTableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty) {
                    setText(item);
                    WatcherStatus status = item.equals("")
                            ? WatcherStatus.UNKNOWN
                            : WatcherStatus.valueOf(item.replace(" ", "_").toUpperCase());
                    setTooltip(new Tooltip(status.getDescription()));
                } else {
                    setText(null);
                    setTooltip(null);
                }
            }
        });
        this.watcherStatusColumn.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
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
                    TreeItem<IndexLocation> fileItem = new TreeItem<>(file);
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
