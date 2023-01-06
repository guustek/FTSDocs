package ftsdocs.service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import ftsdocs.model.Document;

@Service
@Slf4j
public class TikaContentExtractor implements ContentExtractor{

    private final Tika tika;

    public TikaContentExtractor() {
        this.tika = new Tika();
    }

    public Document getDocumentFromFile(File file) {
        if (!file.exists() || !file.isFile() || file.isDirectory()) {
            log.error("File does not exist or is invalid: {}", file);
            return null;
        }
        try {
            long start = System.currentTimeMillis();
            final BasicFileAttributes fileAttributes = Files.readAttributes(file.toPath(),
                    BasicFileAttributes.class);
            String path = file.getAbsolutePath();
            String content = getContent(file);
            long fileSize = fileAttributes.size();
            String extension = FilenameUtils.getExtension(file.getName());
            var creationTime = Date.from(fileAttributes.creationTime().toInstant());
            var lastModifiedTime = Date.from(fileAttributes.lastModifiedTime().toInstant());
            long time = System.currentTimeMillis() - start;
            log.info("Extracted data from {} in {} seconds", file, (double) time / 1000);
            return new Document(path, content, fileSize, extension, creationTime, lastModifiedTime);
        } catch (Exception e) {
            log.error("Error while building document: {}", file, e);
            return null;
        }
    }

    private String getContent(File file) throws Exception {
        try {
            log.info("Detected type {} for file {}", file, tika.detect(file.getName()));
            if (file.length() == 0) {
                log.info("Zero byte file - {}, returning empty content", file);
                return "";
            }
            return tika.parseToString(file);
        } catch (Exception e) {
            log.error("Error while extracting content from {}", file, e);
            throw new Exception(e);
        }
    }
}
