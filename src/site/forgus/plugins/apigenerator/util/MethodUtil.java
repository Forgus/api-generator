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
import site.forgus.plugins.apigenerator.normal.NormalTypes;

import java.util.*;

public class MethodUtil {

    public static MethodInfo getMethodInfo(Project project, PsiMethod psiMethod) {
        MethodInfo methodInfo = new MethodInfo();
        List<FieldInfo> paramFieldInfoList = listParamFieldInfos(project, psiMethod);
        List<FieldInfo> responseFieldInfoList = listResponseFieldInfo(project,psiMethod);
        methodInfo.setDesc(DesUtil.getDescription(psiMethod));
        PsiClass psiClass = psiMethod.getContainingClass();
        methodInfo.setPackageName(PsiUtil.getPackageName(psiClass));
        methodInfo.setClassName(psiClass.getName());
        methodInfo.setReturnStr(psiMethod.getReturnType().getPresentableText());
        methodInfo.setParamStr(psiMethod.getParameterList().getText());
        methodInfo.setMethodName(psiMethod.getName());
        methodInfo.setRequestFields(paramFieldInfoList);
        methodInfo.setResponseFields(responseFieldInfoList);
        return methodInfo;
    }

    private static List<FieldInfo> listParamFieldInfos(Project project, PsiMethod psiMethod) {
        List<FieldInfo> fieldInfoList = new ArrayList<>();
        Map<String, String> paramNameDescMap = getParamDescMap(psiMethod.getDocComment());
        PsiParameter[] psiParameters = psiMethod.getParameterList().getParameters();
        for (PsiParameter psiParameter : psiParameters) {
            PsiType psiType = psiParameter.getType();
            FieldInfo fieldInfo = FieldInfo.resolve(
                    psiParameter.getName(),
                    psiType,
                    paramNameDescMap.get(psiParameter.getName()),
                    project,
                    psiParameter.getAnnotations()
            );
            fieldInfoList.add(fieldInfo);
        }
        return fieldInfoList;
    }

    public static List<FieldInfo> listResponseFieldInfo(Project project,PsiMethod psiMethod) {
        PsiType psiType = psiMethod.getReturnType();
        if (psiType == null) {
            return null;
        }
        if (NormalTypes.isNormalType(psiType.getPresentableText())) {
            return Collections.singletonList(FieldInfo.child(psiType.getPresentableText(), psiType, "", new PsiAnnotation[0]));
        }
        return FieldInfo.listFieldInfos(project, psiType);
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
        String desc = tagText.split(" ")[2];
        return desc.replace("\n", "");
    }

}

