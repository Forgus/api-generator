package site.forgus.plugins.apigenerator.util;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiUtil;

import java.util.*;

public class FieldUtil {

    public static final Map<String, Object> normalTypes = new HashMap<>();

    public static final List<String> iterableTypes = Arrays.asList("List", "Set", "Collection");
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
        normalTypes.put("Short", Short.valueOf((short) 0));
        normalTypes.put("Integer", 0);
        normalTypes.put("Long", 0L);
        normalTypes.put("Float", 0.0F);
        normalTypes.put("Double", 0.0D);
        normalTypes.put("String", "@string");
        normalTypes.put("Date", new Date().getTime());
        normalTypes.put("BigDecimal", 0.111111);
        genericList.add("T");
        genericList.add("E");
        genericList.add("K");
        genericList.add("V");
    }

    public static Object getValue(PsiType psiType) {
        if (isIterableType(psiType)) {
            PsiType type = PsiUtil.extractIterableTypeParameter(psiType, false);
            if (type == null) {
                return "[]";
            }
            if (isNormalType(type)) {
                Object obj = normalTypes.get(type.getPresentableText());
                if (obj == null) {
                    return null;
                }
                return obj.toString() + "," + obj.toString();
            }
        }
        Object value = normalTypes.get(psiType.getPresentableText());
        return value == null ? "" : value;
    }


    public static boolean isNormalType(String typeName) {
        return normalTypes.containsKey(typeName);
    }

    private static boolean isIterableType(String typeName) {
        if (iterableTypes.contains(typeName)) {
            return true;
        }
        return typeName.startsWith("List<") || typeName.startsWith("Set<") || typeName.startsWith("Collection<");
    }

    public static boolean isIterableType(PsiType psiType) {
        return isIterableType(psiType.getPresentableText());
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
}

