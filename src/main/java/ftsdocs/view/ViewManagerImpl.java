package ftsdocs.view;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import jfxtras.styles.jmetro.Style;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ftsdocs.Configuration;
import ftsdocs.FTSDocsApplication;

@Slf4j
@RequiredArgsConstructor
@Component
@Lazy
public class ViewManagerImpl implements ViewManager {

    private final FTSDocsApplication application;

    private final Configuration configuration;

    public void changeScene(String viewName) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/" + viewName + ".fxml"));
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
            stage.show();
            log.info("Changed view to " + viewName);
        } catch (IOException e) {
            log.error("Error while changing view", e);
        }
    }
}
