package ftsdocs;

import java.io.IOException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.JMetroStyleClass;
import jfxtras.styles.jmetro.Style;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrClient;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ComponentScan
public class FTSDocsApplication extends Application {

    private ConfigurableApplicationContext context;
    private SolrServer server;
    private Stage stage;
    private JMetro jmetro;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() {
        this.jmetro = new JMetro(Style.LIGHT);
        this.context = new AnnotationConfigApplicationContext(getClass());
        String[] beans = this.context.getBeanDefinitionNames();
        log.info("Registered spring beans {}", GsonUtils.toJson(beans));
    }

    @Override
    public void start(Stage primaryStage) throws IOException, InterruptedException {
        this.stage = primaryStage;
        this.stage.setTitle("FTS Docs");
        this.stage.initStyle(StageStyle.UNDECORATED);
        changeScene("splash.fxml");
        startSolrServer();
    }

    private void startSolrServer() {
        long start = System.currentTimeMillis();
        Task<SolrServer> task = new Task<>() {
            @Override
            protected SolrServer call() {
                return SolrServer.getServer();
            }
        };
        task.setOnSucceeded(event -> {
            this.server = task.getValue();
            var splashStage = this.stage;
            this.stage = new Stage();
            this.stage.initStyle(StageStyle.DECORATED);
            changeScene("main.fxml");
            splashStage.close();
            long time = System.currentTimeMillis() - start;
            log.info("Server started in {} seconds", (double) time / 1000);
        });
        Thread thread = new Thread(task, "Solr startup thread");
        thread.start();
    }

    private void changeScene(String view) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/" + view));
            loader.setControllerFactory(this.context::getBean);
            Parent root = loader.load();
            Scene scene = new Scene(root);
            jmetro.setScene(scene);
            this.stage.setScene(scene);
            this.stage.centerOnScreen();
            this.stage.show();
        } catch (IOException e) {
            log.error("Error while changing view", e);
        }
    }

    @Override
    public void stop() throws Exception {
        if (server != null) {
            server.stop();
        }
        stage.hide();
        context.close();
        Platform.exit();
        System.exit(0);
    }
}
