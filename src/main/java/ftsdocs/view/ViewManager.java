package ftsdocs.view;

import javafx.scene.Node;

public interface ViewManager {

    void changeScene(View view);

    void showNotification(String title, String text, Node graphic);

}
