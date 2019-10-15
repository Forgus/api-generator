package site.forgus.plugins.apigenerator.yapi.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class YApiPathVariable implements Serializable {
    private static final long serialVersionUID = 1643857942192295230L;

    private String desc;
    private String example;
    private String name;

}
