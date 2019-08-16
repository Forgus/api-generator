package site.forgus.plugins.apigenerator.yapi.model;

import java.io.Serializable;

public class YApiParam implements Serializable {
    private static final long serialVersionUID = 1022289922468567639L;

    private String desc;
    private String example;
    private String name;
    private String required;

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
}
