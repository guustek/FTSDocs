package ftsdocs.model;

import lombok.Getter;

public enum IndexStatus {
    UPDATING("Updating"),
    READING_FILE_TREE("Reading file tree"),
    EXTRACTING_CONTENT("Extracting content"),
    INDEXING("Indexing"),
    INDEXED("Indexed");

    @Getter
    private final String displayName;

    IndexStatus(String displayName) {
        this.displayName = displayName;
    }
}
