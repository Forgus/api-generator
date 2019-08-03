package site.forgus.plugins.apigenerator.normal;

import java.io.Serializable;
import java.util.List;

public class MethodInfo implements Serializable {
    private static final long serialVersionUID = -9143203778013000538L;

    private String desc;
    private List<FieldInfo> requestFields;
    private List<FieldInfo> responseFields;

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public List<FieldInfo> getRequestFields() {
        return requestFields;
    }

    public void setRequestFields(List<FieldInfo> requestFields) {
        this.requestFields = requestFields;
    }

    public List<FieldInfo> getResponseFields() {
        return responseFields;
    }

    public void setResponseFields(List<FieldInfo> responseFields) {
        this.responseFields = responseFields;
    }
}
