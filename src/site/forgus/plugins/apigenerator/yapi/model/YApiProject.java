package site.forgus.plugins.apigenerator.yapi.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

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

    public Boolean getSwitch_notice() {
        return switch_notice;
    }

    public void setSwitch_notice(Boolean switch_notice) {
        this.switch_notice = switch_notice;
    }

    public Boolean getIs_mock_open() {
        return is_mock_open;
    }

    public void setIs_mock_open(Boolean is_mock_open) {
        this.is_mock_open = is_mock_open;
    }

    public Boolean getStrice() {
        return strice;
    }

    public void setStrice(Boolean strice) {
        this.strice = strice;
    }

    public Boolean getIs_json5() {
        return is_json5;
    }

    public void setIs_json5(Boolean is_json5) {
        this.is_json5 = is_json5;
    }

    public Integer get_id() {
        return _id;
    }

    public void set_id(Integer _id) {
        this._id = _id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBasepath() {
        return basepath;
    }

    public void setBasepath(String basepath) {
        this.basepath = basepath;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getProject_type() {
        return project_type;
    }

    public void setProject_type(String project_type) {
        this.project_type = project_type;
    }

    public Integer getUid() {
        return uid;
    }

    public void setUid(Integer uid) {
        this.uid = uid;
    }

    public Integer getGroup_id() {
        return group_id;
    }

    public void setGroup_id(Integer group_id) {
        this.group_id = group_id;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Date getAdd_time() {
        return add_time;
    }

    public void setAdd_time(Date add_time) {
        this.add_time = add_time;
    }

    public Date getUp_time() {
        return up_time;
    }

    public void setUp_time(Date up_time) {
        this.up_time = up_time;
    }

    public Boolean getRole() {
        return role;
    }

    public void setRole(Boolean role) {
        this.role = role;
    }

    public String getAfter_script() {
        return after_script;
    }

    public void setAfter_script(String after_script) {
        this.after_script = after_script;
    }

    public String getPre_script() {
        return pre_script;
    }

    public void setPre_script(String pre_script) {
        this.pre_script = pre_script;
    }

    public String getProject_mock_script() {
        return project_mock_script;
    }

    public void setProject_mock_script(String project_mock_script) {
        this.project_mock_script = project_mock_script;
    }

    public List<YApiEnv> getEnv() {
        return env;
    }

    public void setEnv(List<YApiEnv> env) {
        this.env = env;
    }

    public List<YApiTag> getTag() {
        return tag;
    }

    public void setTag(List<YApiTag> tag) {
        this.tag = tag;
    }
}
