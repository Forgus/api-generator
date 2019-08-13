package site.forgus.plugins.apigenerator.yapi.model;

import java.io.Serializable;

public class YApiForm implements Serializable {
    private static final long serialVersionUID = 259883183902353577L;

    private String desc;
    private String example;
    private String name;
    private String required;
    private String type;

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRequired() {
        return required;
    }

    public void setRequired(String required) {
        this.required = required;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
