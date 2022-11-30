package ftsdocs;

import java.io.IOException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
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

    public static void main(String[] args) {
       launch(args);
    }

    @Override
    public void init() {
        this.context = new AnnotationConfigApplicationContext(getClass());
        String[] beans = this.context.getBeanDefinitionNames();
        log.info("Registered spring beans {}", GsonUtils.toJson(beans));
    }

    @Override
    public void start(Stage primaryStage) throws IOException, InterruptedException {
        Platform.setImplicitExit(true);
        this.stage = primaryStage;
        this.stage.setTitle("FTS Docs");
        changeView("splash.fxml");
        initializeServer();
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
            this.server = task.getValue();
            changeView("main.fxml");
            long time = System.currentTimeMillis() - start;
            log.info("Server started in {} seconds", time / 1000);
        });
        Thread thread = new Thread(task, "Solr initialization thread");
        thread.start();
    }

    private void changeView(String view) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/" + view));
            loader.setControllerFactory(this.context::getBean);
            Parent root = loader.load();
            this.stage.setScene(new Scene(root));
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
