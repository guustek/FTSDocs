package ftsdocs.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IndexLocation {

    private final String path;
    private IndexStatus indexStatus;
    private WatcherStatus watcherStatus;

    public IndexLocation(String path) {
        this.path = path;
        this.indexStatus = IndexStatus.EXTRACTING_CONTENT;
        this.watcherStatus = WatcherStatus.READING_FILE_TREE;
    }

    public IndexLocation(String path, IndexStatus indexStatus, WatcherStatus watcherStatus) {
        this.path = path;
        this.indexStatus = indexStatus;
        this.watcherStatus = watcherStatus;
    }
}
