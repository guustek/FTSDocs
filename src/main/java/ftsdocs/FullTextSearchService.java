package ftsdocs;

import java.io.File;
import java.util.Collection;

import ftsdocs.model.Document;

public interface FullTextSearchService {

    Collection<Document> searchDocuments(String query);

    void indexFiles(Collection<File> files);
}
