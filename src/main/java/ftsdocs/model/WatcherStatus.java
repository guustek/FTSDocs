package ftsdocs.model;

import lombok.Getter;

public enum WatcherStatus {
    DISABLED("Disabled"),
    BUILDING_WATCHER("Building watcher"),
    WATCHING("Watching"),
    UNKNOWN("");

    @Getter
    private final String displayName;

    WatcherStatus(String displayName) {
        this.displayName = displayName;
    }
}
