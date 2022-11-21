package ftsdocs;

import lombok.NonNull;
import org.apache.solr.common.SolrDocument;

public record Document(
        @NonNull String path,
        @NonNull String content,
        long fileSize,
        String extension,
        String creationTime) {

    public static Document fromSolrDocument(@NonNull SolrDocument solrDocument) {
        String path = (String) solrDocument.get("path");
        String content = (String) solrDocument.get("content");
        long fileSize = Long.parseLong((String) solrDocument.get("fileSize"));
        String extension = (String) solrDocument.get("extension");
        String creationTime = (String) solrDocument.get("creationTime");
        return new Document(path, content, fileSize, extension, creationTime);
    }
}
