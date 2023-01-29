package ftsdocs.model;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@SuppressWarnings("java:S2065")
public class IndexLocation {


    private final File root;
    private transient Collection<File> indexedFiles;
    private transient IndexStatus indexStatus;
    private transient WatcherStatus watcherStatus;

    public IndexLocation(File file) {
        this.root = file;
        this.indexedFiles = new LinkedList<>();
        this.indexStatus = IndexStatus.UNKNOWN;
        this.watcherStatus = WatcherStatus.UNKNOWN;
    }

    public IndexLocation(File file, IndexStatus indexStatus, WatcherStatus watcherStatus) {
        this.root = file;
        this.indexedFiles = new LinkedList<>();
        this.indexStatus = indexStatus;
        this.watcherStatus = watcherStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        IndexLocation that = (IndexLocation) o;

        return root.equals(that.root);
    }

    @Override
    public int hashCode() {
        return root.hashCode();
    }
}
