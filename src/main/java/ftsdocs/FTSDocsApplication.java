package ftsdocs;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class FTSDocsApplication extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {

        Parent root = FXMLLoader.load(getClass().getResource("/scene/main.fxml"));
        Scene scene = new Scene(root, Color.BLACK);
        primaryStage.setTitle("FTS Docs");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
