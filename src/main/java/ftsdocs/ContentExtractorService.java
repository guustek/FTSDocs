package ftsdocs;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;

import ftsdocs.model.Document;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ContentExtractorService {

    private final Tika tika;

    public ContentExtractorService() {
        this.tika = new Tika();
    }

    private String getContent(File file) throws Exception {
        try {
            log.debug("Detected type for file {} : {}", file.getName(), tika.detect(file.getName()));
            return tika.parseToString(file);
        } catch (Exception e) {
            log.error("Error while extracting content from {}", file, e);
            throw new Exception(e);
        }
    }

    public Document getDocumentFromFile(File file) {
        if (!file.exists() || !file.isFile() || file.isDirectory()) {
            log.error("File does not exist or is invalid: {}", file.getAbsolutePath());
            return null;
        }
        try {
            BasicFileAttributes fileAttributes = Files.readAttributes(file.toPath(),
                    BasicFileAttributes.class);
            String path = file.getAbsolutePath();
            String content = getContent(file);
            long fileSize = fileAttributes.size();
            String extension = FilenameUtils.getExtension(file.getName());
            String creationTime = fileAttributes.creationTime().toString();
            String lastModifiedTime = fileAttributes.lastModifiedTime().toString();
            return new Document(path, content, fileSize, extension, creationTime, lastModifiedTime);
        } catch (Exception e) {
            log.error("Error while building document: {}", file, e);
            return null;
        }
    }
}
