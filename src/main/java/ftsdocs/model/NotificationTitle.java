package ftsdocs.model;

import lombok.Getter;

public enum NotificationTitle {

    INFORMATION("Information"),
    ERROR("Error");

    @Getter
    private final String title;

    NotificationTitle(String title) {
        this.title = title;
    }

}
