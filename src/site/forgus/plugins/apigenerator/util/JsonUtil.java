package site.forgus.plugins.apigenerator.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.psi.PsiClass;
import com.intellij.psi.util.PsiUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import site.forgus.plugins.apigenerator.normal.FieldInfo;
import site.forgus.plugins.apigenerator.normal.NormalTypes;
import site.forgus.plugins.apigenerator.normal.ParamTypeEnum;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonUtil {

    private static final Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.STATIC, Modifier.FINAL).setPrettyPrinting().create();

    public static String buildJson5(String prettyJson, Map<String, String> fieldDescMap) {
        String[] split = prettyJson.split("\n");
        StringBuffer json5 = new StringBuffer();
        for (String str : split) {
            String temp = str;
            for (Map.Entry<String, String> entry : fieldDescMap.entrySet()) {
                if (str.contains(entry.getKey())) {
                    temp = str + "//" + entry.getValue();
                    break;
                }
            }
            json5.append(temp);
            json5.append("\n");
        }
        return json5.toString();
    }

    public static String buildJson5(List<FieldInfo> fieldInfos) {
        return buildJson5(buildPrettyJson(fieldInfos), buildFieldDescMap(fieldInfos));
    }

    public static Map<String, String> buildFieldDescMap(List<FieldInfo> fieldInfos) {
        Map<String, String> map = new HashMap<>(32);
        if (fieldInfos == null) {
            return map;
        }
        for (FieldInfo fieldInfo : fieldInfos) {
            if (ParamTypeEnum.LITERAL.equals(fieldInfo.getParamType())) {
                if (StringUtils.isEmpty(fieldInfo.getDesc())) {
                    continue;
                }
                map.put(fieldInfo.getName(), buildDesc(fieldInfo));
            } else {
                map.putAll(buildFieldDescMap(fieldInfo.getChildren()));
            }
        }
        return map;
    }

    private static String buildDesc(FieldInfo fieldInfo) {
        String desc = fieldInfo.getDesc();
        if (!fieldInfo.isRequire()) {
            return desc;
        }
        return desc + ",必填";
    }

    public static String buildPrettyJson(List<FieldInfo> fieldInfos) {
        Map<String, Object> map = getStringObjectMap(fieldInfos);
        return gson.toJson(map);
    }

    private static Map<String, Object> getStringObjectMap(List<FieldInfo> fieldInfos) {
        Map<String, Object> map = new HashMap<>(64);
        for (FieldInfo fieldInfo : fieldInfos) {
            buildJsonValue(map, fieldInfo);
        }
        return map;
    }

    private static void buildJsonValue(Map<String, Object> map, FieldInfo fieldInfo) {
        if (ParamTypeEnum.LITERAL.equals(fieldInfo.getParamType())) {
            map.put(fieldInfo.getName(), fieldInfo.getValue());
        }
        if (ParamTypeEnum.ARRAY.equals(fieldInfo.getParamType())) {
            if (CollectionUtils.isNotEmpty(fieldInfo.getChildren())) {
                map.put(fieldInfo.getName(), Collections.singletonList(getStringObjectMap(fieldInfo.getChildren())));
                return;
            }
            PsiClass psiClass = PsiUtil.resolveClassInType(fieldInfo.getPsiType());
            String innerType = PsiUtil.substituteTypeParameter(fieldInfo.getPsiType(), psiClass, 0, true).getPresentableText();
            map.put(fieldInfo.getName(), Collections.singletonList(NormalTypes.normalTypes.get(innerType)));
        }
        if (fieldInfo.getChildren() == null) {
            return;
        }
        for (FieldInfo info : fieldInfo.getChildren()) {
            if (!info.getName().equals(fieldInfo.getName())) {
                map.put(fieldInfo.getName(), getStringObjectMap(fieldInfo.getChildren()));
            }
        }
    }
}
