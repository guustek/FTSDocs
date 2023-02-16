package ftsdocs.view.controls;

import javafx.beans.value.ObservableValue;

import org.controlsfx.control.PropertySheet;
import org.controlsfx.control.ToggleSwitch;
import org.controlsfx.property.editor.AbstractPropertyEditor;

public class BooleanPropertyEditor extends AbstractPropertyEditor<Boolean, ToggleSwitch> {

    public BooleanPropertyEditor(final PropertySheet.Item property) {
        super(property, new ToggleSwitch());
    }

    @Override
    protected ObservableValue<Boolean> getObservableValue() {
        return getEditor().selectedProperty();
    }

    @Override
    public void setValue(final Boolean aBoolean) {
        getEditor().selectedProperty().setValue(aBoolean);
    }
}
