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
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.xml.XmlFile;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.*;
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

        try {
            generateMarkdownForDubboApi(e);
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

    private void generateMarkdownForDubboApi(AnActionEvent actionEvent) throws IOException {
        Editor editor = actionEvent.getDataContext().getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            return;
        }
        Project project = editor.getProject();
        PsiFile pomFile = FilenameIndex.getFilesByName(project, "pom.xml", GlobalSearchScope.projectScope(project))[0];
        String pomPath = pomFile.getContainingDirectory().getVirtualFile().getPath() + "/pom.xml";
        Model pomModel = readPom(pomPath);
        String selectedText = actionEvent.getRequiredData(CommonDataKeys.EDITOR).getSelectionModel().getSelectedText();
        if (Strings.isNullOrEmpty(selectedText)) {
            Notification error = notificationGroup.createNotification("please select method or class", NotificationType.ERROR);
            Notifications.Bus.notify(error, project);
            return;
        }
        PsiFile psiFile = actionEvent.getDataContext().getData(CommonDataKeys.PSI_FILE);
        PsiElement referenceAt = psiFile.findElementAt(editor.getCaretModel().getOffset());
        PsiClass selectedClass = (PsiClass) PsiTreeUtil.getContextOfType(referenceAt, new Class[]{PsiClass.class});
        PsiMethod selectedMethod = getSelectedMethod(selectedClass, selectedText);
        if (selectedMethod == null) {
            //TODO
            return;
        }
        BuildMdForDubbo buildMdForDubbo = new BuildMdForDubbo();
        MethodInfo methodInfo = buildMdForDubbo.getMethod(project, selectedMethod);
        String dirPath = "/Users/chenwenbin/Desktop/api_docs";
        File dir = new File(dirPath);
        if(!dir.exists()) {
            dir.mkdir();
        }
        String fileName = getFileName(methodInfo);
        File apiDoc = new File(dirPath + "/" + fileName + ".md");
        if(!apiDoc.exists()) {
            apiDoc.createNewFile();
        }
        Writer md = new FileWriter(apiDoc);
        md.write("# " + fileName + "\n");
        md.write("## 功能介绍\n");
        md.write(methodInfo.getDesc() + "\n");
        md.write("## Maven依赖\n");
        md.write("```xml\n");
        md.write("<dependency>\n");
        md.write("\t<groupId>" + pomModel.getGroupId() + "</groupId>\n");
        md.write("\t<artifactId>" + pomModel.getGroupId() + "</artifactId>\n");
        md.write("\t<version>" + pomModel.getVersion() + "</version>\n");
        md.write("</dependency>\n");
        md.write("```\n");
        md.write("## Dubbo接口声明\n");
        md.write("```java\n");
        md.write(methodInfo.getPackageName() + "\n\n");
        md.write("public interface " + methodInfo.getClassName() + " {\n\n");
        md.write("\t" + methodInfo.getReturnStr() + " " + methodInfo.getMethodName() + methodInfo.getParamStr() + ";\n\n");
        md.write("}\n");
        md.write("```\n");
        md.write("## 请求参数\n");
        md.write("### 请求参数示例\n");
        if (CollectionUtils.isNotEmpty(methodInfo.getRequestFields())) {
            md.write("```json\n");
            String requestDemo = buildDemo(methodInfo.getRequestFields());
            md.write(requestDemo + "\n");
            md.write("```\n");
        }
        md.write("### 请求参数说明\n");
        if (CollectionUtils.isNotEmpty(methodInfo.getRequestFields())) {
            md.write("名称|类型|必填|值域范围|描述/示例\n");
            md.write("--|--|--|--|--\n");
            for (FieldInfo fieldInfo : methodInfo.getRequestFields()) {
                writeFieldInfo(md, fieldInfo);
            }
        }
        md.write("## 返回结果\n");
        md.write("### 返回结果示例\n");
        if (CollectionUtils.isNotEmpty(methodInfo.getResponseFields())) {
            md.write("```json\n");
            String responseDemo = buildDemo(methodInfo.getResponseFields());
            md.write(responseDemo + "\n");
            md.write("```\n");
        }
        md.write("### 返回结果说明\n");
        if (CollectionUtils.isNotEmpty(methodInfo.getResponseFields())) {
            md.write("名称|类型|必填|值域范围|描述/示例\n");
            md.write("--|--|--|--|--\n");
            for (FieldInfo fieldInfo : methodInfo.getResponseFields()) {
                writeFieldInfo(md, fieldInfo, "");
            }
        }
        md.close();
    }

    private String getFileName(MethodInfo methodInfo) {
        if(StringUtils.isEmpty(methodInfo.getDesc()) || !methodInfo.getDesc().contains(" ")) {
            return methodInfo.getMethodName();
        }
        return methodInfo.getDesc().split(" ")[0];
    }

    private PsiMethod getSelectedMethod(PsiClass selectedClass, String selectedText) {
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
            String str = "**" + info.getName() + "**" + "|" + getTypeInArray(info.getPsiType()) + "|" + getRequireStr(info.isRequire()) + "|" + info.getRange() + "|" + info.getDesc() + "\n";
            if (NormalTypes.isNormalType(iterableTypePresentableText)) {
                str = info.getName() + "|" + iterableTypePresentableText + "[]|" + getRequireStr(info.isRequire()) + "|" + info.getRange() + "|" + info.getDesc() + "\n";
                writer.write(str);
            } else {
                writer.write(str);
                for (FieldInfo fieldInfo : info.getChildren()) {
                    writeFieldInfo(writer, fieldInfo, CHILD_PREFIX);
                }
            }
        } else {
            String str = info.getName() + "|" + info.getPsiType().getPresentableText() + "|" + getRequireStr(info.isRequire()) + "|" + info.getRange() + "|" + info.getDesc() + "\n";
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

    public Model readPom(String pom){
        MavenXpp3Reader reader = new MavenXpp3Reader();
        try {
            return reader.read(new FileReader(pom));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }
        return  null;
    }
}
