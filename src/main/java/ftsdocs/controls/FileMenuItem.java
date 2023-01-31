package ftsdocs.controls;

import java.io.File;
import java.io.IOException;

import javafx.scene.control.MenuItem;

import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.Notifications;

import ftsdocs.FileSystemUtils;

@Slf4j
public class FileMenuItem extends MenuItem {

    public FileMenuItem(String text, File file) {
        super(text);
        setOnAction(event -> {
            try {
                FileSystemUtils.openDocument(file.toPath());
            } catch (IOException e) {
                log.error("Failed opening {}", file.getAbsolutePath(), e);
                Notifications.create()
                        .owner(getParentPopup().getOwnerWindow())
                        .title("Error")
                        .text(e.getMessage())
                        .show();
            }
        });
    }
}
