package site.forgus.plugins.apigenerator.normal;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtil;
import org.apache.commons.lang.StringUtils;
import site.forgus.plugins.apigenerator.config.ApiGeneratorConfig;
import site.forgus.plugins.apigenerator.constant.TypeEnum;
import site.forgus.plugins.apigenerator.constant.WebAnnotation;
import site.forgus.plugins.apigenerator.util.FieldUtil;

import java.util.*;

/**
 * @author 孤峰
 * @since 2021/02/03
 */
public class FieldFactory {

    private static List<String> requiredTexts = Arrays.asList("@NotNull", "@NotBlank", "@NotEmpty", "@PathVariable");

    public static FieldInfo buildPsiType(Project project, PsiType psiType) {
        FieldInfo fieldInfo = new FieldInfo();
        fieldInfo.fieldType= FieldType.PSI_TYPE;
        fieldInfo.project = project;
        fieldInfo.config = ServiceManager.getService(project, ApiGeneratorConfig.class);
        fieldInfo.psiType = psiType;
        fieldInfo.genericTypeMap = resolveGenerics(psiType);
        TypeEnum paramType;
        if (FieldUtil.isNormalType(psiType)) {
            paramType = TypeEnum.LITERAL;
        } else if (FieldUtil.isIterableType(psiType)) {
            paramType = TypeEnum.ARRAY;
        } else {
            paramType = TypeEnum.OBJECT;
        }
        fieldInfo.paramType = paramType;
        if (needResolveChildren(psiType)) {
            fieldInfo.resolveChildren();
        }
        return fieldInfo;
    }

    public static FieldInfo buildField(Project project,String name, PsiType psiType, String desc, PsiAnnotation[] annotations) {
        FieldInfo fieldInfo = new FieldInfo();
        fieldInfo.fieldType = FieldType.FIELD;
        fieldInfo.project = project;
        fieldInfo.config = ServiceManager.getService(project,ApiGeneratorConfig.class);
        RequireAndRange requireAndRange = getRequireAndRange(annotations);
        String fieldName = getParamName(name, annotations);
        fieldInfo.name = fieldName == null ? "N/A" : fieldName;
        fieldInfo.psiType = psiType;
        fieldInfo.require = requireAndRange.isRequire();
        fieldInfo.range = requireAndRange.getRange();
        fieldInfo.desc = desc == null ? "" : desc;
        fieldInfo.annotations = Arrays.asList(annotations);
        if (psiType != null) {
            if (FieldUtil.isNormalType(psiType)) {
                fieldInfo.paramType = TypeEnum.LITERAL;
            } else if (FieldUtil.isIterableType(psiType)) {
                fieldInfo.paramType = TypeEnum.ARRAY;
            } else {
                fieldInfo.paramType = TypeEnum.OBJECT;
            }
            if (needResolveChildren(psiType)) {
                fieldInfo.resolveChildren();
            }
        } else {
            fieldInfo.paramType = TypeEnum.OBJECT;
        }
        return fieldInfo;
    }

    public static FieldInfo buildFieldWithParent(Project project,FieldInfo parent,String name, PsiType psiType, String desc, PsiAnnotation[] annotations) {
        FieldInfo fieldInfo = new FieldInfo();
        fieldInfo.fieldType = FieldType.FIELD;
        fieldInfo.project = project;
        fieldInfo.config = ServiceManager.getService(project,ApiGeneratorConfig.class);
        RequireAndRange requireAndRange = getRequireAndRange(annotations);
        String fieldName = getParamName(name, annotations);
        fieldInfo.name = fieldName == null ? "N/A" : fieldName;
        fieldInfo.psiType = psiType;
        fieldInfo.require = requireAndRange.isRequire();
        fieldInfo.range = requireAndRange.getRange();
        fieldInfo.desc = desc == null ? "" : desc;
        fieldInfo.annotations = Arrays.asList(annotations);
        fieldInfo.genericTypeMap = resolveGenerics(psiType);
        fieldInfo.parent = parent;
        if (psiType != null) {
            if (FieldUtil.isNormalType(psiType)) {
                fieldInfo.paramType = TypeEnum.LITERAL;
            } else if (FieldUtil.isIterableType(psiType)) {
                fieldInfo.paramType = TypeEnum.ARRAY;
            } else {
                fieldInfo.paramType = TypeEnum.OBJECT;
            }
            if (needResolveChildren(parent, psiType,fieldInfo.paramType)) {
                fieldInfo.resolveChildren();
            }
        } else {
            fieldInfo.paramType = TypeEnum.OBJECT;
        }
        return fieldInfo;
    }

    private static boolean isMapType(PsiType psiType) {
        String presentableText = psiType.getPresentableText();
        List<String> mapList = Arrays.asList("Map","HashMap","LinkedHashMap","JSONObject");
        if(mapList.contains(presentableText)) {
            return true;
        }
        return presentableText.startsWith("Map<") || presentableText.startsWith("HashMap<") || presentableText.startsWith("LinkedHashMap<");
    }

    private static boolean needResolveChildren(PsiType psiType) {
        PsiClass psiClass = PsiUtil.resolveClassInType(psiType);
        if(psiClass != null) {
            if(psiClass.isEnum()) {
                return false;
            }
        }
        return !isMapType(psiType);
    }

    private static boolean needResolveChildren(FieldInfo parent, PsiType psiType,TypeEnum paramType) {
        if (parent == null) {
            return true;
        }
        PsiClass psiClass = PsiUtil.resolveClassInType(psiType);
        if(psiClass != null) {
            if(psiClass.isEnum()) {
                return false;
            }
        }
        if (isMapType(psiType)) {
            return false;
        }
        Set<PsiType> resolvedTypeSet = new HashSet<>();
        FieldInfo p = parent;
        while (p != null) {
            resolvedTypeSet.add(p.getPsiType());
            p = p.parent;
        }
        if (TypeEnum.ARRAY.equals(paramType)) {
            if (psiType instanceof PsiArrayType) {
                psiType = ((PsiArrayType) psiType).getComponentType();
            } else {
                psiType = PsiUtil.extractIterableTypeParameter(psiType, false);
            }
        }
        if(resolvedTypeSet.contains(psiType)) {
            return false;
        }
        return true;
    }

    private static Map<PsiTypeParameter, PsiType> resolveGenerics(PsiType psiType){
        if(psiType instanceof PsiArrayType) {
            return new HashMap<>();
        }
        if(psiType instanceof PsiClassType) {
            PsiClassType psiClassType = (PsiClassType) psiType;
            PsiType[] realParameters = psiClassType.getParameters();
            PsiTypeParameter[] formParameters = psiClassType.resolve().getTypeParameters();
            int i = 0;
            Map<PsiTypeParameter, PsiType> map = new HashMap<>();
            for (PsiType realParameter : realParameters) {
                map.put(formParameters[i], realParameter);
                i ++;
            }
            return map;
        }
        return new HashMap<>();
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

    private static boolean isParamRequired(PsiAnnotation annotation) {
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
        return requiredTexts.contains(annotationText.split("\\(")[0]);
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
            if (annotation.getText().contains(WebAnnotation.RequestParam)) {
                return annotation;
            }
        }
        return null;
    }

}
