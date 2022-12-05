package ftsdocs.model;

import java.time.Instant;
import java.util.Date;

import lombok.NonNull;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;

public record Document(
        @NonNull String path,
        @NonNull String content,
        long fileSize,
        String extension,
        Instant creationTime,
        Instant lastModifiedTime) {

    public static final class FieldName {

        public static final String PATH = "path";
        public static final String CONTENT = "content";
        public static final String FILE_SIZE = "fileSize";
        public static final String EXTENSION = "extension";
        public static final String CREATION_TIME = "creationTime";
        public static final String LAST_MODIFIED_TIME = "lastModifiedTime";

        private FieldName() {
        }

    }

    @NonNull
    public static Document getDocumentFromSolrDocument(@NonNull SolrDocument solrDocument) {
        String path = (String) solrDocument.get(FieldName.PATH);
        String content = (String) solrDocument.get(FieldName.CONTENT);
        long fileSize = (long) solrDocument.get(FieldName.FILE_SIZE);
        String extension = (String) solrDocument.get(FieldName.EXTENSION);
        Instant creationTime = ((Date) solrDocument.get(FieldName.CREATION_TIME)).toInstant();
        Instant lastModifiedTime = ((Date) solrDocument.get(FieldName.LAST_MODIFIED_TIME)).toInstant();
        return new Document(path, content, fileSize, extension, creationTime, lastModifiedTime);
    }

    @NonNull
    public SolrInputDocument getSolrInputDocument() {
        SolrInputDocument solrDocument = new SolrInputDocument();
        solrDocument.setField(FieldName.PATH, path);
        solrDocument.setField(FieldName.CONTENT, content);
        solrDocument.setField(FieldName.FILE_SIZE, fileSize);
        solrDocument.setField(FieldName.EXTENSION, extension);
        solrDocument.setField(FieldName.CREATION_TIME, creationTime.toString());
        solrDocument.setField(FieldName.LAST_MODIFIED_TIME, lastModifiedTime.toString());
        return solrDocument;
    }
}
