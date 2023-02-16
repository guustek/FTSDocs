package ftsdocs.model.configuration;

import lombok.Getter;

public enum Category {

    APPEARANCE("Appearance"),
    SEARCHING("Searching"),
    INDEXING("Indexing");

    @Getter
    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }
}
