package site.forgus.plugins.apigenerator.yapi.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class YApiProject implements Serializable {

    private static final long serialVersionUID = -4425604264358577061L;

    private Boolean switch_notice;
    private Boolean is_mock_open;
    private Boolean strice;
    private Boolean is_json5;
    private Integer _id;
    private String name;
    private String basepath;
    private String desc;
    private String project_type;
    private Integer uid;
    private Integer group_id;
    private String icon;
    private String color;
    private Date add_time;
    private Date up_time;
    private Boolean role;
    private String after_script;
    private String pre_script;
    private String project_mock_script;
    private List<YApiEnv> env;
    private List<YApiTag> tag;

}
