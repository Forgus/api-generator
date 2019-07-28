package site.forgus.plugins.apigenerator.normal;

import java.io.Serializable;

public class ParamField implements Serializable {

    private static final long serialVersionUID = -641325142048753376L;

    private String name;
    private String type;
    private String range;
    private String desc;
    private FieldTypeEnum fieldType;

    private ParamField(String name, String type, String range, String desc, FieldTypeEnum fieldType) {
        this.name = name;
        this.type = type;
        this.range = range;
        this.desc = desc;
        this.fieldType = fieldType;
    }

    public static ParamField normal(String name, String type, String range, String desc) {
        return new ParamField(name,type,range,desc,FieldTypeEnum.NORMAL);
    }

    public static ParamField parent(String name,String desc) {
        return new ParamField(name,"Object","N/A",desc,FieldTypeEnum.PARENT);
    }

    public static ParamField child(String name,String type,String range,String desc) {
        return new ParamField(name,type,range,desc,FieldTypeEnum.CHILD);
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getRange() {
        return range;
    }

    public String getDesc() {
        return desc;
    }

    public FieldTypeEnum getFieldType() {
        return fieldType;
    }
}
