package ftsdocs.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class DocumentType implements Checkable {

    private String name;

    private Set<String> extensions;

    private boolean enabled;

    public DocumentType(String name, String... extensions) {
        this(name, Arrays.stream(extensions).collect(Collectors.toSet()));
    }

    public DocumentType(String name, Collection<String> extensions) {
        this.name = name;
        this.enabled = true;
        this.extensions = extensions.stream()
                .map(ext -> {
                    String actualExtension = ext.toLowerCase();
                    if (!ext.startsWith(".")) {
                        actualExtension = "." + actualExtension;
                    }
                    return actualExtension;
                })
                .collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    public String toString() {
        return this.name + " (" + String.join(" | ", this.extensions) + ")";
    }

    @Override
    public boolean isChecked() {
        return enabled;
    }
}
