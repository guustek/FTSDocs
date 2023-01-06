package ftsdocs;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Lazy
@Getter
@Setter
public class Configuration {

    public static final String serverClassName = "ftsdocs.solr.SolrEmbeddedServer";

    private boolean darkModeEnabled;

    private Set<String> indexedLocations;

    public Configuration() {
        this.darkModeEnabled = false;
        this.indexedLocations = new HashSet<>();
    }

    public void writeToFile() {
        try {
            Files.writeString(
                    FTSDocsApplication.CONFIG_FILE.toPath(),
                    FTSDocsApplication.GSON.toJson(this));
        } catch (IOException e) {
            log.info("Encountered an error when writing configuration into config.json file");
        }
    }
}
