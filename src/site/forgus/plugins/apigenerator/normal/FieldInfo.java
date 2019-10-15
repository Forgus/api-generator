package site.forgus.plugins.apigenerator.normal;

import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.util.PsiUtil;
import com.sun.xml.internal.ws.wsdl.writer.document.ParamType;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import site.forgus.plugins.apigenerator.config.PersistentConfig;
import site.forgus.plugins.apigenerator.constant.TypeEnum;
import site.forgus.plugins.apigenerator.constant.WebAnnotation;
import site.forgus.plugins.apigenerator.util.AssertUtils;
import site.forgus.plugins.apigenerator.util.DesUtil;
import site.forgus.plugins.apigenerator.util.FieldUtil;

import java.util.*;

@Data
public class FieldInfo {

    private String name;
    private PsiType psiType;
    private boolean require;
    private String range;
    private String desc;
    private TypeEnum paramType;
    private List<FieldInfo> children;
    private FieldInfo parent;
    private List<PsiAnnotation> annotations;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldInfo fieldInfo = (FieldInfo) o;
        return name.equals(fieldInfo.name) &&
                Objects.equals(parent, fieldInfo.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, parent);
    }

    private static List<String> requiredTexts = Arrays.asList("@NotNull", "@NotBlank", "@NotEmpty", "@PathVariable");

    protected PersistentConfig config = PersistentConfig.getInstance();

    public FieldInfo(PsiType psiType) {
        this(psiType, "", new PsiAnnotation[0]);
    }

    public FieldInfo(String name, PsiType psiType, String desc, PsiAnnotation[] annotations) {
        RequireAndRange requireAndRange = getRequireAndRange(annotations);
        String fieldName = getParamName(name, annotations);
        this.name = fieldName == null ? "N/A" : fieldName;
        this.psiType = psiType;
        this.require = requireAndRange.isRequire();
        this.range = requireAndRange.getRange();
        this.desc = desc == null ? "" : desc;
        this.annotations = Arrays.asList(annotations);
        if (psiType != null) {
            if (FieldUtil.isNormalType(psiType)) {
                paramType = TypeEnum.LITERAL;
            } else if (FieldUtil.isIterableType(psiType)) {
                paramType = TypeEnum.ARRAY;
            } else {
                paramType = TypeEnum.OBJECT;
            }
            this.children = listChildren(this);
        } else {
            paramType = TypeEnum.OBJECT;
        }
    }

    public FieldInfo(FieldInfo parent, String name, PsiType psiType, String desc, PsiAnnotation[] annotations) {
        RequireAndRange requireAndRange = getRequireAndRange(annotations);
        String fieldName = getParamName(name, annotations);
        this.name = fieldName == null ? "N/A" : fieldName;
        this.psiType = psiType;
        this.require = requireAndRange.isRequire();
        this.range = requireAndRange.getRange();
        this.desc = desc == null ? "" : desc;
        this.annotations = Arrays.asList(annotations);
        this.parent = parent;
        if (psiType != null) {
            if (FieldUtil.isNormalType(psiType)) {
                paramType = TypeEnum.LITERAL;
            } else if (FieldUtil.isIterableType(psiType)) {
                paramType = TypeEnum.ARRAY;
            } else {
                paramType = TypeEnum.OBJECT;
            }
            if (needResolveChildren(parent, psiType)) {
                this.children = listChildren(this);
            }
        } else {
            paramType = TypeEnum.OBJECT;
        }
    }

    public FieldInfo(PsiType psiType, String desc, PsiAnnotation[] annotations) {
        this(psiType.getPresentableText(), psiType, desc, annotations);
    }

    private String getParamName(String name, PsiAnnotation[] annotations) {
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

    private PsiAnnotation getRequestParamAnnotation(PsiAnnotation[] annotations) {
        for (PsiAnnotation annotation : annotations) {
            if (annotation.getText().contains(WebAnnotation.RequestParam)) {
                return annotation;
            }
        }
        return null;
    }

    private List<FieldInfo> listChildren(FieldInfo fieldInfo) {
        PsiType psiType = fieldInfo.getPsiType();
        if (psiType == null) {
            return new ArrayList<>();
        }
        if (FieldUtil.isNormalType(psiType.getPresentableText())) {
            //基础类或基础包装类没有子域
            return new ArrayList<>();
        }
        List<FieldInfo> fieldInfos = new ArrayList<>();
        if (psiType instanceof PsiClassReferenceType) {
            //如果是集合类型
            if (FieldUtil.isIterableType(psiType)) {
                PsiType iterableType = PsiUtil.extractIterableTypeParameter(psiType, false);
                if (iterableType == null || FieldUtil.isNormalType(iterableType.getPresentableText())) {
                    return new ArrayList<>();
                }
                return listChildren(new FieldInfo(fieldInfo,iterableType.getPresentableText(), iterableType, "", new PsiAnnotation[0]));
            }
            String typeName = psiType.getPresentableText();
            if (typeName.startsWith("Map")) {
                fieldInfos.add(new FieldInfo(fieldInfo,typeName, null, "", new PsiAnnotation[0]));
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
                    fieldInfos.add(new FieldInfo(fieldInfo,outField.getName(), type, DesUtil.getDescription(outField.getDocComment()), outField.getAnnotations()));
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
                fieldInfos.add(new FieldInfo(fieldInfo,psiField.getName(), psiField.getType(), DesUtil.getDescription(psiField.getDocComment()), psiField.getAnnotations()));
            }
            return fieldInfos;
        }
        return new ArrayList<>();
    }

    private boolean needResolveChildren(FieldInfo parent, PsiType psiType) {
        if (parent == null) {
            return true;
        }
        Set<PsiType> resolvedTypeSet = new HashSet<>();
        FieldInfo p = parent;
        while (p != null) {
            resolvedTypeSet.add(p.getPsiType());
            p = p.getParent();
        }
        if(TypeEnum.ARRAY.equals(paramType)) {
            psiType = PsiUtil.extractIterableTypeParameter(psiType, false);
        }
        for (PsiType resolvedType : resolvedTypeSet) {
            if (resolvedType.equals(psiType)) {
                return false;
            }
        }
        return true;
    }

    private boolean containGeneric(String str) {
        for (String generic : FieldUtil.genericList) {
            if (str.contains(generic)) {
                return true;
            }
        }
        return false;
    }

    private RequireAndRange getRequireAndRange(PsiAnnotation[] annotations) {
        if (annotations.length == 0) {
            return RequireAndRange.instance();
        }
        boolean require = false;
        String min = "";
        String max = "";
        String range = "N/A";
        for (PsiAnnotation annotation : annotations) {
            if (isParamRequired(annotation)) {
                require = true;
                break;
            }
        }
        for (PsiAnnotation annotation : annotations) {
            String qualifiedName = annotation.getText();
            if (qualifiedName.contains("Length") || qualifiedName.contains("Range") || qualifiedName.contains("Size")) {
                PsiAnnotationMemberValue minValue = annotation.findAttributeValue("min");
                if (minValue != null) {
                    min = minValue.getText();
                    break;
                }
            }
            if (qualifiedName.contains("Min")) {
                PsiAnnotationMemberValue minValue = annotation.findAttributeValue("value");
                if (minValue != null) {
                    min = minValue.getText();
                    break;
                }
            }
        }
        for (PsiAnnotation annotation : annotations) {
            String qualifiedName = annotation.getText();
            if (qualifiedName.contains("Length") || qualifiedName.contains("Range") || qualifiedName.contains("Size")) {
                PsiAnnotationMemberValue maxValue = annotation.findAttributeValue("max");
                if (maxValue != null) {
                    max = maxValue.getText();
                    break;
                }
            }
            if (qualifiedName.contains("Max")) {
                PsiAnnotationMemberValue maxValue = annotation.findAttributeValue("value");
                if (maxValue != null) {
                    max = maxValue.getText();
                    break;
                }
            }
        }
        if (StringUtils.isNotEmpty(min) || StringUtils.isNotEmpty(max)) {
            range = "[" + min + "," + max + "]";
        }
        return new RequireAndRange(require, range);
    }

    private boolean isParamRequired(PsiAnnotation annotation) {
        String annotationText = annotation.getText();
        if (annotationText.contains(WebAnnotation.RequestParam)) {
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

    public boolean hasChildren() {
        return AssertUtils.isNotEmpty(children);
    }

}
