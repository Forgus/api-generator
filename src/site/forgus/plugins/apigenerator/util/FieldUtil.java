package site.forgus.plugins.apigenerator.util;

import com.intellij.psi.PsiType;

import java.text.SimpleDateFormat;
import java.util.*;

public class FieldUtil {

    public static final Map<String, Object> normalTypes = new HashMap<>();
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
        normalTypes.put("String", "String");
        normalTypes.put("Date", new SimpleDateFormat("YYYY-MM-dd HH:mm:ss").format(new Date()));
        normalTypes.put("BigDecimal", 0.111111);
        genericList.add("T");
        genericList.add("E");
        genericList.add("A");
        genericList.add("B");
        genericList.add("K");
        genericList.add("V");
    }

    public static Object getValue(PsiType psiType) {
        return normalTypes.get(psiType.getPresentableText());
    }


    public static boolean isNormalType(String typeName) {
        return normalTypes.containsKey(typeName);
    }

    private static boolean isIterableType(String typeName) {
        return typeName.startsWith("List") || typeName.startsWith("Set") || typeName.startsWith("Collection");
    }

    public static boolean isIterableType(PsiType psiType) {
        return isIterableType(psiType.getPresentableText());
    }

    public static boolean isNormalType(PsiType psiType) {
        return isNormalType(psiType.getPresentableText());
    }
}

