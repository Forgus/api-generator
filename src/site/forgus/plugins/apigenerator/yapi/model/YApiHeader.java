package site.forgus.plugins.apigenerator.yapi.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class YApiHeader implements Serializable {
    private static final long serialVersionUID = -6583156150132193662L;

    private String name;
    private String value;

    private YApiHeader(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public static YApiHeader json() {
        return new YApiHeader("Content-Type", "application/json");
    }

    public static YApiHeader form() {
        return new YApiHeader("Content-Type", "application/x-www-form-urlencoded");
    }

}
