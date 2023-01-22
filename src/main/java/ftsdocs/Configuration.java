package ftsdocs;

import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.Set;

import javafx.scene.paint.Color;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ftsdocs.model.DocumentType;

@Slf4j
@Component
@Lazy
@Getter
@Setter
public class Configuration {

    public static final String serverClassName = "ftsdocs.solr.SolrEmbeddedServer";

    private static final Set<DocumentType> DEFAULT_DOCUMENT_TYPES = Set.of(
            new DocumentType("PDF file", "pdf"),
            new DocumentType("Microsoft Word Document", ".doc", "DOCX")
    );

    //region Appearance

    private boolean enableDarkMode;

    private Color highlightColor;

    private int contentFontSize;

    //endregion Appearance

    //region Searching

    private int maxSearchResults;

    private int maxPhraseHighlights;

    //endregion Searching

    //region Indexing

    private LinkedHashSet<DocumentType> documentTypes;

    //endregion Indexing
    private LinkedHashSet<String> indexedLocations;

    public Configuration() {
        this.indexedLocations = new LinkedHashSet<>();
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

        this.documentTypes = new LinkedHashSet<>(DEFAULT_DOCUMENT_TYPES);
    }

    public enum Categories {

        APPEARANCE,
        SEARCHING,
        INDEXING;

        public String getDisplayName() {
            return StringUtils.capitalize(this.name().toLowerCase().replace("_", " "));
        }
    }
}
