package site.forgus.plugins.apigenerator.yapi.model;

import java.io.Serializable;
import java.util.List;

public class YApiEnv implements Serializable {

    private static final long serialVersionUID = -2972535876457197291L;

    private String _id;
    private String name;
    private String domain;
    private List<YApiHeader> header;
    private List<YApiGlobal> global;

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

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public List<YApiHeader> getHeader() {
        return header;
    }

    public void setHeader(List<YApiHeader> header) {
        this.header = header;
    }

    public List<YApiGlobal> getGlobal() {
        return global;
    }

    public void setGlobal(List<YApiGlobal> global) {
        this.global = global;
    }
}
