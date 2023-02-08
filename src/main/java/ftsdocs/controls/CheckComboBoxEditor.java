package ftsdocs.controls;

import java.util.ArrayList;
import java.util.Collection;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

import org.controlsfx.control.CheckComboBox;
import org.controlsfx.control.PropertySheet.Item;
import org.controlsfx.property.editor.AbstractPropertyEditor;

import ftsdocs.model.Checkable;
import ftsdocs.model.DocumentType;

public class CheckComboBoxEditor extends
        AbstractPropertyEditor<ObservableList<DocumentType>, CheckComboBox<DocumentType>> {

    private ListProperty<DocumentType> list;

    private boolean initialized;

    @SuppressWarnings("unchecked")
    public CheckComboBoxEditor(Item property) {
        super(property, new CheckComboBox<>());
        this.initialized = false;

        MenuItem addFormatMenuItem = new MenuItem("Add new document format");
        addFormatMenuItem.setOnAction(event -> {
            NewItemPopOver popover = new NewItemPopOver();
            popover.getStyleClass().add("background");
            popover.setOnApplyCallback(documentType -> {
                getEditor().getItems().add(documentType);
                getEditor().getCheckModel().check(documentType);
            });
            popover.show(addFormatMenuItem.getParentPopup().getOwnerWindow());

        });
        this.getEditor().setContextMenu(new ContextMenu(addFormatMenuItem));
        ObservableValue<DocumentType> observable = (ObservableValue<DocumentType>) property.getValue();
        Collection<DocumentType> values = (Collection<DocumentType>) observable.getValue();
        this.getEditor().getItems().setAll(values);
        this.getEditor().getCheckModel().clearChecks();
        values.stream()
                .filter(Checkable::isChecked)
                .forEach(v -> this.getEditor().getCheckModel().check(v));
        this.list.setValue(this.getEditor().getCheckModel().getCheckedItems());
        this.initialized = true;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected ListProperty<DocumentType> getObservableValue() {
        if (this.list == null && !this.initialized) {
            ObservableValue<DocumentType> observable = (ObservableValue<DocumentType>) getProperty().getValue();
            Collection<DocumentType> values = (Collection<DocumentType>) observable.getValue();
            this.list = new SimpleListProperty<>(
                    FXCollections.observableArrayList(new ArrayList<>(values)));
        }
        return this.list;
    }

    @Override
    public void setValue(ObservableList<DocumentType> value) {
        value.stream()
                .filter(Checkable::isChecked)
                .forEach(v -> this.getEditor().getCheckModel().check(v));
    }
}
