package site.forgus.plugins.apigenerator.normal;

import com.intellij.psi.PsiType;

import java.util.List;

public class ParamModel {

    private ParamTypeEnum paramType;
    private String name;
    private PsiType psiType;
    private List<FieldInfo> fields;

    public ParamModel() {

    }

    public ParamModel(String name,PsiType psiType,ParamTypeEnum paramType, List<FieldInfo> fields) {
        this.name = name;
        this.psiType = psiType;
        this.paramType = paramType;
        this.fields = fields;
    }

    public PsiType getPsiType() {
        return psiType;
    }

    public void setPsiType(PsiType psiType) {
        this.psiType = psiType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ParamTypeEnum getParamType() {
        return paramType;
    }

    public void setParamType(ParamTypeEnum paramType) {
        this.paramType = paramType;
    }

    public List<FieldInfo> getFields() {
        return fields;
    }

    public void setFields(List<FieldInfo> fields) {
        this.fields = fields;
    }
}
