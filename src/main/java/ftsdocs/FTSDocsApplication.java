package ftsdocs;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.Style;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import ftsdocs.server.FullTextSearchServer;

@Slf4j
@ComponentScan
public class FTSDocsApplication extends Application {

    private ConfigurableApplicationContext context;
    private FullTextSearchServer server;
    private Stage stage;
    private JMetro jmetro;

    public static final String APP_NAME = "FTSDocs";
    //public static final File HOME_DIR = new File(SystemUtils.getUserHome(), APP_NAME);
    public static final File HOME_DIR = new File(APP_NAME);
    public static final File CONFIG_FILE = new File(FTSDocsApplication.HOME_DIR, "config.json");

    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .registerTypeAdapter(Instant.class,
                    (JsonSerializer<Instant>) (Instant date, Type typeOfSrc, JsonSerializationContext jsonContext) ->
                            new JsonPrimitive(date.toString()))
            .registerTypeAdapter(Instant.class,
                    (JsonDeserializer<Instant>) (JsonElement json, Type typeOfSrc, JsonDeserializationContext jsonContext) ->
                            Instant.parse(json.getAsString()))
            .create();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() {
        log.info("Starting application at {} on host: {}",
                DisplayUtils.dateTimeFormatter.format(Instant.now()), SystemUtils.getHostName());
        log.info("JAVA_HOME: {}", SystemUtils.getJavaHome());
        log.info("Working directory: {}", SystemUtils.getUserDir());
        log.info("Operating system: {}, {}", SystemUtils.OS_NAME, SystemUtils.OS_ARCH);
        this.context = new AnnotationConfigApplicationContext(getClass());
        this.context.getBeanFactory().registerSingleton("configuration", loadConfiguration());
        Configuration configuration = this.context.getBean(Configuration.class);
        String[] beans = this.context.getBeanDefinitionNames();
        log.info("Registered spring beans {}", GSON.toJson(beans));

        Style style = configuration.isDarkModeEnabled() ? Style.DARK : Style.LIGHT;
        this.jmetro = new JMetro(style);
    }

    @Override
    public void start(Stage primaryStage) throws IOException, InterruptedException {
        Platform.setImplicitExit(true);
        this.stage = primaryStage;
        this.stage.setTitle(APP_NAME);
        this.stage.initStyle(StageStyle.UNDECORATED);
        changeScene("splash.fxml");
        startFullTextSearchServer();
    }

    private void startFullTextSearchServer() {
        long start = System.currentTimeMillis();
        Task<FullTextSearchServer> task = new Task<>() {
            @Override
            protected FullTextSearchServer call() {
                return context.getBean(FullTextSearchServer.class);
            }
        };
        task.setOnSucceeded(event -> {
            this.server = task.getValue();
            Stage splashStage = this.stage;
            this.stage = new Stage();
            this.stage.initStyle(StageStyle.DECORATED);
            changeScene("main.fxml");
            splashStage.close();
            long time = System.currentTimeMillis() - start;
            log.info("Server started in {} seconds", (double) time / 1000);
        });
        Thread thread = new Thread(task, "Server startup thread");
        thread.start();
    }

    private void changeScene(String view) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/" + view));
            loader.setControllerFactory(this.context::getBean);
            Parent root = loader.load();
            Scene scene = new Scene(root);

            this.jmetro.setScene(scene);
            String globalCssName =
                    this.jmetro.getStyle() == Style.DARK ? "global-dark.css" : "global.css";
            scene.getStylesheets()
                    .add(this.getClass().getResource("/css/" + globalCssName).toExternalForm());

            this.stage.setScene(scene);
            this.stage.centerOnScreen();
            this.stage.show();
            this.stage.setMinWidth(this.stage.getWidth());
            this.stage.setMinHeight(this.stage.getHeight());
        } catch (IOException e) {
            log.error("Error while changing view", e);
        }
    }

    private ftsdocs.Configuration loadConfiguration() {
        if (HOME_DIR.exists()) {
            try {
                String configJson = Files.readString(
                        CONFIG_FILE.toPath(),
                        StandardCharsets.UTF_8
                );
                return GSON.fromJson(configJson, Configuration.class);
            } catch (IOException e) {
                log.info(
                        "Encountered an error when reading config.json file, using default configuration");
            }
        }
        if (HOME_DIR.mkdir()) {
            log.info("Created home directory in {}", HOME_DIR);
        }
        Configuration defaultConfig = new Configuration();
        defaultConfig.writeToFile();
        return defaultConfig;
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
