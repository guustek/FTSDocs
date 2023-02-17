package ftsdocs.service;

import ftsdocs.model.Document;
import ftsdocs.model.IndexLocation;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

import java.nio.file.Path;
import java.util.Collection;

public interface FullTextSearchService {

    Collection<Document> searchDocuments(String query);

    Collection<String> getSuggestions(String searchPhrase);

    void deleteFromIndex(Collection<Path> path);

    void indexLocations(Collection<IndexLocation> locations, EventHandler<WorkerStateEvent> successHandler);

    void updateFileWatcher();

}
