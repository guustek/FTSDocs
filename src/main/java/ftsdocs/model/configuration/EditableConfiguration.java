package ftsdocs.model.configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.scene.paint.Color;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import ftsdocs.FTSDocsApplication;
import ftsdocs.model.DocumentType;

@Slf4j
@Getter
@Setter
public abstract class EditableConfiguration {

    private static final Set<DocumentType> DEFAULT_DOCUMENT_TYPES = Set.of(
            new DocumentType("Portable Document Format", true, "pdf"),
            new DocumentType("Microsoft Word Document", true, "doc", "docx"),
            new DocumentType("OpenDocument", true, "odt","ods","odg","odp"),
            new DocumentType("Plain text", true, "txt"),
            new DocumentType("Rich Text Format", true, "rtf"),
            new DocumentType("HTML",true,"html","xhtml"),
            new DocumentType("Microsoft Excel",true,"xls","xlsx")

    );

    //region Appearance

    private boolean enableDarkMode;

    private Color highlightColor;

    private int contentFontSize;

    //endregion Appearance

    //region Searching

    private boolean enableSynonymSearch;

    private boolean enableSuggestions;

    private int maxSearchResults;

    private int maxPhraseHighlights;

    //endregion Searching

    //region Indexing

    private boolean enableFileWatcher;
    private LinkedHashSet<DocumentType> documentTypes;

    //endregion Indexing

    protected EditableConfiguration() {
        reset();
    }

    public void reset() {
        this.enableDarkMode = false;
        this.highlightColor = Color.rgb(0, 120, 215);
        this.contentFontSize = 14;

        this.enableSynonymSearch = false;
        this.enableSuggestions = true;
        this.maxSearchResults = 100;
        this.maxPhraseHighlights = 100;

        this.enableFileWatcher = true;
        this.documentTypes = new LinkedHashSet<>(DEFAULT_DOCUMENT_TYPES);
    }

    public void copyFrom(EditableConfiguration configuration) {
        this.enableDarkMode = configuration.isEnableDarkMode();
        this.highlightColor = configuration.getHighlightColor();
        this.contentFontSize = configuration.getContentFontSize();

        this.enableSynonymSearch = configuration.isEnableSynonymSearch();
        this.enableSuggestions = configuration.isEnableSuggestions();
        this.maxSearchResults = configuration.getMaxSearchResults();
        this.maxPhraseHighlights = configuration.getMaxPhraseHighlights();

        this.enableFileWatcher = configuration.isEnableFileWatcher();
        this.documentTypes = configuration.getDocumentTypes().stream()
                .map(doc -> new DocumentType(doc.getName(), doc.isEnabled(), doc.getExtensions()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
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
