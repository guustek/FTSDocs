package ftsdocs;

import java.io.IOException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class FTSDocsApplication extends Application {

    private SolrServer server;
    private Stage stage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException, InterruptedException {
        initializeServer();

        stage = primaryStage;
        stage.setTitle("FTS Docs");
        Platform.setImplicitExit(true);

        Parent splash = FXMLLoader.load(getClass().getResource("/scene/Splash.fxml"));
        Scene splashScene = new Scene(splash);
        stage.setScene(splashScene);
        stage.show();
    }

    private void initializeServer() {
        long start = System.currentTimeMillis();
        Task<SolrServer> task = new Task<>() {
            @Override
            protected SolrServer call() {
                return SolrServer.getInstance();
            }
        };
        task.setOnSucceeded(event -> {
            server = task.getValue();
            showScene("Main.fxml");
            long time = System.currentTimeMillis() - start;
            System.out.println(time / 1000);
        });
        Thread thread = new Thread(task);
        thread.start();
    }

    private void showScene(String sceneName) {
        try {
            Parent main = FXMLLoader.load(getClass().getResource("/scene/" + sceneName));
            Scene scene = new Scene(main);
            stage.setScene(scene);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() throws Exception {
        if (server != null) {
            server.stop();
        }
        stage.hide();
        System.exit(0);
    }
}
