package site.forgus.plugins.apigenerator.yapi.model;

import java.io.Serializable;

public class YApiPathVariable implements Serializable {
    private static final long serialVersionUID = 1643857942192295230L;

    private String desc;
    private String example;
    private String name;

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
}
