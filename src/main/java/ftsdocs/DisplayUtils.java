package ftsdocs;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import javafx.scene.Parent;

import org.controlsfx.control.Notifications;

public class DisplayUtils {

    private DisplayUtils() {
    }

    public static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(
                    FormatStyle.MEDIUM)
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault());

    public static String toUnescapedWhiteSpacesJson(Object o) {
        return FTSDocsApplication.GSON.toJson(o)
                .replace("\\n", "\n").replace("\\\n", "\n")
                .replace("\\r", "\r").replace("\\\r", "\r")
                .replace("\\t", "\r").replace("\\\t", "\r")
                .replace("\\\"", "\"");
    }

    public static void showNotification(Parent parent, String title, String text) {
        Notifications.create()
                .owner(parent)
                .title(title)
                .text(text)
                .show();
    }
}
