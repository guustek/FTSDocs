package ftsdocs.configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javafx.scene.paint.Color;

import com.google.gson.annotations.JsonAdapter;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ftsdocs.FTSDocsApplication;
import ftsdocs.model.DocumentType;
import ftsdocs.model.IndexLocation;

@Slf4j
@Component
@Lazy
@Getter
@Setter
public class Configuration {

    public static final String SERVER_CLASS = "ftsdocs.server.SolrEmbeddedServer";

    private static final Set<DocumentType> DEFAULT_DOCUMENT_TYPES = Set.of(
            new DocumentType("PDF file", true, "pdf"),
            new DocumentType("Microsoft Word Document", true, ".doc", "*.DOCX")
    );

    //region Appearance

    private double width;

    private double height;

    private boolean enableDarkMode;

    private Color highlightColor;

    private int contentFontSize;

    //endregion Appearance

    //region Searching

    private boolean enableSuggestions;

    private int maxSearchResults;

    private int maxPhraseHighlights;

    //endregion Searching

    //region Indexing

    private boolean enableFileWatcher;
    private LinkedHashSet<DocumentType> documentTypes;

    //endregion Indexing

    @JsonAdapter(IndexedLocationsAdapter.class)
    private ConcurrentHashMap<String, IndexLocation> indexedLocations;

    public Configuration() {
        this.indexedLocations = new ConcurrentHashMap<>();
        reset();
    }

    public Configuration(Configuration configuration) {
        this.copyFrom(configuration);
    }

    public void reset() {
        this.width = 900;
        this.height = 700;
        this.enableDarkMode = false;
        this.highlightColor = Color.rgb(0, 120, 215);
        this.contentFontSize = 14;

        this.enableSuggestions = true;
        this.maxSearchResults = 100;
        this.maxPhraseHighlights = 100;

        this.enableFileWatcher = true;
        this.documentTypes = new LinkedHashSet<>(DEFAULT_DOCUMENT_TYPES);
    }

    public void copyFrom(Configuration configuration) {
        this.width = configuration.getWidth();
        this.height = configuration.getHeight();
        this.enableDarkMode = configuration.isEnableDarkMode();
        this.highlightColor = configuration.getHighlightColor();
        this.contentFontSize = configuration.getContentFontSize();

        this.enableSuggestions = configuration.isEnableSuggestions();
        this.maxSearchResults = configuration.getMaxSearchResults();
        this.maxPhraseHighlights = configuration.getMaxPhraseHighlights();

        this.enableFileWatcher = configuration.isEnableFileWatcher();
        this.documentTypes = configuration.getDocumentTypes().stream()
                .map(doc -> new DocumentType(doc.getName(), doc.isEnabled(), doc.getExtensions()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        this.indexedLocations = new ConcurrentHashMap<>(configuration.getIndexedLocations());
    }

    public boolean isFileFormatSupported(File file) {
        String extension = FilenameUtils.getExtension(file.getName());
        return documentTypes.stream()
                .anyMatch(type -> type.getExtensions().contains("*." + extension.toLowerCase()));
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

}
