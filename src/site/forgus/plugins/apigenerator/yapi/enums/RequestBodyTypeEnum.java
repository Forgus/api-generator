package site.forgus.plugins.apigenerator.yapi.enums;

public enum RequestBodyTypeEnum {

    FORM("form"),
    JSON("json"),
    RAW("raw");

    private String value;

    RequestBodyTypeEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
