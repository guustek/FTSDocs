package ftsdocs.view.controls;

import javafx.beans.value.ObservableValue;
import org.controlsfx.control.PropertySheet.Item;
import org.controlsfx.property.editor.PropertyEditor;

import java.util.Optional;

public abstract class PropertyItem implements Item {

    private final Class<?> type;
    private final String displayName;
    private final String description;
    private final String category;
    private final Class<?> editorType;

    protected PropertyItem(Class<?> type, String displayName, String description, String category) {
        this(type, displayName, description, category, null);
    }

    protected PropertyItem(Class<?> type, String displayName, String description, String category,
            Class<?> editorType) {
        this.type = type;
        this.displayName = displayName;
        this.description = description;
        this.category = category;
        this.editorType = editorType;
    }

    @Override
    public Class<?> getType() {
        return type;
    }

    @Override
    public String getCategory() {
        return this.category;
    }

    @Override
    public String getName() {
        return this.displayName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Optional<ObservableValue<?>> getObservableValue() {
        return Optional.empty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<Class<? extends PropertyEditor<?>>> getPropertyEditorClass() {
        return editorType != null ? Optional.of((Class<? extends PropertyEditor<?>>) editorType)
                : Item.super.getPropertyEditorClass();
    }

}
