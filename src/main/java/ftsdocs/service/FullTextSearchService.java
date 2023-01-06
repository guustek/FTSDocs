package ftsdocs.service;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;

import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

import ftsdocs.model.Document;

public interface FullTextSearchService {

    Collection<Document> searchDocuments(String query);

    void deleteFromIndex(Path path);

    void indexFiles(Collection<File> files, EventHandler<WorkerStateEvent> successHandler, boolean updateWatcher);
}
