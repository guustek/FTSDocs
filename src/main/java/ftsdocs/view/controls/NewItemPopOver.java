package ftsdocs.view.controls;

import java.io.IOException;
import java.util.function.Consumer;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

import org.controlsfx.control.PopOver;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;
import org.controlsfx.validation.decoration.CompoundValidationDecoration;
import org.controlsfx.validation.decoration.GraphicValidationDecoration;
import org.controlsfx.validation.decoration.StyleClassValidationDecoration;

import ftsdocs.model.DocumentType;

public class NewItemPopOver extends PopOver {

    @FXML
    private TextField nameTextField;
    @FXML
    private TextField extensionsTextField;
    @FXML
    private Button applyButton;

    private Consumer<DocumentType> onApplyCallback;
    private final ValidationSupport validationSupport;

    public NewItemPopOver() {
        super();
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/new_format_form.fxml"));
            loader.setController(this);
            Node content = loader.load();
            setContentNode(content);
        } catch (IOException ignored) {
        }

        validationSupport = new ValidationSupport();
        validationSupport.setValidationDecorator(
                new CompoundValidationDecoration(new StyleClassValidationDecoration(),
                        new GraphicValidationDecoration()));
        validationSupport.registerValidator(nameTextField, false,
                Validator.createEmptyValidator("Name is required!"));
        validationSupport.registerValidator(extensionsTextField, false,
                Validator.createEmptyValidator("Extensions are required!"));
        validationSupport.initInitialDecoration();
        validationSupport.setErrorDecorationEnabled(false);

        this.applyButton.setOnMouseClicked(event -> {
            if (onApplyCallback != null) {
                validationSupport.setErrorDecorationEnabled(true);
                validationSupport.revalidate();
                validationSupport.redecorate();
                if (validationSupport.isInvalid() != null && !validationSupport.isInvalid()) {
                    String name = nameTextField.getText();
                    String[] extensions = extensionsTextField.getText().strip().split(",");
                    DocumentType documentType = new DocumentType(name, true, extensions);
                    onApplyCallback.accept(documentType);
                    hide();
                }
            }
        });
    }

    public void setOnApplyCallback(Consumer<DocumentType> onApplyCallback) {
        this.onApplyCallback = onApplyCallback;
    }
}

