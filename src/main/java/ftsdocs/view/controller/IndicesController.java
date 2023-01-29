package ftsdocs.view.controller;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.BorderPane;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ftsdocs.model.IndexLocation;
import ftsdocs.model.IndexStatus;
import ftsdocs.model.WatcherStatus;

@Slf4j
@RequiredArgsConstructor
@Component
@Lazy
public class IndicesController implements Initializable {

    @FXML
    private BorderPane root;
    @FXML
    private TreeTableView<IndexLocation> indicesTable;
    @FXML
    private TreeTableColumn<Integer, Integer> indexColumn;
    @FXML
    private TreeTableColumn<IndexLocation, String> locationColumn;
    @FXML
    private TreeTableColumn<IndexLocation, String> indexStatusColumn;
    @FXML
    private TreeTableColumn<IndexLocation, String> watcherStatusColumn;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        defineLocationsTable();
        this.indicesTable.setRoot(new TreeItem<>());

        final TreeItem<IndexLocation> childNode1 = new TreeItem<>(
                new IndexLocation("XD", IndexStatus.INDEXED, WatcherStatus.WATCHING));
        final TreeItem<IndexLocation> childNode2 = new TreeItem<>(new IndexLocation("XD"));
        final TreeItem<IndexLocation> childNode3 = new TreeItem<>(new IndexLocation("XD"));

        final TreeItem<IndexLocation> root1 = new TreeItem<>(new IndexLocation("XD"));
        root1.getChildren().addAll(childNode1, childNode2, childNode3);
        final TreeItem<IndexLocation> root2 = new TreeItem<>(new IndexLocation("XD"));

        this.indicesTable.getRoot().getChildren().addAll(root1, root2);
    }

    private void defineLocationsTable() {
        this.indicesTable.setEditable(false);
        this.indexColumn.setCellFactory(column -> new TreeTableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty) {
                    setText(String.valueOf(getIndex() + 1));
                }
            }
        });
        this.locationColumn.prefWidthProperty().bind(getLocationColumnSize());
        this.locationColumn.setCellValueFactory(
                c -> new ReadOnlyStringWrapper(c.getValue().getValue().getPath()));
        this.indexStatusColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().getValue().getIndexStatus().getDisplayName()));
        this.watcherStatusColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().getValue().getWatcherStatus().getDisplayName()));
    }

    private ObservableValue<? extends Number> getLocationColumnSize() {
        return this.indicesTable.widthProperty()
                .subtract(this.indexColumn.widthProperty())
                .subtract(this.indexStatusColumn.widthProperty())
                .subtract(this.watcherStatusColumn.widthProperty())
                .subtract(20);
    }
}
