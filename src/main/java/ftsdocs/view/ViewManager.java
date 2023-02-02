package ftsdocs.view;

import javafx.scene.Node;

import ftsdocs.model.NotificationTitle;

public interface ViewManager {

    void changeScene(View view);

    void showNotification(NotificationTitle title, String text, Node graphic);

}
