package site.forgus.plugins.apigenerator.util;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import com.intellij.psi.util.PsiUtil;
import org.apache.commons.lang.StringUtils;
import site.forgus.plugins.apigenerator.normal.DesUtil;
import site.forgus.plugins.apigenerator.normal.FieldInfo;
import site.forgus.plugins.apigenerator.normal.MethodInfo;

import java.util.*;

public class MethodUtil {

    private static final List<String> excludeParamTypes = Arrays.asList("RedirectAttributes", "HttpServletRequest", "HttpServletResponse");

    public static MethodInfo getMethodInfo(Project project, PsiMethod psiMethod) {
        MethodInfo methodInfo = new MethodInfo();
        methodInfo.setDesc(DesUtil.getDescription(psiMethod));
        PsiClass psiClass = psiMethod.getContainingClass();
        methodInfo.setPackageName(PsiUtil.getPackageName(psiClass));
        methodInfo.setClassName(psiClass.getName());
        methodInfo.setReturnStr(psiMethod.getReturnType().getPresentableText());
        methodInfo.setParamStr(psiMethod.getParameterList().getText());
        methodInfo.setMethodName(psiMethod.getName());
        methodInfo.setRequestFields(listParamFieldInfos(project, psiMethod));
        methodInfo.setResponseFields(listResponseFieldInfos(project, psiMethod));
        return methodInfo;
    }

    private static List<FieldInfo> listParamFieldInfos(Project project, PsiMethod psiMethod) {
        List<FieldInfo> fieldInfoList = new ArrayList<>();
        Map<String, String> paramNameDescMap = getParamDescMap(psiMethod.getDocComment());
        PsiParameter[] psiParameters = psiMethod.getParameterList().getParameters();
        for (PsiParameter psiParameter : psiParameters) {
            PsiType psiType = psiParameter.getType();
            if (excludeParamTypes.contains(psiType.getPresentableText())) {
                continue;
            }
            FieldInfo fieldInfo = FieldInfo.normal(project,
                    psiParameter.getName(),
                    psiType,
                    paramNameDescMap.get(psiParameter.getName()),
                    psiParameter.getAnnotations()
            );
            fieldInfoList.add(fieldInfo);
        }
        return fieldInfoList;
    }

    public static List<FieldInfo> listResponseFieldInfos(Project project, PsiMethod psiMethod) {
        return getResponseFieldInfo(project,psiMethod).getChildren();
    }

    public static FieldInfo getResponseFieldInfo(Project project,PsiMethod psiMethod) {
        return FieldInfo.normal(project,psiMethod.getReturnType(), "",  new PsiAnnotation[0]);
    }

    private static Map<String, String> getParamDescMap(PsiDocComment docComment) {
        Map<String, String> paramDescMap = new HashMap<>();
        if (docComment == null) {
            return paramDescMap;
        }
        for (PsiDocTag docTag : docComment.getTags()) {
            String tagValue = docTag.getValueElement() == null ? "" : docTag.getValueElement().getText();
            if ("param".equals(docTag.getName()) && StringUtils.isNotEmpty(tagValue)) {
                paramDescMap.put(tagValue, getParamDesc(docTag.getText()));
            }
        }
        return paramDescMap;
    }

    private static String getParamDesc(String tagText) {
        String[] strings = tagText.split(" ");
        if (strings.length == 3) {
            String desc = strings[2];
            return desc.replace("\n", "");
        }
        return "";
    }

}

