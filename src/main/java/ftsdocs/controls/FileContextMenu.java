package ftsdocs.controls;

import java.io.File;

import javafx.scene.control.ContextMenu;

public class FileContextMenu extends ContextMenu {

    public FileContextMenu(File file) {
        super();

        getItems().add(new FileMenuItem("Open", file));

        if (!file.isDirectory()) {
            getItems().add(new FileMenuItem("Open parent directory", file.getParentFile()));
        }

    }
}
