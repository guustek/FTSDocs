package ftsdocs.model;

import lombok.Getter;

@Getter
public enum IndexStatus {
    UPDATING("Updating", "Location is being updated, you can still do search but data validity is not guaranteed"),
    READING_FILE_TREE("Reading file tree", "Reading files in this location file tree"),
    EXTRACTING_CONTENT("Extracting content", "Extracting data from found files"),
    INDEXING("Indexing", "Indexing files"),
    INDEXED("Indexed", "Files indexed, you can search"),
    FAILED("Failed", "Error occurred for this file, check logs"),
    UNKNOWN("", "");

    private final String displayName;
    private final String description;

    IndexStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

}
