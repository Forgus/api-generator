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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuildMdForDubbo {

    private static NotificationGroup notificationGroup;


    static {
        notificationGroup = new NotificationGroup("Java2Json.NotificationGroup", NotificationDisplayType.BALLOON, true);
    }


    public static MethodInfo getMethod(Project project,PsiMethod psiMethod) {
        MethodInfo methodInfo = new MethodInfo();
        List<FieldInfo> paramFieldInfos = listParamFieldInfos(project, psiMethod);
        List<FieldInfo> responseFieldInfos = listResponseFieldInfos(psiMethod, project);
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
        PsiDocComment docComment = psiMethod.getDocComment();
        Map<String, String> paramDescMap = new HashMap<>();
        for (PsiDocTag docTag : docComment.getTags()) {
            String tagText = docTag.getText();
            String tagName = docTag.getName();
            String tagValue = docTag.getValueElement() == null ? "" : docTag.getValueElement().getText();
            if ("param".equals(tagName) && StringUtils.isNotEmpty(tagValue)) {
                paramDescMap.put(tagValue, getParamDesc(tagText));
            }
        }
        PsiParameter[] psiParameters = psiMethod.getParameterList().getParameters();
        for (PsiParameter psiParameter : psiParameters) {
            PsiType psiType = psiParameter.getType();
            FieldInfo fieldInfo = FieldInfo.normal(
                    psiParameter.getName(),
                    psiType,
                    getRequireAndRange(psiParameter.getAnnotations()),
                    paramDescMap.get(psiParameter.getName()),
                    listFieldInfos(project, psiType)
            );
            fieldInfos.add(fieldInfo);
        }
        return fieldInfos;
    }

    private static String getParamDesc(String tagText) {
        String desc = tagText.split(" ")[2];
        return desc.replace("\n", "");
    }

    public static List<FieldInfo> listResponseFieldInfos(PsiMethod psiMethodTarget, Project project) {
        PsiType psiType = psiMethodTarget.getReturnType();
        if(psiType == null) {
            return new ArrayList<>();
        }
        return listResponseFieldInfos(psiType,project);
    }

    public static List<FieldInfo> listResponseFieldInfos(PsiType psiType,Project project) {
        List<FieldInfo> fieldInfos = new ArrayList<>();
        resolveAndFillFieldInfos(project,psiType,fieldInfos);
        return fieldInfos;
    }

    private static List<FieldInfo> listFieldInfos(Project project, PsiType psiType) {
        List<FieldInfo> fieldInfos = new ArrayList<>();
        String typeName = psiType.getPresentableText();
        if (psiType instanceof PsiClassReferenceType) {
            if (typeName.startsWith("List") || typeName.startsWith("Set")) {
                if (typeName.contains("<")) {
                    PsiType iterableType = PsiUtil.extractIterableTypeParameter(psiType, false);
                    if (iterableType == null) {
                        return null;
                    }
                    if (NormalTypes.isNormalType(iterableType.getPresentableText())) {
                        fieldInfos.add(FieldInfo.child("N/A", psiType, RequireAndRange.instance(), ""));
                    } else {
                        PsiClass iterableClass = PsiUtil.resolveClassInType(iterableType);
                        if (iterableClass == null) {
                            return null;
                        }
                        for (PsiField psiField : iterableClass.getAllFields()) {
                            resolveFields(project, fieldInfos, psiField);
                        }
                    }
                } else {
                    fieldInfos.add(FieldInfo.child("N/A", psiType, RequireAndRange.instance(), ""));
                }
            } else if (NormalTypes.isNormalType(typeName)) {
                fieldInfos.add(FieldInfo.child(typeName, psiType, RequireAndRange.instance(), ""));
            } else if (typeName.contains("<")) {
                PsiClass outerClass = PsiUtil.resolveGenericsClassInType(psiType).getElement();
                if (outerClass == null) {
                    return null;
                }
                PsiType innerType = PsiUtil.substituteTypeParameter(psiType, outerClass, 0, false);
                if (innerType == null) {
                    return null;
                }
                PsiElementFactory elementFactory = JavaPsiFacade.getInstance(project).getElementFactory();
                for (PsiField outField : outerClass.getAllFields()) {
                    if (NormalTypes.genericList.contains(outField.getType().getPresentableText())) {
                        resolveFields(project, fieldInfos, elementFactory.createField(outField.getName() == null ? "" : outField.getName(), innerType));
                    } else {
                        resolveFields(project, fieldInfos, outField);
                    }
                }
            } else {
                PsiClass psiClass = PsiUtil.resolveClassInType(psiType);
                if (psiClass == null) {
                    return null;
                }
                for (PsiField psiField : psiClass.getAllFields()) {
                    resolveFields(project, fieldInfos, psiField);
                }
            }
        } else {
            fieldInfos.add(FieldInfo.child(typeName, psiType, RequireAndRange.instance(), ""));
        }
        return fieldInfos;
    }

    private static void resolveAndFillFieldInfos(Project project, PsiType psiType, List<FieldInfo> fieldInfos) {
        String typeName = psiType.getPresentableText();
        if (psiType instanceof PsiClassReferenceType) {
            if (typeName.startsWith("List") || typeName.startsWith("Set")) {
                if (typeName.contains("<")) {
                    PsiType iterableType = PsiUtil.extractIterableTypeParameter(psiType, false);
                    if (iterableType == null) {
                        return;
                    }
                    if (NormalTypes.isNormalType(iterableType.getPresentableText())) {
                        fieldInfos.add(FieldInfo.child("N/A", psiType, RequireAndRange.instance(), ""));
                    } else {
                        PsiClass iterableClass = PsiUtil.resolveClassInType(iterableType);
                        if (iterableClass == null) {
                            return;
                        }
                        for (PsiField psiField : iterableClass.getAllFields()) {
                            resolveFields(project, fieldInfos, psiField);
                        }
                    }
                } else {
                    fieldInfos.add(FieldInfo.child("N/A", psiType, RequireAndRange.instance(), ""));
                }
            } else if (NormalTypes.isNormalType(typeName)) {
                fieldInfos.add(FieldInfo.child(typeName, psiType, RequireAndRange.instance(), ""));
            } else if (typeName.contains("<")) {
                PsiClass outerClass = PsiUtil.resolveGenericsClassInType(psiType).getElement();
                if (outerClass == null) {
                    return;
                }
                PsiType innerType = PsiUtil.substituteTypeParameter(psiType, outerClass, 0, false);
                if (innerType == null) {
                    return;
                }
                PsiElementFactory elementFactory = JavaPsiFacade.getInstance(project).getElementFactory();
                for (PsiField outField : outerClass.getAllFields()) {
                    if (NormalTypes.genericList.contains(outField.getType().getPresentableText())) {
                        resolveFields(project, fieldInfos, elementFactory.createField(outField.getName() == null ? "" : outField.getName(), innerType));
                    } else {
                        resolveFields(project, fieldInfos, outField);
                    }
                }
            } else {
                PsiClass psiClass = PsiUtil.resolveClassInType(psiType);
                if (psiClass == null) {
                    return;
                }
                for (PsiField psiField : psiClass.getAllFields()) {
                    resolveFields(project, fieldInfos, psiField);
                }
            }
        } else {
            fieldInfos.add(FieldInfo.child(typeName, psiType, RequireAndRange.instance(), ""));
        }
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
        if (NormalTypes.genericList.contains(typeName)) {
            return;
        }
        RequireAndRange requireAndRange = getRequireAndRange(psiField.getAnnotations());
        String desc = DesUtil.getFiledDesc(psiField.getDocComment()).replace("@see", "见");
        if (NormalTypes.isNormalType(typeName)) {
            fieldInfos.add(FieldInfo.child(name, type, requireAndRange, desc));
        } else if (typeName.startsWith("List") || typeName.startsWith("Set")) {
            //list type
            PsiType iterableType = PsiUtil.extractIterableTypeParameter(type, false);
            if (NormalTypes.isNormalType(typeName)) {
                fieldInfos.add(FieldInfo.child(name, type, requireAndRange, desc));
            } else {
                PsiClass iterableClass = PsiUtil.resolveClassInClassTypeOnly(iterableType);
                if (iterableClass == null) {
                    return;
                }
                fieldInfos.add(FieldInfo.parent(name, type, requireAndRange, desc, listFieldInfos(iterableClass, project)));
            }
        } else if (typeName.startsWith("Map")) {
            //TODO
        } else if (typeName.contains("<")) {
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
        } else {
            //class type
            fieldInfos.add(FieldInfo.parent(name, type, requireAndRange, desc, listFieldInfos(PsiUtil.resolveClassInType(type), project)));
        }
    }

    private static RequireAndRange getRequireAndRange(PsiAnnotation[] annotations) {
        boolean require = false;
        String range = "N/A";
        for (PsiAnnotation annotation : annotations) {
            String qualifiedName = annotation.getText();
            if (qualifiedName.contains("NotNull") || qualifiedName.contains("NotBlank") || qualifiedName.contains("NotEmpty")) {
                require = true;
            }
            if (qualifiedName.contains("Length") || qualifiedName.contains("Range")) {
                String min = "";
                String max = "";
                PsiAnnotationMemberValue maxValue = annotation.findAttributeValue("max");
                PsiAnnotationMemberValue minValue = annotation.findAttributeValue("min");
                if (maxValue != null) {
                    max = maxValue.getText();
                }
                if (minValue != null) {
                    min = "0".equals(minValue.getText()) ? "1" : minValue.getText();
                }
                if (StringUtils.isNotEmpty(min) && StringUtils.isNotEmpty(max)) {
                    range = "[" + min + "," + max + "]";
                }
            }
        }
        return new RequireAndRange(require, range);
    }


}

