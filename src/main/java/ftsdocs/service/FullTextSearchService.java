package ftsdocs.service;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;

import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

import ftsdocs.model.Document;
import ftsdocs.model.IndexLocation;

public interface FullTextSearchService {

    Collection<Document> searchDocuments(String query);

    void deleteFromIndex(Collection<Path> path);

    void indexLocations(Collection<IndexLocation> locations, boolean updateWatcher,
            EventHandler<WorkerStateEvent> successHandler);

    void updateFile(IndexLocation indexLocation, File file);

}
