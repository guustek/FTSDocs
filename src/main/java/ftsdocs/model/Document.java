package ftsdocs.model;

import lombok.NonNull;
import org.apache.solr.common.SolrDocument;

public record Document(
        @NonNull String path,
        @NonNull String content,
        long fileSize,
        String extension,
        String creationTime,
        String modificationTime) {

    public static Document fromSolrDocument(@NonNull SolrDocument solrDocument) {
        String path = (String) solrDocument.get(FieldName.PATH);
        String content = (String) solrDocument.get(FieldName.CONTENT);
        long fileSize = Long.parseLong((String) solrDocument.get(FieldName.FILE_SIZE));
        String extension = (String) solrDocument.get(FieldName.EXTENSION);
        String creationTime = (String) solrDocument.get(FieldName.CREATION_TIME);
        String lastModificationTime = (String) solrDocument.get(FieldName.LAST_MODIFICATION_TIME);
        return new Document(path, content, fileSize, extension, creationTime, lastModificationTime);
    }

//    public static Document fromFile(File file){
//
//    }

    public SolrDocument toSolrDocument() {
        SolrDocument solrDocument = new SolrDocument();
        solrDocument.setField(FieldName.PATH, path);
        solrDocument.setField(FieldName.CONTENT, content);
        solrDocument.setField(FieldName.FILE_SIZE, fileSize);
        solrDocument.setField(FieldName.EXTENSION, extension);
        solrDocument.setField(FieldName.CREATION_TIME, creationTime);
        return solrDocument;
    }

    public static final class FieldName {

        public static final String PATH = "path";
        public static final String CONTENT = "content";
        public static final String FILE_SIZE = "fileSize";
        public static final String EXTENSION = "extension";
        public static final String CREATION_TIME = "creationTime";
        public static final String LAST_MODIFICATION_TIME = "lastModificationTime";

        private FieldName() {
        }

    }
}
