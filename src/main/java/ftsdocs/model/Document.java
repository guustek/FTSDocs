package ftsdocs.model;

import lombok.NonNull;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;

public record Document(
        @NonNull String path,
        @NonNull String content,
        long fileSize,
        String extension,
        String creationTime,
        String lastModifiedTime) {

    @NonNull
    public static Document getDocumentFromSolrDocument(@NonNull SolrDocument solrDocument) {
        String path = (String) solrDocument.get(FieldName.PATH);
        String content = (String) solrDocument.get(FieldName.CONTENT);
        long fileSize = Long.parseLong((String) solrDocument.get(FieldName.FILE_SIZE));
        String extension = (String) solrDocument.get(FieldName.EXTENSION);
        String creationTime = (String) solrDocument.get(FieldName.CREATION_TIME);
        String lastModifiedTime = (String) solrDocument.get(FieldName.LAST_MODIFIED_TIME);
        return new Document(path, content, fileSize, extension, creationTime, lastModifiedTime);
    }

    @NonNull
    public  SolrInputDocument getSolrInputDocument() {
        SolrInputDocument solrDocument = new SolrInputDocument();
        solrDocument.setField(FieldName.PATH, path);
        solrDocument.setField(FieldName.CONTENT, content);
        solrDocument.setField(FieldName.FILE_SIZE, fileSize);
        solrDocument.setField(FieldName.EXTENSION, extension);
        solrDocument.setField(FieldName.CREATION_TIME, creationTime);
        solrDocument.setField(FieldName.LAST_MODIFIED_TIME, lastModifiedTime);
        return solrDocument;
    }


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
}
