package ftsdocs.model;

import lombok.Getter;

public enum WatcherStatus {
    DISABLED("Disabled"),
    READING_FILE_TREE("Reading file tree"),
    WATCHING("Watching");

    @Getter
    private final String displayName;

    WatcherStatus(String displayName) {
        this.displayName = displayName;
    }
}
