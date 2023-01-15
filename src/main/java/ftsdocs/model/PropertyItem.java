package ftsdocs.model;

import java.util.Optional;

import javafx.beans.value.ObservableValue;

import org.controlsfx.control.PropertySheet.Item;

public abstract class PropertyItem implements Item {

    private final Class<?> type;
    private final String displayName;
    private final String category;

    protected PropertyItem(Class<?> type, String displayName, String category) {
        this.type = type;
        this.displayName = displayName;
        this.category = category;
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
        return "";
    }

    @Override
    public Optional<ObservableValue<?>> getObservableValue() {
        return Optional.empty();
    }
}
