package ftsdocs;

import java.io.File;
import java.util.Collections;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;

import org.apache.solr.common.SolrDocumentList;

public class Controller {

    public TextField searchTextField;
    public Button searchButton;
    public Button indexButton;
    public TextField fileNameField;
    public ListView<String> resultListView;

    private List<File> files = Collections.emptyList();

    public void onSearchButtonClick() {
        String searchText = searchTextField.getText();
        if (searchText.isEmpty())
            searchText = "*";
        SolrDocumentList result = Main.search(searchText);
        List<String> fileNames = result.stream().map(doc -> (String) doc.getFieldValue("id")).toList();
        resultListView.setItems(FXCollections.observableList(fileNames));
    }

    public void onFileChooseAction() {
        FileChooser fileChooser = new FileChooser();
        this.files = fileChooser.showOpenMultipleDialog(fileNameField.getScene().getWindow());
    }

    public void onIndexButtonClick() {
        files.forEach(file -> Main.indexFile(file.getPath()));
    }
}

