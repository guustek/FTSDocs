package ftsdocs.controls;

import java.util.ArrayList;
import java.util.Collection;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.controlsfx.control.CheckComboBox;
import org.controlsfx.control.PropertySheet.Item;
import org.controlsfx.property.editor.AbstractPropertyEditor;

import ftsdocs.model.Checkable;

public class CheckComboBoxEditor<T extends Checkable> extends
        AbstractPropertyEditor<ObservableList<T>, CheckComboBox<T>> {

    private ListProperty<T> list;

    private boolean initialized = false;

    @SuppressWarnings("unchecked")
    public CheckComboBoxEditor(Item property) {
        super(property, new CheckComboBox<>());
        this.initialized = false;
        ObservableValue<T> observable = (ObservableValue<T>) property.getValue();
        Collection<T> values = (Collection<T>) observable.getValue();
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
    protected ListProperty<T> getObservableValue() {
        if (this.list == null && !this.initialized) {
            ObservableValue<T> observable = (ObservableValue<T>) getProperty().getValue();
            Collection<T> values = (Collection<T>) observable.getValue();
            this.list = new SimpleListProperty<>(
                    FXCollections.observableArrayList(new ArrayList<>(values)));
        }
        return this.list;
    }

    @Override
    public void setValue(ObservableList<T> value) {
        value.stream()
                .filter(Checkable::isChecked)
                .forEach(v -> this.getEditor().getCheckModel().check(v));
    }
}
