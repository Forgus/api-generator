package site.forgus.plugins.apigenerator.normal;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
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
    private List<PsiAnnotation> annotations;

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
        }else if(presentableText.contains("<") && (presentableText.startsWith("List") || presentableText.startsWith("Set"))) {
            paramType = ParamTypeEnum.ARRAY;
        }else {
            paramType = ParamTypeEnum.OBJECT;
        }
    }

    public static FieldInfo normal(String name,PsiType type,String desc,List<FieldInfo> children,PsiAnnotation[] annotations) {
        RequireAndRange requireAndRange = getRequireAndRange(annotations);
        FieldInfo fieldInfo = new FieldInfo(name, type, requireAndRange.isRequire(), requireAndRange.getRange(), desc);
        fieldInfo.setChildren(children);
        fieldInfo.setHasChildren(CollectionUtils.isNotEmpty(children));
        fieldInfo.setAnnotations(Arrays.asList(annotations));
        return fieldInfo;
    }

    public static FieldInfo child(String name, PsiType type, String desc,PsiAnnotation[] annotations) {
        RequireAndRange requireAndRange = getRequireAndRange(annotations);
        return new FieldInfo(name,type,requireAndRange.isRequire(),requireAndRange.getRange(),desc);
    }

    public static FieldInfo parent(String name, PsiType type, String desc,List<FieldInfo> children,PsiAnnotation[] annotations) {
        RequireAndRange requireAndRange = getRequireAndRange(annotations);
        FieldInfo fieldInfo = new FieldInfo(name, type, requireAndRange.isRequire(),requireAndRange.getRange(), desc);
        fieldInfo.setHasChildren(true);
        fieldInfo.setChildren(children);
        return fieldInfo;
    }

    private static RequireAndRange getRequireAndRange(PsiAnnotation[] annotations) {
        if(annotations.length == 0) {
            return RequireAndRange.instance();
        }
        boolean require = false;
        String min = "";
        String max = "";
        String range = "N/A";
        for (PsiAnnotation annotation : annotations) {
            String qualifiedName = annotation.getText();
            if (qualifiedName.contains("NotNull") || qualifiedName.contains("NotBlank") || qualifiedName.contains("NotEmpty")) {
                require = true;
            }
            if (qualifiedName.contains("Length") || qualifiedName.contains("Range")) {
                PsiAnnotationMemberValue maxValue = annotation.findAttributeValue("max");
                PsiAnnotationMemberValue minValue = annotation.findAttributeValue("min");
                if (maxValue != null) {
                    max = maxValue.getText();
                }
                if (minValue != null) {
                    min = minValue.getText();
                }
            }
            if(qualifiedName.contains("Min")) {
                PsiAnnotationMemberValue minValue = annotation.findAttributeValue("value");
                if(minValue != null) {
                    min = minValue.getText();
                }
            }
            if(qualifiedName.contains("Max")) {
                PsiAnnotationMemberValue maxValue = annotation.findAttributeValue("value");
                if(maxValue != null) {
                    max = maxValue.getText();
                }
            }
        }
        if (StringUtils.isNotEmpty(min) || StringUtils.isNotEmpty(max)) {
            range = "[" + min + "," + max + "]";
        }
        return new RequireAndRange(require, range);
    }

    public List<PsiAnnotation> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<PsiAnnotation> annotations) {
        this.annotations = annotations;
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
