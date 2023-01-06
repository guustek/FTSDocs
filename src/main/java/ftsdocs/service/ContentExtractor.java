package ftsdocs.service;

import java.io.File;

import ftsdocs.model.Document;

public interface ContentExtractor {

    Document getDocumentFromFile(File file);
}
