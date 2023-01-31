package ftsdocs.view;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import jfxtras.styles.jmetro.Style;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.Notifications;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ftsdocs.configuration.Configuration;
import ftsdocs.FTSDocsApplication;

@Slf4j
@RequiredArgsConstructor
@Component
@Lazy
public class ViewManagerImpl implements ViewManager {

    private final FTSDocsApplication application;
    private final Configuration configuration;

    public void changeScene(View view) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/" + view.name().toLowerCase() + ".fxml"));
            loader.setControllerFactory(this.application.getContext()::getBean);
            Parent root = loader.load();
            Scene scene = new Scene(root);

            Style style = this.configuration.isEnableDarkMode() ? Style.DARK : Style.LIGHT;
            this.application.getJmetro().setStyle(style);
            this.application.getJmetro().setScene(scene);
            String globalCssName =
                    this.application.getJmetro().getStyle() == Style.DARK ? "global-dark.css"
                            : "global.css";
            scene.getStylesheets()
                    .add(this.getClass().getResource("/css/" + globalCssName).toExternalForm());

            Stage stage = this.application.getStage();
            stage.setScene(scene);

            double height = stage.getHeight();
            double width = stage.getWidth();

            stage.setHeight(height);
            stage.setWidth(width);

            stage.show();

            log.info("Changed view to " + view);

        } catch (IOException e) {
            log.error("Error while changing view", e);
        }
    }

    @Override
    public void showNotification(@NonNull String title, String text, Node graphic) {
        Notifications notification = Notifications.create()
                .owner(this.application.getStage())
                .title(title);
        if (text != null) {
            notification.text(text);
        }
        if (graphic != null) {
            notification.graphic(graphic);
        }
        notification.show();
    }
}
