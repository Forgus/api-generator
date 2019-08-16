package site.forgus.plugins.apigenerator.normal;

import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import com.intellij.psi.util.PsiUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class BuildMdForApi {

    private static NotificationGroup notificationGroup;


    static {
        notificationGroup = new NotificationGroup("Java2Json.NotificationGroup", NotificationDisplayType.BALLOON, true);
    }


    public static MethodInfo getMethodInfo(Project project, PsiMethod psiMethod) {
        MethodInfo methodInfo = new MethodInfo();
        List<FieldInfo> paramFieldInfos = listParamFieldInfos(project, psiMethod);
        List<FieldInfo> responseFieldInfos = listResponseFieldInfo(psiMethod, project);
        methodInfo.setDesc(DesUtil.getDescription(psiMethod));
        PsiClass psiClass = psiMethod.getContainingClass();
        methodInfo.setPackageName(PsiUtil.getPackageName(psiClass));
        methodInfo.setClassName(psiClass.getName());
        methodInfo.setReturnStr(psiMethod.getReturnType().getPresentableText());
        methodInfo.setParamStr(psiMethod.getParameterList().getText());
        methodInfo.setMethodName(psiMethod.getName());
        methodInfo.setRequestFields(paramFieldInfos);
        methodInfo.setResponseFields(responseFieldInfos);
        return methodInfo;
    }

    private static List<FieldInfo> listParamFieldInfos(Project project,PsiMethod psiMethod) {
        List<FieldInfo> fieldInfos =  new ArrayList<>();
        Map<String, String> paramDescMap = getParamDescMap(psiMethod.getDocComment());
        PsiParameter[] psiParameters = psiMethod.getParameterList().getParameters();
        for (PsiParameter psiParameter : psiParameters) {
            PsiType psiType = psiParameter.getType();
            FieldInfo fieldInfo = FieldInfo.normal(
                    psiParameter.getName(),
                    psiType,
                    paramDescMap.get(psiParameter.getName()),
                    listFieldInfos(project, psiType),
                    psiParameter.getAnnotations()
            );
            fieldInfos.add(fieldInfo);
        }
        return fieldInfos;
    }

    private static Map<String, String> getParamDescMap(PsiDocComment docComment) {
        Map<String, String> paramDescMap = new HashMap<>();
        if(docComment == null) {
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

    public static List<FieldInfo> listResponseFieldInfo(PsiMethod psiMethodTarget, Project project) {
        //todo
        PsiType psiType = psiMethodTarget.getReturnType();
        if(psiType == null) {
            return null;
        }
        if(NormalTypes.isNormalType(psiType.getPresentableText())) {
            return Collections.singletonList(FieldInfo.child(psiType.getPresentableText(), psiType,  "",new PsiAnnotation[0]));
        }
        return listFieldInfos(project,psiType);
    }

    private static List<FieldInfo> listFieldInfos(Project project, PsiType psiType) {
        if(psiType == null) {
            return new ArrayList<>();
        }
        if(psiType instanceof PsiClassReferenceType) {
            PsiClass psiClass = PsiUtil.resolveClassInType(psiType);
            String typeName = psiType.getPresentableText();
            if (NormalTypes.isNormalType(typeName)) {
                return new ArrayList<>();
            }
            if (typeName.startsWith("List") || typeName.startsWith("Set")) {
                PsiType iterableType = PsiUtil.extractIterableTypeParameter(psiType, false);
                if (iterableType == null || NormalTypes.isNormalType(iterableType.getPresentableText())) {
                    return new ArrayList<>();
                }
                PsiClass iterableClass = PsiUtil.resolveClassInType(iterableType);
                if (iterableClass == null) {
                    return listFieldInfos(project,iterableType);
                }
                List<FieldInfo> fieldInfos = new ArrayList<>();
                for (PsiField psiField : iterableClass.getAllFields()) {
                    resolveFields(project, fieldInfos, psiField);
                }
                return fieldInfos;
            }
            if(typeName.startsWith("Map")) {
                //TODO
                return new ArrayList<>();
            }
            if (typeName.contains("<")) {
                PsiClass outerClass = PsiUtil.resolveGenericsClassInType(psiType).getElement();
                PsiType innerType = PsiUtil.substituteTypeParameter(psiType, outerClass, 0, false);
                PsiElementFactory elementFactory = JavaPsiFacade.getInstance(project).getElementFactory();
                List<FieldInfo> fieldInfos = new ArrayList<>();
                for (PsiField outField : outerClass.getAllFields()) {
                    if (NormalTypes.genericList.contains(outField.getType().getPresentableText())) {
                        resolveFields(project, fieldInfos, elementFactory.createField(outField.getName(), innerType));
                    } else {
                        resolveFields(project, fieldInfos, outField);
                    }
                }
                return fieldInfos;
            }
            if (psiClass == null) {
                return new ArrayList<>();
            }
            List<FieldInfo> fieldInfos = new ArrayList<>();
            for (PsiField psiField : psiClass.getAllFields()) {
                resolveFields(project, fieldInfos, psiField);
            }
            return fieldInfos;
        }
        return new ArrayList<>();
    }

    public static List<FieldInfo> listFieldInfos(PsiClass psiClass, Project project) {
        List<FieldInfo> fieldInfos = new ArrayList<>();
        for (PsiField psiField : psiClass.getAllFields()) {
            resolveFields(project, fieldInfos, psiField);
        }
        return fieldInfos;
    }

    private static void resolveFields(Project project, List<FieldInfo> fieldInfos, PsiField psiField) {
        String name = psiField.getName();
        PsiType type = psiField.getType();
        String typeName = type.getPresentableText();
        if (NormalTypes.genericList.contains(typeName) || type instanceof PsiEnumConstant) {
            return;
        }
        String desc = DesUtil.getFiledDesc(psiField.getDocComment()).replace("@see", "见");
        if (NormalTypes.isNormalType(typeName)) {
            fieldInfos.add(FieldInfo.child(name, type, desc,psiField.getAnnotations()));
            return;
        }
        if (typeName.startsWith("List") || typeName.startsWith("Set")) {
            //list type
            PsiType iterableType = PsiUtil.extractIterableTypeParameter(type, false);
            if(iterableType == null || NormalTypes.isNormalType(iterableType.getPresentableText())) {
                fieldInfos.add(FieldInfo.child(name, type, desc,psiField.getAnnotations()));
                return;
            }
            fieldInfos.add(FieldInfo.parent(name, type, desc, listFieldInfos(project,iterableType),psiField.getAnnotations()));
            return;
        }
        if (typeName.startsWith("Map")) {
            //TODO
            return;
        }
        if (typeName.contains("<")) {
            PsiClass outerClass = PsiUtil.resolveGenericsClassInType(type).getElement();
            PsiType innerType = PsiUtil.substituteTypeParameter(type, outerClass, 0, false);
            PsiElementFactory elementFactory = JavaPsiFacade.getInstance(project).getElementFactory();
            for (PsiField outField : outerClass.getAllFields()) {
                if (NormalTypes.genericList.contains(outField.getType().getPresentableText())) {
                    resolveFields(project, fieldInfos, elementFactory.createField(outField.getName(), innerType));
                } else {
                    resolveFields(project, fieldInfos, outField);
                }
            }
            return;
        }
        PsiClass containingClass = psiField.getContainingClass();
        PsiClass psiClass = PsiUtil.resolveClassInType(type);
        if(psiClass.isEnum() || containingClass.getText().equals(psiClass.getText())) {
            fieldInfos.add(FieldInfo.normal(name,type,desc,new ArrayList<>(),psiField.getAnnotations()));
            return;
        }
        fieldInfos.add(FieldInfo.parent(name, type, desc, listFieldInfos(psiClass, project),psiField.getAnnotations()));
    }




}

