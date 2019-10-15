package site.forgus.plugins.apigenerator.yapi.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class YApiCat implements Serializable {
    private static final long serialVersionUID = -279447231054168270L;

    private Integer index;
    private Integer _id;
    private Integer project_id;
    private Integer uid;
    private Integer __v;
    private String name;
    private String desc;
    private Date add_time;
    private Date up_time;

}
