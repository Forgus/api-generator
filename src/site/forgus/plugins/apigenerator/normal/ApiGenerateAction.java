package site.forgus.plugins.apigenerator.normal;

import com.google.common.base.Strings;
import com.google.gson.GsonBuilder;
import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import org.apache.commons.collections.CollectionUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiGenerateAction extends AnAction {

    private static NotificationGroup notificationGroup;

    private static final String CHILD_PREFIX = "└";//空格：&emsp;

    static {
        notificationGroup = new NotificationGroup("Java2Json.NotificationGroup", NotificationDisplayType.BALLOON, true);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Editor editor = e.getDataContext().getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            return;
        }
        Project project = editor.getProject();
//        Notification notification = notificationGroup.createNotification("Hey,you should config first before generate api docs!", NotificationType.ERROR);
//        Notifications.Bus.notify(notification, project);
        try {
            generateMarkdownForDubboApi(e, project);
//            generateClassFieldInfo(e,project);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void update(AnActionEvent anActionEvent) {
        // Set the availability based on whether a project is open
        Project project = anActionEvent.getProject();
        anActionEvent.getPresentation().setEnabledAndVisible(project != null);
    }

    private void generateClassFieldInfo(AnActionEvent anActionEvent, Project project) throws IOException {
        BuildMdForDubbo buildMdForDubbo = new BuildMdForDubbo();
        List<FieldInfo> fieldInfos = buildMdForDubbo.listClassFieldInfos(anActionEvent);
        File file = new File("/Users/chenwenbin/Desktop/test.md");
        Writer md = new FileWriter(file);
        md.write("### 参数说明\n");
        if(CollectionUtils.isNotEmpty(fieldInfos)) {
            md.write("名称|类型|必填|值域范围|描述/示例\n");
            md.write("--|--|--|--|--\n");
            for (FieldInfo fieldInfo : fieldInfos) {
                writeFieldInfo(md, fieldInfo);
            }
        }
        md.close();
    }


    private void generateMarkdownForDubboApi(AnActionEvent actionEvent, Project project) throws IOException {
        BuildMdForDubbo buildMdForDubbo = new BuildMdForDubbo();
        Editor editor = actionEvent.getDataContext().getData(CommonDataKeys.EDITOR);
        String selectedText = actionEvent.getRequiredData(CommonDataKeys.EDITOR).getSelectionModel().getSelectedText();
        if (Strings.isNullOrEmpty(selectedText)) {
            Notification error = notificationGroup.createNotification("please select method or class", NotificationType.ERROR);
            Notifications.Bus.notify(error, project);
            return;
        }
        PsiFile psiFile = actionEvent.getDataContext().getData(CommonDataKeys.PSI_FILE);
        PsiElement referenceAt = psiFile.findElementAt(editor.getCaretModel().getOffset());
        PsiClass selectedClass = (PsiClass) PsiTreeUtil.getContextOfType(referenceAt, new Class[]{PsiClass.class});
        PsiMethod selectedMethod = getSelectedMethod(selectedClass,selectedText);
        if(selectedMethod == null) {
            //TODO
            return;
        }
        MethodInfo methodInfo = buildMdForDubbo.getMethod(project, selectedMethod);
        File file = new File("/Users/chenwenbin/Desktop/test.md");
        Writer md = new FileWriter(file);
        md.write("# 申请冲红\n");
        md.write("## 功能介绍\n");
        md.write(methodInfo.getDesc() + "\n");
        md.write("## Maven依赖\n");
        md.write("```xml\n");
        md.write("TODO\n");
        md.write("```\n");
        md.write("## Dubbo接口声明\n");
        md.write("```java\n");
        md.write(methodInfo.getPackageName()+"\n\n");
        md.write("public interface " + methodInfo.getClassName() +" {\n\n");
        md.write("\t"+ methodInfo.getReturnStr() + " " + methodInfo.getMethodName() + methodInfo.getParamStr() +";\n\n");
        md.write("}\n");
        md.write("```\n");
        md.write("## 请求参数\n");
        md.write("### 请求参数示例\n");
        if(CollectionUtils.isNotEmpty(methodInfo.getRequestFields())) {
            md.write("```json\n");
            String requestDemo = buildDemo(methodInfo.getRequestFields());
            md.write(requestDemo + "\n");
            md.write("```\n");
        }
        md.write("### 请求参数说明\n");
        if(CollectionUtils.isNotEmpty(methodInfo.getRequestFields())) {
            md.write("名称|类型|必填|值域范围|描述/示例\n");
            md.write("--|--|--|--|--\n");
            for (FieldInfo fieldInfo : methodInfo.getRequestFields()) {
                writeFieldInfo(md, fieldInfo);
            }
        }
        md.write("## 返回结果\n");
        md.write("### 返回结果示例\n");
        if(CollectionUtils.isNotEmpty(methodInfo.getResponseFields())) {
            md.write("```json\n");
            String responseDemo = buildDemo(methodInfo.getResponseFields());
            md.write(responseDemo + "\n");
            md.write("```\n");
        }
        md.write("### 返回结果说明\n");
        if(CollectionUtils.isNotEmpty(methodInfo.getResponseFields())) {
            md.write("名称|类型|必填|值域范围|描述/示例\n");
            md.write("--|--|--|--|--\n");
            for (FieldInfo fieldInfo : methodInfo.getResponseFields()) {
                writeFieldInfo(md, fieldInfo, "");
            }
        }
        md.close();
    }

    private PsiMethod getSelectedMethod(PsiClass selectedClass,String selectedText) {
        //寻找目标Method
        PsiMethod psiMethodTarget = null;
        for (PsiMethod psiMethod : selectedClass.getMethods()) {
            if (psiMethod.getName().equals(selectedText)) {
                psiMethodTarget = psiMethod;
                break;
            }
        }
        return psiMethodTarget;
    }

    private void writeFieldInfo(Writer writer, FieldInfo info) throws IOException {
        if (ParamTypeEnum.OBJECT.equals(info.getParamType())) {
            String str = "**" + info.getName() + "**" + "|Object|" + getRequireStr(info.isRequire()) + "|" + info.getRange() + "|" + info.getDesc() + "\n";
            writer.write(str);
            for (FieldInfo fieldInfo : info.getChildren()) {
                writeFieldInfo(writer, fieldInfo, CHILD_PREFIX);
            }
        } else if (ParamTypeEnum.ARRAY.equals(info.getParamType())) {
            PsiType iterableType = PsiUtil.extractIterableTypeParameter(info.getPsiType(), false);
            String iterableTypePresentableText = iterableType.getPresentableText();
            String str = "**" + info.getName() + "**" + "|" + getTypeInArray(info.getPsiType()) +"|"+ getRequireStr(info.isRequire()) + "|" + info.getRange() + "|" + info.getDesc() + "\n";
            if (NormalTypes.isNormalType(iterableTypePresentableText)) {
                str = info.getName() + "|" + iterableTypePresentableText + "[]|"+ getRequireStr(info.isRequire()) + "|" + info.getRange() + "|" + info.getDesc() + "\n";
                writer.write(str);
            } else {
                writer.write(str);
                for (FieldInfo fieldInfo : info.getChildren()) {
                    writeFieldInfo(writer, fieldInfo, CHILD_PREFIX);
                }
            }
        } else {
            String str = info.getName() + "|" + info.getPsiType().getPresentableText() + "|"+ getRequireStr(info.isRequire()) + "|" + info.getRange() + "|" + info.getDesc() + "\n";
            writer.write(str);
        }
    }

    private String getTypeInArray(PsiType psiType) {
        PsiType iterableType = PsiUtil.extractIterableTypeParameter(psiType, false);
        String iterableTypePresentableText = iterableType.getPresentableText();
        if (NormalTypes.isNormalType(iterableTypePresentableText)) {
            return iterableTypePresentableText + "[]";
        }
        return "Object[]";
    }

    private void writeFieldInfo(Writer writer, FieldInfo info, String prefix) throws IOException {
        if (info.isHasChildren()) {
            String str = "**" + info.getName() + "**" + "|" + getType(true, info.getPsiType()) + "|" + getRequireStr(info.isRequire()) + "|" + info.getRange() + "|" + info.getDesc() + "\n";
            writer.write(prefix + str);
            for (FieldInfo fieldInfo : info.getChildren()) {
                writeFieldInfo(writer, fieldInfo, CHILD_PREFIX + prefix);
            }
        } else {
            String str = info.getName() + "|" + getType(false, info.getPsiType()) + "|" + getRequireStr(info.isRequire()) + "|" + info.getRange() + "|" + info.getDesc() + "\n";
            writer.write(prefix + str);
        }
    }

    private String getType(boolean isParent, PsiType psiType) {
        String presentableText = psiType.getPresentableText();
        if (isParent) {
            if (presentableText.contains("<")) {
                return "Object[]";
            }
            return "Object";
        }
        if (presentableText.contains("<")) {
            PsiType iterableType = PsiUtil.extractIterableTypeParameter(psiType, false);
            String iterableTypePresentableText = iterableType.getPresentableText();
            return iterableTypePresentableText + "[]";
        }
        return presentableText;
    }

    private String getRequireStr(boolean isRequire) {
        return isRequire ? "Y" : "N";
    }

    private String buildDemo(List<FieldInfo> fieldInfos) {
        Map<String, Object> map = new HashMap(32);
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

    private Object buildObjectDemo(List<FieldInfo> fieldInfos) {
        Map<String, Object> map = new HashMap(32);
        for (FieldInfo fieldInfo : fieldInfos) {
            if (ParamTypeEnum.LITERAL.equals(fieldInfo.getParamType())) {
                map.put(fieldInfo.getName(), fieldInfo.getValue());
            } else if (ParamTypeEnum.ARRAY.equals(fieldInfo.getParamType())) {
                map.put(fieldInfo.getName(), Arrays.asList(buildObjectDemo(fieldInfo.getChildren())));
            } else {
                map.put(fieldInfo.getName(), buildObjectDemo(fieldInfo.getChildren()));
            }
        }
        return map;
    }
}
