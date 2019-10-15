package site.forgus.plugins.apigenerator.yapi.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class YApiGlobal implements Serializable {
    private static final long serialVersionUID = 6928390299424588771L;

    private String _id;
    private String name;
    private String value;

}
