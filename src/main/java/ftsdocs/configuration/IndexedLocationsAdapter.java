package ftsdocs.configuration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import ftsdocs.model.IndexLocation;

public class IndexedLocationsAdapter extends TypeAdapter<Map<String, IndexLocation>> {

    @Override
    public void write(JsonWriter out, Map<String, IndexLocation> value) throws IOException {
        out.beginArray();
        for (String key : value.keySet()) {
            out.value(key);
        }
        out.endArray();
    }

    @Override
    public Map<String, IndexLocation> read(JsonReader in) throws IOException {
        Map<String, IndexLocation> map = new LinkedHashMap<>();
        in.beginArray();
        while (in.hasNext()) {
            String key = in.nextString();
            map.put(key, new IndexLocation(new File(key)));
        }
        in.endArray();
        return map;
    }
}