package site.forgus.plugins.apigenerator.yapi.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class YApiTag implements Serializable {
    private static final long serialVersionUID = -9151534644001353048L;

    private String _id;
    private String name;
    private String desc;

}
