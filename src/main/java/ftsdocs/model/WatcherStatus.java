package ftsdocs.model;

import lombok.Getter;

@Getter
public enum WatcherStatus {
    DISABLED("Disabled", "Watcher is disabled"),
    BUILDING_WATCHER("Building watcher", "Watcher is building and reading file tree"),
    WATCHING("Watching", "Watcher is listening for changes in file system"),
    UNKNOWN("", "");

    private final String displayName;
    private final String description;

    WatcherStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
