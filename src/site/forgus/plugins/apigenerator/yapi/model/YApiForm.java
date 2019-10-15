package site.forgus.plugins.apigenerator.yapi.model;

import lombok.Data;

@Data
public class YApiForm extends YApiParam {
    private static final long serialVersionUID = 259883183902353577L;

    private String type = "text";

}
