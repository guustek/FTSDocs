package ftsdocs;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

import javafx.scene.paint.Color;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Lazy
@Getter
@Setter
public class Configuration {

    public static final String serverClassName = "ftsdocs.solr.SolrEmbeddedServer";

    private boolean enableDarkMode;

    private Color highlightColor;

    private int contentFontSize;

    private int maxSearchResults;

    private int maxPhraseHighlights;


    private Set<String> indexedLocations;

    public Configuration() {
        this.indexedLocations = new HashSet<>();
        reset();
    }

    public void writeToFile() {
        try {
            FieldUtils.writeField(this.highlightColor, "platformPaint", null, true);
            Files.writeString(
                    FTSDocsApplication.CONFIG_FILE.toPath(),
                    FTSDocsApplication.GSON.toJson(this));
        } catch (IOException | IllegalAccessException e) {
            log.info("Encountered an error when writing configuration into config.json file");
        }
    }

    public void reset() {
        this.enableDarkMode = false;
        this.highlightColor = Color.rgb(0, 120, 215);
        this.contentFontSize = 14;
        this.maxSearchResults = 100;
        this.maxPhraseHighlights = 100;
    }

    public static final class Categories {

        public static final String APPEARANCE = "Appearance";
        public static final String SEARCHING = "Searching";

        private Categories() {
        }
    }
}
