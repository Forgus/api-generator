package site.forgus.plugins.apigenerator.normal;

import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.util.PsiUtil;
import org.apache.commons.lang.StringUtils;
import site.forgus.plugins.apigenerator.config.PersistentConfig;
import site.forgus.plugins.apigenerator.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FieldInfo {

    private String name;
    private Object value;
    private PsiType psiType;
    private boolean require;
    private String range;
    private String desc;
    private ParamTypeEnum paramType;
    private List<FieldInfo> children;
    private List<PsiAnnotation> annotations;

    private static List<String> requiredTexts = Arrays.asList("NotNull","NotBlank","NotEmpty","PathVariable");

    protected PersistentConfig config = PersistentConfig.getInstance();

    private FieldInfo(String name, PsiType psiType, boolean require, String range, String desc) {
        this.name = name == null ? "N/A" : name;
        this.psiType = psiType;
        this.require = require;
        this.range = range == null ? "N/A" : range;
        this.desc = desc == null ? "" : desc;
        if (psiType != null) {
            String presentableText = psiType.getPresentableText();
            if (NormalTypes.isNormalType(presentableText)) {
                paramType = ParamTypeEnum.LITERAL;
                value = NormalTypes.normalTypes.get(presentableText);
            } else if (presentableText.contains("<") && (presentableText.startsWith("List") || presentableText.startsWith("Set"))) {
                paramType = ParamTypeEnum.ARRAY;
            } else {
                paramType = ParamTypeEnum.OBJECT;
            }
            this.children = listFieldInfos(psiType);
        } else {
            paramType = ParamTypeEnum.OBJECT;
        }
    }

    public FieldInfo(String name, PsiType psiType, String desc, PsiAnnotation[] annotations) {
        RequireAndRange requireAndRange = getRequireAndRange(annotations);
        String fieldName = getParamName(name, annotations);
        this.name = fieldName == null ? "N/A" : fieldName;
        this.psiType = psiType;
        this.require = requireAndRange.isRequire();
        this.range = requireAndRange.getRange() == null ? "N/A" : requireAndRange.getRange();
        this.desc = desc == null ? "" : desc;
        this.annotations = Arrays.asList(annotations);
        if (psiType != null) {
            String presentableText = psiType.getPresentableText();
            if (NormalTypes.isNormalType(presentableText)) {
                paramType = ParamTypeEnum.LITERAL;
                value = NormalTypes.normalTypes.get(presentableText);
            } else if (presentableText.contains("<") && (presentableText.startsWith("List") || presentableText.startsWith("Set"))) {
                paramType = ParamTypeEnum.ARRAY;
            } else {
                paramType = ParamTypeEnum.OBJECT;
            }
            this.children = listFieldInfos(psiType);
        } else {
            paramType = ParamTypeEnum.OBJECT;
        }
    }

    public FieldInfo(PsiType psiType, String desc, PsiAnnotation[] annotations) {
        this(psiType.getPresentableText(), psiType, desc, annotations);
    }

    private static FieldInfo child(String name, PsiType type, String desc, PsiAnnotation[] annotations) {
        RequireAndRange requireAndRange = getRequireAndRange(annotations);
        return new FieldInfo(name, type, requireAndRange.isRequire(), requireAndRange.getRange(), desc);
    }

    private static String getParamName(String name, PsiAnnotation[] annotations) {
        PsiAnnotation requestParamAnnotation = getRequestParamAnnotation(annotations);
        if (requestParamAnnotation == null) {
            return name;
        }
        PsiNameValuePair[] attributes = requestParamAnnotation.getParameterList().getAttributes();
        if (attributes.length == 1 && attributes[0].getName() == null) {
            return attributes[0].getLiteralValue();
        }
        for (PsiNameValuePair psiNameValuePair : attributes) {
            String pairName = psiNameValuePair.getName();
            if ("value".equals(pairName) || "name".equals(pairName)) {
                return psiNameValuePair.getLiteralValue();
            }
        }
        return name;
    }

    private static PsiAnnotation getRequestParamAnnotation(PsiAnnotation[] annotations) {
        for (PsiAnnotation annotation : annotations) {
            if (annotation.getText().contains("RequestParam")) {
                return annotation;
            }
        }
        return null;
    }

    private List<FieldInfo> listFieldInfos(PsiType psiType) {
        if (psiType == null) {
            return new ArrayList<>();
        }
        if (NormalTypes.isNormalType(psiType.getPresentableText())) {
            //基础类或基础包装类没有子域
            return new ArrayList<>();
        }
        List<FieldInfo> fieldInfos = new ArrayList<>();
        if (psiType instanceof PsiClassReferenceType) {
            String typeName = psiType.getPresentableText();
            if (typeName.startsWith("List") || typeName.startsWith("Set")) {
                PsiType iterableType = PsiUtil.extractIterableTypeParameter(psiType, false);
                if (iterableType == null || NormalTypes.isNormalType(iterableType.getPresentableText())) {
                    return new ArrayList<>();
                }
                return listFieldInfos(iterableType);
            }
            if (typeName.startsWith("Map")) {
                fieldInfos.add(FieldInfo.child(typeName, null, "", new PsiAnnotation[0]));
                return fieldInfos;
            }
            if (typeName.contains("<")) {
                PsiClass outerClass = PsiUtil.resolveClassInType(psiType);
                PsiType innerType = PsiUtil.substituteTypeParameter(psiType, outerClass, 0, false);
                for (PsiField outField : outerClass.getAllFields()) {
                    PsiType type = containGeneric(outField.getType().getPresentableText()) ? innerType : outField.getType();
                    if (config.getState().excludeFields.contains(outField.getName())) {
                        continue;
                    }
                    fieldInfos.add(new FieldInfo(outField.getName(), type, DesUtil.getDescription(outField.getDocComment()), outField.getAnnotations()));
                }
                return fieldInfos;
            }
            PsiClass psiClass = PsiUtil.resolveClassInType(psiType);
            if (psiClass == null) {
                return new ArrayList<>();
            }
            for (PsiField psiField : psiClass.getAllFields()) {
                if (config.getState().excludeFields.contains(psiField.getName())) {
                    continue;
                }
                PsiType type = psiField.getType().getPresentableText().equals(psiType.getPresentableText()) ? null : psiField.getType();
                fieldInfos.add(new FieldInfo(psiField.getName(), type, DesUtil.getDescription(psiField.getDocComment()), psiField.getAnnotations()));
            }
            return fieldInfos;
        }
        return new ArrayList<>();
    }

    private static boolean containGeneric(String str) {
        for (String generic : NormalTypes.genericList) {
            if (str.contains(generic)) {
                return true;
            }
        }
        return false;
    }

    private static RequireAndRange getRequireAndRange(PsiAnnotation[] annotations) {
        if (annotations.length == 0) {
            return RequireAndRange.instance();
        }
        boolean require = false;
        String min = "";
        String max = "";
        String range = "N/A";
        for (PsiAnnotation annotation : annotations) {
            String qualifiedName = annotation.getText();
            require = isParamRequired(annotation);
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
            if (qualifiedName.contains("Min")) {
                PsiAnnotationMemberValue minValue = annotation.findAttributeValue("value");
                if (minValue != null) {
                    min = minValue.getText();
                }
            }
            if (qualifiedName.contains("Max")) {
                PsiAnnotationMemberValue maxValue = annotation.findAttributeValue("value");
                if (maxValue != null) {
                    max = maxValue.getText();
                }
            }
        }
        if (StringUtils.isNotEmpty(min) || StringUtils.isNotEmpty(max)) {
            range = "[" + min + "," + max + "]";
        }
        return new RequireAndRange(require, range);
    }

    private static boolean isParamRequired(PsiAnnotation annotation) {
        String annotationText = annotation.getText();
        if (annotationText.contains("RequestParam")) {
            PsiNameValuePair[] psiNameValuePairs = annotation.getParameterList().getAttributes();
            for (PsiNameValuePair psiNameValuePair : psiNameValuePairs) {
                if ("required".equals(psiNameValuePair.getName()) && "false".equals(psiNameValuePair.getLiteralValue())) {
                    return false;
                }
            }
            return true;
        }
        return requiredTexts.contains(annotationText);
    }

    public boolean isHasChildren() {
        return CollectionUtils.isNotEmpty(children);
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

    public ParamTypeEnum getParamType() {
        return paramType;
    }

    public void setParamType(ParamTypeEnum paramType) {
        this.paramType = paramType;
    }

    public List<FieldInfo> getChildren() {
        return children;
    }

    public void setChildren(List<FieldInfo> children) {
        this.children = children;
    }

    public List<PsiAnnotation> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<PsiAnnotation> annotations) {
        this.annotations = annotations;
    }
}
