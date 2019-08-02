package site.forgus.plugins.apigenerator.normal;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class BuildMdForDubbo {

    private static NotificationGroup notificationGroup;

    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    static {
        notificationGroup = new NotificationGroup("Java2Json.NotificationGroup", NotificationDisplayType.BALLOON, true);
    }


    public List<FieldInfo> listRequestParamModel(AnActionEvent e) {
        Editor editor = e.getDataContext().getData(CommonDataKeys.EDITOR);
        Project project = editor.getProject();
        String selectedText = e.getRequiredData(CommonDataKeys.EDITOR).getSelectionModel().getSelectedText();
        if (Strings.isNullOrEmpty(selectedText)) {
            Notification error = notificationGroup.createNotification("please select method or class", NotificationType.ERROR);
            Notifications.Bus.notify(error, project);
            return null;
        }
        PsiFile psiFile = e.getDataContext().getData(CommonDataKeys.PSI_FILE);
        PsiElement referenceAt = psiFile.findElementAt(editor.getCaretModel().getOffset());
        PsiClass selectedClass = (PsiClass) PsiTreeUtil.getContextOfType(referenceAt, new Class[]{PsiClass.class});
        List<FieldInfo> fieldInfos = new ArrayList<>();
        if (selectedText.equals(selectedClass.getName())) {
            //TODO
        } else {
            PsiMethod[] psiMethods = selectedClass.getAllMethods();
            //寻找目标Method
            PsiMethod psiMethodTarget = null;
            for (PsiMethod psiMethod : psiMethods) {
                if (psiMethod.getName().equals(selectedText)) {
                    psiMethodTarget = psiMethod;
                    break;
                }
            }
            //判断是否有匹配的目标方法
            if (psiMethodTarget == null) {
                Notification error = notificationGroup.createNotification("please check method name", NotificationType.ERROR);
                Notifications.Bus.notify(error, project);
                return null;
            }
            PsiParameter[] psiParameters = psiMethodTarget.getParameterList().getParameters();
            for (PsiParameter psiParameter : psiParameters) {
                resolveAndFillFieldInfos(project, psiParameter, fieldInfos);
            }
        }
        return fieldInfos;
    }

    public List<FieldInfo> generateResponseFieldInfos(AnActionEvent e) {
        Editor editor = e.getDataContext().getData(CommonDataKeys.EDITOR);
        Project project = editor.getProject();
        String selectedText = e.getRequiredData(CommonDataKeys.EDITOR).getSelectionModel().getSelectedText();
        if (Strings.isNullOrEmpty(selectedText)) {
            Notification error = notificationGroup.createNotification("please select method or class", NotificationType.ERROR);
            Notifications.Bus.notify(error, project);
            return null;
        }
        PsiFile psiFile = e.getDataContext().getData(CommonDataKeys.PSI_FILE);
        PsiElement referenceAt = psiFile.findElementAt(editor.getCaretModel().getOffset());
        PsiClass selectedClass = (PsiClass) PsiTreeUtil.getContextOfType(referenceAt, new Class[]{PsiClass.class});
        List<FieldInfo> fieldDocVOS = new ArrayList<>();
        if (selectedText.equals(selectedClass.getName())) {
            //TODO
        } else {
            PsiMethod[] psiMethods = selectedClass.getAllMethods();
            //寻找目标Method
            PsiMethod psiMethodTarget = null;
            for (PsiMethod psiMethod : psiMethods) {
                if (psiMethod.getName().equals(selectedText)) {
                    psiMethodTarget = psiMethod;
                    break;
                }
            }
            fieldDocVOS.addAll(listResponseFieldInfos(psiMethodTarget, project));
        }
        return fieldDocVOS;
    }

    public List<FieldInfo> listResponseFieldInfos(PsiMethod psiMethodTarget, Project project) {
        //判断是否有匹配的目标方法
        if (psiMethodTarget == null) {
            Notification error = notificationGroup.createNotification("please check method name", NotificationType.ERROR);
            Notifications.Bus.notify(error, project);
            return null;
        }
        PsiType psiType = psiMethodTarget.getReturnType();
        List<FieldInfo> fieldInfos = new ArrayList<>();
        if (psiType != null) {
            resolveAndFillFieldInfos(project, psiType, fieldInfos);
        }
        return fieldInfos;
    }

    private void resolveAndFillFieldInfos(Project project, PsiParameter psiParameter, List<FieldInfo> fieldInfoList) {
        PsiType psiType = psiParameter.getType();
        List<FieldInfo> fieldInfos = new ArrayList<>();
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
        fieldInfoList.add(FieldInfo.normal(psiParameter.getName(), psiType, new RequireAndRange(true, "N/A"), "desc", fieldInfos));
    }

    private void resolveAndFillFieldInfos(Project project, PsiType psiType, List<FieldInfo> fieldInfos) {
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

    public List<FieldInfo> listFieldInfos(PsiClass psiClass, Project project) {
        List<FieldInfo> fieldInfos = new ArrayList<>();
        for (PsiField psiField : psiClass.getAllFields()) {
            resolveFields(project, fieldInfos, psiField);
        }
        return fieldInfos;
    }

    private void resolveFields(Project project, List<FieldInfo> fieldInfos, PsiField psiField) {
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
        } else if (typeName.startsWith("List")) {
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

    private RequireAndRange getRequireAndRange(PsiAnnotation[] annotations) {
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

