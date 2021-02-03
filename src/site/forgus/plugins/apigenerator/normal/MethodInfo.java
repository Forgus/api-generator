package site.forgus.plugins.apigenerator.normal;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import com.intellij.psi.util.PsiUtil;
import org.apache.commons.lang.StringUtils;
import site.forgus.plugins.apigenerator.util.DesUtil;

import java.io.Serializable;
import java.util.*;

public class MethodInfo implements Serializable {
    private static final long serialVersionUID = -9143203778013000538L;

    private String desc;
    private String packageName;
    private String className;
    private String returnStr;
    private String paramStr;
    private String methodName;
    private List<FieldInfo> requestFields;
    private FieldInfo response;

    private List<String> excludeParamTypes = Arrays.asList("RedirectAttributes", "HttpServletRequest", "HttpServletResponse");

    public MethodInfo(PsiMethod psiMethod) {
        this.setDesc(DesUtil.getDescription(psiMethod));
        PsiClass psiClass = psiMethod.getContainingClass();
        if (psiClass == null) {
            return;
        }
        this.setPackageName(PsiUtil.getPackageName(psiClass));
        this.setClassName(psiClass.getName());
        PsiType returnType = psiMethod.getReturnType();
        if (returnType != null) {
            this.setReturnStr(returnType.getPresentableText());
        }
        this.setParamStr(psiMethod.getParameterList().getText());
        this.setMethodName(psiMethod.getName());
        this.setRequestFields(listParamFieldInfos(psiMethod));
        this.setResponse(FieldFactory.buildPsiType(psiMethod.getProject(), psiMethod.getReturnType()));
    }

    private List<FieldInfo> listParamFieldInfos(PsiMethod psiMethod) {
        List<FieldInfo> fieldInfoList = new ArrayList<>();
        Map<String, String> paramNameDescMap = getParamDescMap(psiMethod.getDocComment());
        PsiParameter[] psiParameters = psiMethod.getParameterList().getParameters();
        for (PsiParameter psiParameter : psiParameters) {
            PsiType psiType = psiParameter.getType();
            if (excludeParamTypes.contains(psiType.getPresentableText())) {
                continue;
            }
            FieldInfo fieldInfo = new FieldInfo(
                    psiMethod.getProject(),
                    psiParameter.getName(),
                    psiType,
                    paramNameDescMap.get(psiParameter.getName()),
                    psiParameter.getAnnotations()
            );
            fieldInfoList.add(fieldInfo);
        }
        return fieldInfoList;
    }

    private Map<String, String> getParamDescMap(PsiDocComment docComment) {
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

    private String getParamDesc(String tagText) {
        String[] strings = tagText.replace("*", "").trim().split(" ");
        if (strings.length == 3) {
            String desc = strings[2];
            return desc.replace("\n", "");
        }
        return "";
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getReturnStr() {
        return returnStr;
    }

    public void setReturnStr(String returnStr) {
        this.returnStr = returnStr;
    }

    public String getParamStr() {
        return paramStr;
    }

    public void setParamStr(String paramStr) {
        this.paramStr = paramStr;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public List<FieldInfo> getRequestFields() {
        return requestFields;
    }

    public void setRequestFields(List<FieldInfo> requestFields) {
        this.requestFields = requestFields;
    }

    public FieldInfo getResponse() {
        return response;
    }

    public void setResponse(FieldInfo response) {
        this.response = response;
    }

    public List<String> getExcludeParamTypes() {
        return excludeParamTypes;
    }

    public void setExcludeParamTypes(List<String> excludeParamTypes) {
        this.excludeParamTypes = excludeParamTypes;
    }
}
