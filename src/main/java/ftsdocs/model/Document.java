package ftsdocs.model;

import java.util.Date;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.solr.client.solrj.beans.Field;

@NoArgsConstructor
@Getter
public class Document {

    @Field
    private String path;
    @Field
    private String content;
    @Field
    private long fileSize;
    @Field
    private String extension;
    @Field
    private Date creationTime;
    @Field
    private Date lastModifiedTime;
//    @Field
//    private double score;
    @Setter
    private String highlight;

    public Document(String path, String content, long fileSize, String extension, Date creationTime,
            Date lastModifiedTime) {
        this.path = path;
        this.content = content;
        this.fileSize = fileSize;
        this.extension = extension;
        this.creationTime = creationTime;
        this.lastModifiedTime = lastModifiedTime;
    }
}
