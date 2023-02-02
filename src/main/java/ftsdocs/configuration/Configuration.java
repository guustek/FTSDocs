package ftsdocs.configuration;

import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.annotations.JsonAdapter;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ftsdocs.model.IndexLocation;

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
    }

    public Configuration(Configuration configuration) {
        this.copyFrom(configuration);
    }

    @Override
    public void reset() {
        super.reset();
        this.width = 900;
        this.height = 700;
        this.indexedLocations = new ConcurrentHashMap<>();
    }

    public void copyFrom(Configuration configuration) {
        super.copyFrom(configuration);
        this.width = configuration.getWidth();
        this.height = configuration.getHeight();
        this.indexedLocations = new ConcurrentHashMap<>(configuration.getIndexedLocations());
    }
}
