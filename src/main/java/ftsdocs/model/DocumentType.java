package ftsdocs.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FilenameUtils;

@Getter
@Setter
@Data
public class DocumentType implements Checkable {

    private String name;

    private boolean enabled;

    private LinkedHashSet<String> extensions;

    public DocumentType(String name, boolean enabled, String... extensions) {
        this(name, enabled, Arrays.stream(extensions)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    public DocumentType(String name, boolean enabled, Collection<String> extensions) {
        this.name = name;
        this.enabled = enabled;
        this.extensions = extensions.stream()
                .map(ext -> {
                    String actualExtension = FilenameUtils.getExtension("." + ext.toLowerCase());
                    return "*." + actualExtension;
                })
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public String toString() {
        return this.name + " (" + String.join(";", this.extensions) + ")";
    }

    @Override
    public boolean isChecked() {
        return enabled;
    }
}
