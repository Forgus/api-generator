package site.forgus.plugins.apigenerator.util;

import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtil;
import org.apache.commons.lang.StringUtils;
import site.forgus.plugins.apigenerator.constant.JacksonAnnotation;
import site.forgus.plugins.apigenerator.constant.SwaggerAnnotation;
import site.forgus.plugins.apigenerator.constant.WebAnnotation;
import site.forgus.plugins.apigenerator.normal.FieldInfo;
import site.forgus.plugins.apigenerator.normal.RequireAndRange;

import java.util.*;

public class FieldUtil {

    public static final Map<String, Object> normalTypes = new HashMap<>();

    private static List<String> requiredTexts = Arrays.asList("@NotNull", "@NotBlank", "@NotEmpty", "@PathVariable");
    /**
     * 泛型列表
     */
    public static final List<String> genericList = new ArrayList<>();


    static {
        normalTypes.put("int", 1);
        normalTypes.put("boolean", false);
        normalTypes.put("byte", 1);
        normalTypes.put("short", 1);
        normalTypes.put("long", 1L);
        normalTypes.put("float", 1.0F);
        normalTypes.put("double", 1.0D);
        normalTypes.put("char", 'a');
        normalTypes.put("Boolean", false);
        normalTypes.put("Byte", 0);
        normalTypes.put("Short", (short) 0);
        normalTypes.put("Integer", 0);
        normalTypes.put("Long", 0L);
        normalTypes.put("Float", 0.0F);
        normalTypes.put("Double", 0.0D);
        normalTypes.put("String", "@string");
        normalTypes.put("Date", new Date().getTime());
        normalTypes.put("BigDecimal", 0.111111);
        normalTypes.put("LocalDateTime", "yyyy-MM-dd HH:mm:ss");
        normalTypes.put("BigInteger", 0);
        genericList.add("T");
        genericList.add("E");
        genericList.add("K");
        genericList.add("V");
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
        if(requiredTexts.contains(annotationText.split("\\(")[0])) {
            return true;
        }
        if(annotationText.contains(SwaggerAnnotation.ApiModelProperty)) {
            PsiNameValuePair[] psiNameValuePairs = annotation.getParameterList().getAttributes();
            for (PsiNameValuePair psiNameValuePair : psiNameValuePairs) {
                if ("required".equals(psiNameValuePair.getName()) && "true".equals(psiNameValuePair.getLiteralValue())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static RequireAndRange getRequireAndRange(PsiAnnotation[] annotations) {
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

    public static Object getValue(FieldInfo fieldInfo) {
        PsiType psiType = fieldInfo.getPsiType();
        if (isIterableType(psiType)) {
            PsiType type = PsiUtil.extractIterableTypeParameter(psiType, false);
            if (type == null) {
                return "[]";
            }
            if (isNormalType(type)) {
                Object value = getValue(psiType.getPresentableText(),fieldInfo.getAnnotations());
                if (value == null) {
                    return null;
                }
                return value.toString() + "," + value.toString();
            }
        }
        Object value = getValue(psiType.getPresentableText(),fieldInfo.getAnnotations());
        return value == null ? "" : value;
    }

    private static Object getValue(String typeStr,List<PsiAnnotation> annotations) {
        if(Arrays.asList("LocalDateTime","Date").contains(typeStr)) {
            for (PsiAnnotation annotation : annotations) {
                if(annotation.getText().contains(JacksonAnnotation.JsonFormat)) {
                    PsiNameValuePair[] attributes = annotation.getParameterList().getAttributes();
                    if (attributes.length >= 1) {
                        for (PsiNameValuePair attribute : attributes) {
                            if ("pattern".equals(attribute.getName())) {
                                return attribute.getLiteralValue();
                            }
                        }
                    }
                }
            }
        }
        return normalTypes.get(typeStr);
    }

    public static boolean isNormalType(String typeName) {
        return normalTypes.containsKey(typeName);
    }

    public static boolean isGenericType(PsiType psiType) {
        return psiType != null && isGenericType(psiType.getPresentableText());
    }
    public static boolean isGenericType(String typeName) {
        return genericList.contains(typeName);
    }

    public static boolean isIterableType(String typeName) {
        return isCollectionType(typeName) || typeName.contains("[]");
    }

    public static boolean isIterableType(PsiType psiType) {
        return isIterableType(psiType.getPresentableText());
    }

    public static boolean isCollectionType(PsiType psiType) {
        return isCollectionType(psiType.getPresentableText());
    }

    private static boolean isCollectionType(String typeName) {
        if (AssertUtils.isEmpty(typeName)) {
            return false;
        }
        return typeName.startsWith("List<")
                || typeName.startsWith("Set<")
                || typeName.startsWith("Collection<");
    }

    public static boolean isNormalType(PsiType psiType) {
        PsiClass psiClass = PsiUtil.resolveClassInType(psiType);
        if (psiClass != null) {
            if (psiClass.isEnum()) {
                return true;
            }
        }
        return isNormalType(psiType.getPresentableText());
    }

    public static PsiAnnotation findAnnotationByName(List<PsiAnnotation> annotations, String text) {
        if (annotations == null) {
            return null;
        }
        for (PsiAnnotation annotation : annotations) {
            if (annotation.getText().contains(text)) {
                return annotation;
            }
        }
        return null;
    }

    public static boolean isMapType(PsiType psiType) {
        String presentableText = psiType.getPresentableText();
        List<String> mapList = Arrays.asList("Map","HashMap","LinkedHashMap","JSONObject");
        if(mapList.contains(presentableText)) {
            return true;
        }
        return presentableText.startsWith("Map<") || presentableText.startsWith("HashMap<") || presentableText.startsWith("LinkedHashMap<");
    }

    public static boolean isStaticField(PsiField psiField) {
        PsiModifierList modifierList = psiField.getModifierList();
        if(modifierList == null) {
            return false;
        }
        for (PsiElement child : modifierList.getChildren()) {
            if(child instanceof PsiKeyword) {
                if(child.getText().equals("static")) {
                    return true;
                }
            }
        }
        return false;
    }
}

