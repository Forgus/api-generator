package site.forgus.plugins.apigenerator.normal;

import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;

public class FieldInfo {

    private String name;
    private Object value;
    private PsiType psiType;
    private boolean require;
    private String range;
    private String desc;
    private boolean hasChildren;
    private ParamTypeEnum paramType;
    private List<FieldInfo> children;

    private FieldInfo(String name, PsiType psiType, boolean require, String range, String desc) {
        this.name = name == null ? "N/A" : name;
        this.psiType = psiType;
        this.require = require;
        this.range = range == null ? "N/A" : range;
        this.desc = desc == null ? "N/A" : desc;
        String presentableText = psiType.getPresentableText();
        if(NormalTypes.isNormalType(presentableText)) {
            paramType = ParamTypeEnum.LITERAL;
            value = NormalTypes.normalTypes.get(presentableText);
//            value = PsiTypesUtil.getDefaultValueOfType(psiType,true);
        }else if(presentableText.contains("<") && (presentableText.startsWith("List") || presentableText.startsWith("Set"))) {
            paramType = ParamTypeEnum.ARRAY;
        }else {
            paramType = ParamTypeEnum.OBJECT;
        }
    }

    public static FieldInfo normal(String name,PsiType type,RequireAndRange requireAndRange,String desc,List<FieldInfo> children) {
        FieldInfo fieldInfo = new FieldInfo(name, type, requireAndRange.isRequire(), requireAndRange.getRange(), desc);
        fieldInfo.setChildren(children);
        fieldInfo.setHasChildren(CollectionUtils.isNotEmpty(children));
        return fieldInfo;
    }

    public static FieldInfo child(String name, PsiType type, RequireAndRange requireAndRange, String desc) {
        return new FieldInfo(name,type,requireAndRange.isRequire(),requireAndRange.getRange(),desc);
    }

    public static FieldInfo parent(String name, PsiType type, RequireAndRange requireAndRange, String desc,List<FieldInfo> children) {
        FieldInfo fieldInfo = new FieldInfo(name, type, requireAndRange.isRequire(),requireAndRange.getRange(), desc);
        fieldInfo.setHasChildren(true);
        fieldInfo.setChildren(children);
        return fieldInfo;
    }

    public ParamTypeEnum getParamType() {
        return paramType;
    }

    public void setParamType(ParamTypeEnum paramType) {
        this.paramType = paramType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public PsiType getPsiType() {
        return psiType;
    }

    public void setPsiType(PsiType psiType) {
        this.psiType = psiType;
    }

    public boolean isRequire() {
        return require;
    }

    public void setRequire(boolean require) {
        this.require = require;
    }

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public boolean isHasChildren() {
        return hasChildren;
    }

    public void setHasChildren(boolean hasChildren) {
        this.hasChildren = hasChildren;
    }

    public List<FieldInfo> getChildren() {
        return children;
    }

    public void setChildren(List<FieldInfo> children) {
        this.children = children;
    }
}
