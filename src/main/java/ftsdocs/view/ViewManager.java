package ftsdocs.view;

import ftsdocs.model.NotificationTitle;
import javafx.scene.Node;

public interface ViewManager {

    void changeScene(View view);

    void showNotification(NotificationTitle title, String text, Node graphic);

}
