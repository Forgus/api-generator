package site.forgus.plugins.apigenerator.yapi.model;

import java.io.Serializable;

public class YApiTag implements Serializable {
    private static final long serialVersionUID = -9151534644001353048L;

    private String _id;
    private String name;
    private String desc;

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
