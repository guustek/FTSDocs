package ftsdocs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GsonUtils {

    private GsonUtils() {
    }

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    public static String toJson(Object o) {
        return gson.toJson(o);
    }

    public static String toUnescapedWhiteSpacesJson(Object o) {
        return gson.toJson(o)
                .replace("\\n", "\n").replace("\\\n", "\n")
                .replace("\\r", "\r").replace("\\\r", "\r")
                .replace("\\t", "\r").replace("\\\t", "\r")
                .replace("\\\"","\"");
    }
}
