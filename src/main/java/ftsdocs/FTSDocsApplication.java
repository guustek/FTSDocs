package ftsdocs;

import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.Locale;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import ftsdocs.model.configuration.Configuration;
import ftsdocs.server.FullTextSearchServer;
import ftsdocs.view.View;
import ftsdocs.view.ViewManager;
import ftsdocs.view.ViewManagerImpl;

@Slf4j
@ComponentScan
public class FTSDocsApplication extends Application {

    public static final String APP_NAME = "FTSDocs";
    //public static final File HOME_DIR = new File(SystemUtils.getUserHome(), APP_NAME);
    public static final File HOME_DIR = new File(APP_NAME);
    public static final File CONFIG_FILE = new File(FTSDocsApplication.HOME_DIR, "config.json");

    public static final Gson GSON = buildGson();
    public static final DateTimeFormatter DATE_TIME_FORMATTER = buildDateTimeFormatter();

    @Getter
    private ConfigurableApplicationContext context;
    @Getter
    private Stage stage;
    @Getter
    private JMetro jmetro;

    private ViewManager viewManager;

    private FullTextSearchServer server;
    private Configuration configuration;

    public static void main(String[] args) {
        log.info("Launching with arguments: {}", Arrays.asList(args));
        launch(args);
    }

    @Override
    public void init() {
        log.info("Starting application at {} on host: {}",
                DATE_TIME_FORMATTER.format(Instant.now()), SystemUtils.getHostName());
        log.info("JAVA_HOME: {}", SystemUtils.getJavaHome());
        log.info("Working directory: {}", SystemUtils.getUserDir());
        log.info("Operating system: {}, {}", SystemUtils.OS_NAME, SystemUtils.OS_ARCH);
        log.info("System language: {}", SystemUtils.USER_LANGUAGE);
        log.info("File system: {}", FileSystemUtils.getFileSystem().getClass().getName());

        this.context = new AnnotationConfigApplicationContext(getClass());

        this.context.getBeanFactory().registerSingleton("configuration", loadConfiguration());
        this.context.getBeanFactory().registerSingleton("application", this);
        this.configuration = this.context.getBean(Configuration.class);

        this.viewManager = new ViewManagerImpl(this, configuration);
        this.context.getBeanFactory().registerSingleton("viewManager", viewManager);

        String[] beans = this.context.getBeanDefinitionNames();
        log.info("Registered spring beans {}", GSON.toJson(beans));

        Style style = configuration.isEnableDarkMode() ? Style.DARK : Style.LIGHT;
        this.jmetro = new JMetro(style);
    }

    @Override
    public void start(Stage primaryStage) throws IOException, InterruptedException {
        Platform.setImplicitExit(true);
        this.stage = primaryStage;
        this.stage.getIcons().add(new Image(getClass().getResourceAsStream("/icon/icon.png")));
        this.stage.setTitle(APP_NAME);
        this.stage.initStyle(StageStyle.UNDECORATED);
        this.viewManager.changeScene(View.SPLASH);
        this.stage.sizeToScene();
        this.stage.centerOnScreen();
        startFullTextSearchServer();
    }

    @Override
    public void stop() throws Exception {
        this.configuration.writeToFile();
        if (this.server != null) {
            this.server.stop();
        }
        this.stage.hide();
        this.context.close();
        this.configuration.writeToFile();
        Platform.exit();
        System.exit(0);
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
            splashStage.close();

            this.stage = new Stage();
            this.stage.getIcons().add(new Image(getClass().getResourceAsStream("/icon/icon.png")));
            this.stage.setTitle(APP_NAME);
            this.stage.initStyle(StageStyle.DECORATED);
            this.viewManager.changeScene(View.MAIN);
            this.stage.centerOnScreen();
            long time = System.currentTimeMillis() - start;
            log.info("Server started in {} seconds", (double) time / 1000);

            this.stage.widthProperty().addListener((observable, oldValue, newValue) -> {
                this.configuration.setWidth((Double) newValue);
                this.configuration.writeToFile();
            });
            this.stage.heightProperty().addListener((observable, oldValue, newValue) -> {
                this.configuration.setHeight((Double) newValue);
                this.configuration.writeToFile();
            });
            setupTray();
        });

        Thread thread = new Thread(task, "Server startup thread");
        thread.start();
    }

    public void setupTray() {
        if (this.configuration.isMinimizeOnClose() && SystemTray.isSupported()) {
            try {
                Platform.setImplicitExit(false);
                SystemTray systemTray = SystemTray.getSystemTray();

                java.awt.Image trayIconImage = Toolkit.getDefaultToolkit()
                        .getImage(getClass().getResource("/icon/icon.png"));
                int trayIconWidth = new TrayIcon(trayIconImage).getSize().width;
                TrayIcon trayIcon = new TrayIcon(
                        trayIconImage.getScaledInstance(trayIconWidth, -1,
                                java.awt.Image.SCALE_SMOOTH),
                        APP_NAME);
                trayIcon.setImageAutoSize(true);
                trayIcon.setToolTip(APP_NAME);
                trayIcon.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(final MouseEvent e) {
                        if (e.getButton() == MouseEvent.BUTTON1) {
                            Platform.runLater(() -> {
                                if (stage.isIconified()) {
                                    stage.setIconified(false);
                                }
                                stage.show();
                                stage.toFront();
                            });
                        }
                    }
                });
                systemTray.add(trayIcon);

                PopupMenu popupMenu = new PopupMenu();

                MenuItem open = new MenuItem("Open");
                open.addActionListener(e -> Platform.runLater(() -> {
                    if (stage.isIconified()) {
                        stage.setIconified(false);
                    }
                    stage.show();
                    stage.toFront();
                }));

                MenuItem exit = new MenuItem("Exit");
                exit.addActionListener(e -> Platform.exit());

                popupMenu.add(open);
                popupMenu.add(exit);

                trayIcon.setPopupMenu(popupMenu);

                stage.setOnCloseRequest(e -> stage.hide());
            } catch (AWTException e) {
                stage.setOnCloseRequest(ev -> Platform.exit());
            }
        } else {
            Platform.setImplicitExit(true);
            stage.setOnCloseRequest(e -> Platform.exit());
        }


    }

    private Configuration loadConfiguration() {
        if (HOME_DIR.exists() && CONFIG_FILE.exists()) {
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

    private static Gson buildGson() {
        return new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .registerTypeAdapter(Instant.class,
                        (JsonSerializer<Instant>) (Instant source, Type typeOfSrc, JsonSerializationContext jsonContext) ->
                                new JsonPrimitive(source.toString()))
                .registerTypeAdapter(Instant.class,
                        (JsonDeserializer<Instant>) (JsonElement json, Type typeOfSrc, JsonDeserializationContext jsonContext) ->
                                Instant.parse(json.getAsString()))
                .create();
    }

    private static DateTimeFormatter buildDateTimeFormatter() {
        return DateTimeFormatter.ofLocalizedDateTime(
                        FormatStyle.MEDIUM)
                .withLocale(Locale.getDefault())
                .withZone(ZoneId.systemDefault());
    }
}
