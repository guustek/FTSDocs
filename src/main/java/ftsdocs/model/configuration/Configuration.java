package ftsdocs.model.configuration;

import com.google.gson.annotations.JsonAdapter;
import ftsdocs.model.IndexLocation;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@Lazy
@Getter
@Setter
public class Configuration extends EditableConfiguration {

    public static final String SERVER_CLASS = "ftsdocs.server.SolrEmbeddedServer";

    private double width;

    private double height;

    @JsonAdapter(IndexedLocationsAdapter.class)
    private ConcurrentHashMap<String, IndexLocation> indexedLocations;

    public Configuration() {
        reset();
        this.indexedLocations = new ConcurrentHashMap<>();
    }

    public Configuration(Configuration configuration) {
        this.copyFrom(configuration);
        this.width = configuration.getWidth();
        this.height = configuration.getHeight();
    }

    @Override
    public void reset() {
        super.reset();
        this.width = 900;
        this.height = 700;
    }

    public void copyFrom(Configuration configuration) {
        super.copyFrom(configuration);
        this.indexedLocations = new ConcurrentHashMap<>(configuration.getIndexedLocations());
    }
}
