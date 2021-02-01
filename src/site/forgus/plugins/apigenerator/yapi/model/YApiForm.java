package site.forgus.plugins.apigenerator.yapi.model;

public class YApiForm extends YApiParam {
    private static final long serialVersionUID = 259883183902353577L;

    private String type = "text";

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
