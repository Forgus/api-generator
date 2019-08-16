package site.forgus.plugins.apigenerator.yapi.util;

import com.google.gson.GsonBuilder;
import org.apache.commons.lang.StringUtils;
import site.forgus.plugins.apigenerator.normal.FieldInfo;
import site.forgus.plugins.apigenerator.normal.ParamTypeEnum;

import java.util.*;

public class JsonUtil {

    public static String buildJson5(String prettyJson,Map<String,String> fieldDescMap) {
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
        return buildJson5(buildPrettyJson(fieldInfos),buildFieldDescMap(fieldInfos));
    }

    public static Map<String,String> buildFieldDescMap(List<FieldInfo> fieldInfos) {
        Map<String,String> map = new HashMap<>(32);
        if(fieldInfos == null) {
            return map;
        }
        for (FieldInfo fieldInfo : fieldInfos) {
            if (ParamTypeEnum.LITERAL.equals(fieldInfo.getParamType())) {
                if(StringUtils.isEmpty(fieldInfo.getDesc())) {
                    continue;
                }
                map.put(fieldInfo.getName(), buildDesc(fieldInfo));
            }else {
                map.putAll(buildFieldDescMap(fieldInfo.getChildren()));
            }
        }
        return map;
    }

    private static String buildDesc(FieldInfo fieldInfo) {
        String desc = fieldInfo.getDesc();
        if(!fieldInfo.isRequire()) {
            return desc;
        }
        return desc + ",必填";
    }

    public static String buildPrettyJson(List<FieldInfo> fieldInfos) {
        Map<String, Object> map = new HashMap<>(32);
        for (FieldInfo fieldInfo : fieldInfos) {
            if (ParamTypeEnum.LITERAL.equals(fieldInfo.getParamType())) {
                map.put(fieldInfo.getName(), fieldInfo.getValue());
            } else if (ParamTypeEnum.ARRAY.equals(fieldInfo.getParamType())) {
                map.put(fieldInfo.getName(), Arrays.asList(buildObjectDemo(fieldInfo.getChildren())));
            } else {
                map.put(fieldInfo.getName(), buildObjectDemo(fieldInfo.getChildren()));
            }
        }
        return new GsonBuilder().setPrettyPrinting().create().toJson(map);
    }

    private static Object buildObjectDemo(List<FieldInfo> fieldInfos) {
        Map<String, Object> map = new HashMap<>(32);
        if (fieldInfos == null) {
            return map;
        }
        for (FieldInfo fieldInfo : fieldInfos) {
            if (ParamTypeEnum.LITERAL.equals(fieldInfo.getParamType())) {
                map.put(fieldInfo.getName(), fieldInfo.getValue());
            } else if (ParamTypeEnum.ARRAY.equals(fieldInfo.getParamType())) {
                map.put(fieldInfo.getName(), Collections.singletonList(buildObjectDemo(fieldInfo.getChildren())));
            } else {
                map.put(fieldInfo.getName(), buildObjectDemo(fieldInfo.getChildren()));
            }
        }
        return map;
    }
}
