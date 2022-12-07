package ftsdocs;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class DisplayUtils {

    private DisplayUtils() {
    }

    public static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(
                    FormatStyle.MEDIUM)
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault());

    public static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .registerTypeAdapter(Instant.class,
                    (JsonSerializer<Instant>) (Instant date, Type typeOfSrc, JsonSerializationContext context) ->
                            new JsonPrimitive(date.toString()))
            .registerTypeAdapter(Instant.class,
                    (JsonDeserializer<Instant>) (JsonElement json, Type typeOfSrc, JsonDeserializationContext context) ->
                            Instant.parse(json.getAsString()))
            .create();

    public static String toJson(Object o) {
        return gson.toJson(o);
    }

    public static String toUnescapedWhiteSpacesJson(Object o) {
        return gson.toJson(o)
                .replace("\\n", "\n").replace("\\\n", "\n")
                .replace("\\r", "\r").replace("\\\r", "\r")
                .replace("\\t", "\r").replace("\\\t", "\r")
                .replace("\\\"", "\"");
    }
}
