package ftsdocs.model.configuration;

import java.io.IOException;

import javafx.scene.paint.Color;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class ColorAdapter extends TypeAdapter<Color> {

    @Override
    public void write(JsonWriter out, Color value) throws IOException {
        out.value(value.toString());
    }

    @Override
    public Color read(JsonReader in) throws IOException {
        return Color.valueOf(in.nextString());
    }
}