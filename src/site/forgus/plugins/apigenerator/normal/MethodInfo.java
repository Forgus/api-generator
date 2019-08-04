package site.forgus.plugins.apigenerator.normal;

import java.io.Serializable;
import java.util.List;

public class MethodInfo implements Serializable {
    private static final long serialVersionUID = -9143203778013000538L;

    private String desc;
    private String packageName;
    private String className;
    private String returnStr;
    private String paramStr;
    private String methodName;
    private List<FieldInfo> requestFields;
    private List<FieldInfo> responseFields;

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getReturnStr() {
        return returnStr;
    }

    public void setReturnStr(String returnStr) {
        this.returnStr = returnStr;
    }

    public String getParamStr() {
        return paramStr;
    }

    public void setParamStr(String paramStr) {
        this.paramStr = paramStr;
    }

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
