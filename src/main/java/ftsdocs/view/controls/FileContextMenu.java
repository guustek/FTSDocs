package ftsdocs.view.controls;

import java.io.File;
import java.io.IOException;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.Notifications;

import ftsdocs.FileSystemUtils;

@Slf4j
public class FileContextMenu extends ContextMenu {

    public FileContextMenu(File file) {
        super();

        MenuItem menuItem = new MenuItem("Open");
        menuItem.setOnAction(event -> openFile(file));
        getItems().add(menuItem);

        if(!file.isDirectory()){
            menuItem = new MenuItem("Open parent directory");
            menuItem.setOnAction(event -> openFile(file.getParentFile()));
            getItems().add(menuItem);
        }
    }

    private void openFile(File file) {
        try {
            FileSystemUtils.openDocument(file.toPath());
        } catch (IOException e) {
            log.error("Failed opening {}", file.getAbsolutePath(), e);
            Notifications.create()
                    .owner(getOwnerWindow())
                    .title("Error")
                    .text(e.getMessage())
                    .show();
        }
    }
}
