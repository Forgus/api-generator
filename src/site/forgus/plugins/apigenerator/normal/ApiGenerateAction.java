package site.forgus.plugins.apigenerator.normal;

import com.google.gson.GsonBuilder;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiUtil;
import gherkin.deps.com.google.gson.Gson;
import gherkin.deps.com.google.gson.JsonObject;
import gherkin.lexer.Pa;

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
            dubboApiUpload(e, project);
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


    private void dubboApiUpload(AnActionEvent anActionEvent, Project project) throws IOException {
        BuildMdForDubbo buildMdForDubbo = new BuildMdForDubbo();
        List<FieldInfo> responseFieldInfos = buildMdForDubbo.generateResponseFieldInfos(anActionEvent);
        List<FieldInfo> requestFieldInfos = buildMdForDubbo.listRequestParamModel(anActionEvent);
        File file = new File("/Users/chenwenbin/Desktop/test.md");
        Writer md = new FileWriter(file);
        md.write("# 申请冲红\n");
        md.write("## 功能介绍\n");
        md.write("TODO\n");
        md.write("## Maven依赖\n");
        md.write("```xml\n");
        md.write("TODO\n");
        md.write("```\n");
        md.write("## Dubbo接口声明\n");
        md.write("```java\n");
        md.write("TODO\n");
        md.write("```\n");
        md.write("## 请求参数\n");
        md.write("### 请求参数示例\n");
        md.write("```json\n");
        String requestDemo = buildJson(requestFieldInfos);
        md.write(requestDemo +"\n");
        md.write("```\n");
        md.write("### 请求参数说明\n");
        md.write("名称|类型|必填|值域范围|描述/示例\n");
        md.write("--|--|--|--|--\n");
        for (FieldInfo fieldInfo : requestFieldInfos) {
            writeFieldInfo(md, fieldInfo);
        }
        md.write("## 返回结果\n");
        md.write("### 返回结果示例\n");
        md.write("```json\n");
        String responseDemo = buildJson(responseFieldInfos);
        md.write(responseDemo +"\n");
        md.write("```\n");
        md.write("### 返回结果说明\n");
        md.write("名称|类型|必填|值域范围|描述/示例\n");
        md.write("--|--|--|--|--\n");
        for (FieldInfo fieldInfo : responseFieldInfos) {
            writeFieldInfo(md, fieldInfo, "");
        }
        md.close();
    }

    private void writeFieldInfo(Writer writer, FieldInfo info) throws IOException {
        if (ParamTypeEnum.OBJECT.equals(info.getParamType())) {
            String str = "**" + info.getName() + "**" + "|Object|Y|N/A|N/A\n";
            writer.write(str);
            for (FieldInfo fieldInfo : info.getChildren()) {
                writeFieldInfo(writer, fieldInfo, CHILD_PREFIX);
            }
        } else if (ParamTypeEnum.ARRAY.equals(info.getParamType())){
            PsiType iterableType = PsiUtil.extractIterableTypeParameter(info.getPsiType(), false);
            String iterableTypePresentableText = iterableType.getPresentableText();
            String str = "**" + info.getName() + "**" + "|"+ getTypeInArray(info.getPsiType()) +"|Y|N/A|N/A\n";
            if(NormalTypes.isNormalType(iterableTypePresentableText)) {
                str = info.getName() + "|"+ iterableTypePresentableText +"[]|Y|N/A|N/A\n";
                writer.write(str);
            }else {
                writer.write(str);
                for (FieldInfo fieldInfo : info.getChildren()) {
                    writeFieldInfo(writer, fieldInfo, CHILD_PREFIX);
                }
            }
        }else {
            String str = info.getName() + "|" + info.getPsiType().getPresentableText() + "|Y|N/A|N/A\n";
            writer.write(str);
        }
    }

    private String getTypeInArray(PsiType psiType) {
        PsiType iterableType = PsiUtil.extractIterableTypeParameter(psiType, false);
        String iterableTypePresentableText = iterableType.getPresentableText();
        if(NormalTypes.isNormalType(iterableTypePresentableText)) {
            return iterableTypePresentableText + "[]";
        }
        return "Object[]";
    }

    private void writeFieldInfo(Writer writer, FieldInfo info, String prefix) throws IOException {
        if (info.isHasChildren()) {
            String str = "**" + info.getName() + "**" + "|" + getType(true,info.getPsiType()) + "|" + getRequireStr(info.isRequire()) + "|" + info.getRange() + "|" + info.getDesc() + "\n";
            writer.write(prefix + str);
            for (FieldInfo fieldInfo : info.getChildren()) {
                writeFieldInfo(writer, fieldInfo, CHILD_PREFIX + prefix);
            }
        } else {
            String str = info.getName() + "|" + getType(false,info.getPsiType()) + "|" + getRequireStr(info.isRequire()) + "|" + info.getRange() + "|" + info.getDesc() + "\n";
            writer.write(prefix + str);
        }
    }

    private String getType(boolean isParent, PsiType psiType) {
        String presentableText = psiType.getPresentableText();
        if(isParent) {
            if(presentableText.contains("<")) {
                return "Object[]";
            }
            return "Object";
        }
        if(presentableText.contains("<")) {
            PsiType iterableType = PsiUtil.extractIterableTypeParameter(psiType, false);
            String iterableTypePresentableText = iterableType.getPresentableText();
            return iterableTypePresentableText + "[]";
        }
        return presentableText;
    }

    private String getRequireStr(boolean isRequire) {
        return isRequire ? "Y" : "N";
    }

    private String buildJson(List<FieldInfo> fieldInfos) {
        Map<String,Object> map = new HashMap(32);
        for(FieldInfo fieldInfo : fieldInfos) {
            if(ParamTypeEnum.LITERAL.equals(fieldInfo.getParamType())) {
                map.put(fieldInfo.getName(),fieldInfo.getValue());
            }else if(ParamTypeEnum.ARRAY.equals(fieldInfo.getParamType())) {
                map.put(fieldInfo.getName(), Arrays.asList(buildObjectJson(fieldInfo.getChildren())));
            }else {
                map.put(fieldInfo.getName(),buildObjectJson(fieldInfo.getChildren()));
            }
        }
        return new GsonBuilder().setPrettyPrinting().create().toJson(map);
    }

    private Object buildObjectJson(List<FieldInfo> fieldInfos) {
        Map<String,Object> map = new HashMap(32);
        for(FieldInfo fieldInfo : fieldInfos) {
            if(ParamTypeEnum.LITERAL.equals(fieldInfo.getParamType())) {
                map.put(fieldInfo.getName(),fieldInfo.getValue());
            }else if(ParamTypeEnum.ARRAY.equals(fieldInfo.getParamType())) {
                map.put(fieldInfo.getName(), Arrays.asList(buildObjectJson(fieldInfo.getChildren())));
            }else {
                map.put(fieldInfo.getName(),buildObjectJson(fieldInfo.getChildren()));
            }
        }
        return map;
    }
}
