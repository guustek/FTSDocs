package ftsdocs.model;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.solr.client.solrj.beans.Field;

@NoArgsConstructor
@AllArgsConstructor
@Data
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

}
