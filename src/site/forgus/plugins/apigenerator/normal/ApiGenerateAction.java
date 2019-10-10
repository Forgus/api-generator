package site.forgus.plugins.apigenerator.normal;

import com.google.common.base.Strings;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import site.forgus.plugins.apigenerator.config.PersistentConfig;
import site.forgus.plugins.apigenerator.util.CollectionUtils;
import site.forgus.plugins.apigenerator.util.JsonUtil;
import site.forgus.plugins.apigenerator.util.MethodUtil;
import site.forgus.plugins.apigenerator.util.NotificationUtil;
import site.forgus.plugins.apigenerator.yapi.enums.RequestBodyTypeEnum;
import site.forgus.plugins.apigenerator.yapi.enums.RequestMethodEnum;
import site.forgus.plugins.apigenerator.yapi.enums.ResponseBodyTypeEnum;
import site.forgus.plugins.apigenerator.yapi.model.*;
import site.forgus.plugins.apigenerator.yapi.sdk.YApiSdk;

import java.io.*;
import java.util.*;

public class ApiGenerateAction extends AnAction {

    protected PersistentConfig config = PersistentConfig.getInstance();

    @Override
    public void actionPerformed(AnActionEvent actionEvent) {
        Editor editor = actionEvent.getDataContext().getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            return;
        }
        PsiFile psiFile = actionEvent.getData(CommonDataKeys.PSI_FILE);
        if (psiFile == null) {
            return;
        }
        Project project = editor.getProject();
        if (project == null) {
            return;
        }
        PsiElement referenceAt = psiFile.findElementAt(editor.getCaretModel().getOffset());
        PsiClass selectedClass = PsiTreeUtil.getContextOfType(referenceAt, PsiClass.class);
        if (selectedClass == null) {
            NotificationUtil.errorNotify("this operate only support in class file", project);
            return;
        }
        if (selectedClass.isInterface()) {
            generateMarkdownForInterface(project, referenceAt, selectedClass);
            return;
        }
        if (haveControllerAnnotation(selectedClass)) {
            uploadApiToYApi(project, referenceAt, selectedClass);
            return;
        }
        generateMarkdownForClass(project, selectedClass);
    }

    private void uploadApiToYApi(Project project, PsiElement referenceAt, PsiClass selectedClass) {
        PsiMethod selectedMethod = PsiTreeUtil.getContextOfType(referenceAt, PsiMethod.class);
        if (selectedMethod != null) {
            try {
                uploadSelectedMethodToYApi(project, selectedMethod);
            } catch (IOException e) {
                NotificationUtil.errorNotify(e.getMessage(), project);
            }
            return;
        }
        try {
            uploadHttpMethodsToYApi(project, selectedClass);
        } catch (IOException e) {
            NotificationUtil.errorNotify(e.getMessage(), project);
        }
    }

    private void uploadHttpMethodsToYApi(Project project, PsiClass psiClass) throws IOException {
        if (!haveControllerAnnotation(psiClass)) {
            NotificationUtil.warnNotify("Upload api failed, reason:\n not REST api.", project);
            return;
        }
        if (StringUtils.isEmpty(config.getState().yApiServerUrl)) {
            String serverUrl = Messages.showInputDialog("Input YApi Server Url", "YApi Server Url", Messages.getInformationIcon());
            if (StringUtils.isEmpty(serverUrl)) {
                NotificationUtil.warnNotify("YApi server url can not be empty.", project);
                return;
            }
            config.getState().yApiServerUrl = serverUrl;
        }
        if (StringUtils.isEmpty(config.getState().projectToken)) {
            String projectToken = Messages.showInputDialog("Input Project Token", "Project Token", Messages.getInformationIcon());
            if (StringUtils.isEmpty(projectToken)) {
                NotificationUtil.warnNotify("Project token can not be empty.", project);
                return;
            }
            config.getState().projectToken = projectToken;
        }
        if (StringUtils.isEmpty(config.getState().projectId)) {
            YApiProject projectInfo = YApiSdk.getProjectInfo(config.getState().yApiServerUrl, config.getState().projectToken);
            String projectId = projectInfo.get_id() == null ? Messages.showInputDialog("Input Project Id", "Project Id", Messages.getInformationIcon()) : projectInfo.get_id().toString();
            if (StringUtils.isEmpty(projectId)) {
                NotificationUtil.warnNotify("Project id can not be empty.", project);
                return;
            }
            config.getState().projectId = projectId;
        }
        PsiMethod[] methods = psiClass.getMethods();
        boolean uploadSuccess = false;
        for (PsiMethod method : methods) {
            if (hasMappingAnnotation(method)) {
                uploadToYApi(project, method);
                uploadSuccess = true;
            }
        }
        if (uploadSuccess) {
            NotificationUtil.infoNotify("Upload api success.", project);
            return;
        }
        NotificationUtil.infoNotify("Upload api failed, reason:\n not REST api.", project);
    }

    private void generateMarkdownForInterface(Project project, PsiElement referenceAt, PsiClass selectedClass) {
        PsiMethod selectedMethod = PsiTreeUtil.getContextOfType(referenceAt, PsiMethod.class);
        if (selectedMethod != null) {
            try {
                generateMarkdownForSelectedMethod(project, selectedMethod);
            } catch (IOException e) {
                NotificationUtil.errorNotify(e.getMessage(), project);
            }
            return;
        }
        try {
            generateMarkdownsForAllMethods(project, selectedClass);
        } catch (IOException e) {
            NotificationUtil.errorNotify(e.getMessage(), project);
        }
    }

    private void generateMarkdownForClass(Project project, PsiClass psiClass) {
        String dirPath = getDirPath(project);
        if (!mkDirectory(project, dirPath)) {
            return;
        }
        try {
            generateDocForClass(project, psiClass, dirPath);
        } catch (IOException e) {
            NotificationUtil.errorNotify(e.getMessage(), project);
        }
        NotificationUtil.infoNotify("generate api doc success.", project);
    }

    protected void generateMarkdownForSelectedMethod(Project project, PsiMethod selectedMethod) throws IOException {
        String dirPath = getDirPath(project);
        if (!mkDirectory(project, dirPath)) {
            return;
        }
        generateDocForMethod(project, selectedMethod, dirPath);
        NotificationUtil.infoNotify("generate api doc success.", project);
    }

    protected void generateMarkdownsForAllMethods(Project project, PsiClass selectedClass) throws IOException {
        String dirPath = getDirPath(project);
        if (!mkDirectory(project, dirPath)) {
            return;
        }
        for (PsiMethod psiMethod : selectedClass.getMethods()) {
            generateDocForMethod(project, psiMethod, dirPath);
        }
        NotificationUtil.infoNotify("generate api doc success.", project);
    }


    private void uploadSelectedMethodToYApi(Project project, PsiMethod method) throws IOException {
        if (!hasMappingAnnotation(method)) {
            NotificationUtil.warnNotify("Upload api failed, reason:\n not REST api.", project);
            return;
        }
        if (StringUtils.isEmpty(config.getState().yApiServerUrl)) {
            String serverUrl = Messages.showInputDialog("Input YApi Server Url", "YApi Server Url", Messages.getInformationIcon());
            if (StringUtils.isEmpty(serverUrl)) {
                NotificationUtil.warnNotify("YApi server url can not be empty.", project);
                return;
            }
            config.getState().yApiServerUrl = serverUrl;
        }
        if (StringUtils.isEmpty(config.getState().projectToken)) {
            String projectToken = Messages.showInputDialog("Input Project Token", "Project Token", Messages.getInformationIcon());
            if (StringUtils.isEmpty(projectToken)) {
                NotificationUtil.warnNotify("Project token can not be empty.", project);
                return;
            }
            config.getState().projectToken = projectToken;
        }
        if (StringUtils.isEmpty(config.getState().projectId)) {
            YApiProject projectInfo = YApiSdk.getProjectInfo(config.getState().yApiServerUrl, config.getState().projectToken);
            String projectId = projectInfo.get_id() == null ? Messages.showInputDialog("Input Project Id", "Project Id", Messages.getInformationIcon()) : projectInfo.get_id().toString();
            if (StringUtils.isEmpty(projectId)) {
                NotificationUtil.warnNotify("Project id can not be empty.", project);
                return;
            }
            config.getState().projectId = projectId;
        }
        uploadToYApi(project, method);
    }

    private void uploadToYApi(Project project, PsiMethod psiMethod) throws IOException {
        YApiInterface yApiInterface = buildYApiInterface(project, psiMethod);
        if (yApiInterface == null) {
            return;
        }
        YApiResponse yApiResponse = YApiSdk.saveInterface(config.getState().yApiServerUrl, yApiInterface);
        if (yApiResponse.getErrcode() != 0) {
            NotificationUtil.errorNotify("Upload api failed, cause:" + yApiResponse.getErrmsg(), project);
            return;
        }
        NotificationUtil.infoNotify("Upload api success.", project);
    }

    private YApiInterface buildYApiInterface(Project project, PsiMethod psiMethod) throws IOException {
        PsiClass containingClass = psiMethod.getContainingClass();
        if (containingClass == null) {
            return null;
        }
        PsiAnnotation controller = null;
        PsiAnnotation classRequestMapping = null;
        for (PsiAnnotation annotation : containingClass.getAnnotations()) {
            String text = annotation.getText();
            if (text.contains("Controller")) {
                controller = annotation;
            } else if (text.contains("RequestMapping")) {
                classRequestMapping = annotation;
            }
        }
        if (controller == null) {
            NotificationUtil.warnNotify("Invalid Class File!", project);
            return null;
        }
        MethodInfo methodInfo = MethodUtil.getMethodInfo(psiMethod);
        PsiAnnotation methodMapping = getMethodMapping(psiMethod);
        YApiInterface yApiInterface = new YApiInterface();
        yApiInterface.setToken(config.getState().projectToken);

        RequestMethodEnum requestMethodEnum = getMethodFromAnnotation(methodMapping);
        yApiInterface.setMethod(requestMethodEnum.name());
        if (methodInfo.getParamStr().contains("RequestBody")) {
            yApiInterface.setReq_body_type(RequestBodyTypeEnum.JSON.getValue());
            yApiInterface.setReq_body_other(JsonUtil.buildJson5(methodInfo.getRequestFields()));
        } else {
            if (yApiInterface.getMethod().equals("POST")) {
                yApiInterface.setReq_body_type("form");
                yApiInterface.setReq_body_form(listYApiForms(methodInfo.getRequestFields()));
            } else if (RequestMethodEnum.GET.equals(requestMethodEnum)) {
                yApiInterface.setReq_query(listYApiQueries(methodInfo.getRequestFields()));
            }
        }
        Map<String, YApiCat> catNameMap = getCatNameMap();
        PsiDocComment classDesc = containingClass.getDocComment();
        yApiInterface.setCatid(getCatId(catNameMap, classDesc));
        yApiInterface.setTitle(methodInfo.getDesc());
        yApiInterface.setPath(getPathFromAnnotation(classRequestMapping) + getPathFromAnnotation(methodMapping));
        if (containResponseBodyAnnotation(psiMethod.getAnnotations()) || controller.getText().contains("Rest")) {
            yApiInterface.setReq_headers(Collections.singletonList(YApiHeader.json()));
            yApiInterface.setRes_body(JsonUtil.buildJson5(methodInfo.getResponseFields()));
        } else {
            yApiInterface.setReq_headers(Collections.singletonList(new YApiHeader("Content-Type", "application/x-www-form-urlencoded")));
            yApiInterface.setRes_body_type(ResponseBodyTypeEnum.RAW.getValue());
            yApiInterface.setRes_body("");
        }
        yApiInterface.setReq_params(listYApiPathVariables(methodInfo.getRequestFields()));
        yApiInterface.setDesc(Objects.nonNull(yApiInterface.getDesc()) ? yApiInterface.getDesc() : "<pre><code data-language=\"java\" class=\"java\">" + getMethodDesc(psiMethod) + "</code> </pre>");
        return yApiInterface;
    }

    private boolean containResponseBodyAnnotation(PsiAnnotation[] annotations) {
        for (PsiAnnotation annotation : annotations) {
            if (annotation.getText().contains("ResponseBody")) {
                return true;
            }
        }
        return false;
    }

    private String getMethodDesc(PsiMethod psiMethod) {
        String methodDesc = psiMethod.getText().replace(Objects.nonNull(psiMethod.getBody()) ? psiMethod.getBody().getText() : "", "");
        if (!Strings.isNullOrEmpty(methodDesc)) {
            methodDesc = methodDesc.replace("<", "&lt;").replace(">", "&gt;");
        }
        return methodDesc;
    }

    private List<YApiPathVariable> listYApiPathVariables(List<FieldInfo> requestFields) {
        List<YApiPathVariable> yApiPathVariables = new ArrayList<>();
        for (FieldInfo fieldInfo : requestFields) {
            List<PsiAnnotation> annotations = fieldInfo.getAnnotations();
            PsiAnnotation pathVariable = findAnnotationByName(annotations, "PathVariable");
            if (pathVariable != null) {
                YApiPathVariable yApiPathVariable = new YApiPathVariable();
                PsiNameValuePair[] psiNameValuePairs = pathVariable.getParameterList().getAttributes();
                if (psiNameValuePairs.length > 0) {
                    for (PsiNameValuePair psiNameValuePair : psiNameValuePairs) {
                        String name = psiNameValuePair.getName();
                        String literalValue = psiNameValuePair.getLiteralValue();
                        if (StringUtils.isNotEmpty(literalValue)) {
                            if ("value".equals(name) || "name".equals(name)) {
                                yApiPathVariable.setName(literalValue);
                                break;
                            }
                        }
                    }
                } else {
                    yApiPathVariable.setName(fieldInfo.getName());
                }
                yApiPathVariable.setDesc(fieldInfo.getDesc());
                yApiPathVariable.setExample(fieldInfo.getValue().toString());
                yApiPathVariables.add(yApiPathVariable);
            }
        }
        return yApiPathVariables;
    }

    private PsiAnnotation findAnnotationByName(List<PsiAnnotation> annotations, String text) {
        if (annotations == null) {
            return null;
        }
        for (PsiAnnotation annotation : annotations) {
            if (annotation.getText().contains(text)) {
                return annotation;
            }
        }
        return null;
    }

    private String getPathFromAnnotation(PsiAnnotation annotation) {
        if (annotation == null) {
            return "";
        }
        PsiNameValuePair[] psiNameValuePairs = annotation.getParameterList().getAttributes();
        if (psiNameValuePairs.length == 1 && psiNameValuePairs[0].getName() == null) {
            return psiNameValuePairs[0].getLiteralValue();
        }
        if (psiNameValuePairs.length > 1) {
            for (PsiNameValuePair psiNameValuePair : psiNameValuePairs) {
                if (psiNameValuePair.getName().equals("value") || psiNameValuePair.getName().equals("path")) {
                    return psiNameValuePair.getLiteralValue();
                }
            }
        }
        return "";
    }

    private String getDefaultCatName() {
        String defaultCat = config.getState().defaultCat;
        return StringUtils.isEmpty(defaultCat) ? "api_generator" : defaultCat;
    }

    private String getClassCatName(PsiDocComment classDesc) {
        if (classDesc == null) {
            return "";
        }
        return DesUtil.getDescription(classDesc).split(" ")[0];
    }

    private String getCatId(Map<String, YApiCat> catNameMap, PsiDocComment classDesc) throws IOException {
        String defaultCatName = getDefaultCatName();
        String catName;
        if (config.getState().autoCat) {
            String classCatName = getClassCatName(classDesc);
            catName = StringUtils.isEmpty(classCatName) ? defaultCatName : classCatName;
        } else {
            catName = defaultCatName;
        }
        YApiCat apiCat = catNameMap.get(catName);
        if (apiCat != null) {
            return apiCat.get_id().toString();
        }
        YApiResponse<YApiCat> yApiResponse = YApiSdk.addCategory(config.getState().yApiServerUrl, config.getState().projectToken, config.getState().projectId, catName);
        return yApiResponse.getData().get_id().toString();
    }

    private Map<String, YApiCat> getCatNameMap() throws IOException {
        List<YApiCat> yApiCats = YApiSdk.listCategories(config.getState().yApiServerUrl, config.getState().projectToken);
        Map<String, YApiCat> catNameMap = new HashMap<>();
        for (YApiCat cat : yApiCats) {
            catNameMap.put(cat.getName(), cat);
        }
        return catNameMap;
    }

    private List<YApiQuery> listYApiQueries(List<FieldInfo> requestFields) {
        List<YApiQuery> queries = new ArrayList<>();
        for (FieldInfo fieldInfo : requestFields) {
            if (ParamTypeEnum.LITERAL.equals(fieldInfo.getParamType())) {
                queries.add(buildYApiQuery(fieldInfo));
            } else if (ParamTypeEnum.OBJECT.equals(fieldInfo.getParamType())) {
                List<FieldInfo> children = fieldInfo.getChildren();
                for (FieldInfo info : children) {
                    queries.add(buildYApiQuery(info));
                }
            } else {
                YApiQuery apiQuery = buildYApiQuery(fieldInfo);
                apiQuery.setExample("1,1,1");
                queries.add(apiQuery);
            }
        }
        return queries;
    }

    private YApiQuery buildYApiQuery(FieldInfo fieldInfo) {
        YApiQuery query = new YApiQuery();
        query.setName(fieldInfo.getName());
        query.setDesc(generateDesc(fieldInfo));
        if (fieldInfo.getValue() != null) {
            query.setExample(fieldInfo.getValue().toString());
        }
        query.setRequired(fieldInfo.isRequire() ? "1" : "0");
        return query;
    }

    private String generateDesc(FieldInfo fieldInfo) {
        if ("N/A".equals(fieldInfo.getRange())) {
            return fieldInfo.getDesc();
        }
        return fieldInfo.getDesc() + "，值范围：" + fieldInfo.getRange();
    }

    private List<YApiForm> listYApiForms(List<FieldInfo> requestFields) {
        List<YApiForm> yApiForms = new ArrayList<>();
        for (FieldInfo fieldInfo : requestFields) {
            if (fieldInfo.getAnnotations().size() == 0) {
                if (ParamTypeEnum.LITERAL.equals(fieldInfo.getParamType())) {
                    yApiForms.add(buildYApiForm(fieldInfo));
                } else if (ParamTypeEnum.OBJECT.equals(fieldInfo.getParamType())) {
                    List<FieldInfo> children = fieldInfo.getChildren();
                    for (FieldInfo info : children) {
                        yApiForms.add(buildYApiForm(info));
                    }
                } else {
                    YApiForm apiQuery = buildYApiForm(fieldInfo);
                    apiQuery.setExample("1,1,1");
                    yApiForms.add(apiQuery);
                }
            }
        }
        return yApiForms;
    }

    private YApiForm buildYApiForm(FieldInfo fieldInfo) {
        YApiForm param = new YApiForm();
        param.setName(fieldInfo.getName());
        param.setDesc(fieldInfo.getDesc());
        if (fieldInfo.getValue() != null) {
            param.setExample(fieldInfo.getValue().toString());
        }
        param.setRequired(Boolean.toString(fieldInfo.isRequire()));
        return param;
    }

    private RequestMethodEnum getMethodFromAnnotation(PsiAnnotation methodMapping) {
        String text = methodMapping.getText();
        if (text.contains("RequestMapping")) {
            return extractMethodFromAttribute(methodMapping);
        }
        return extractMethodFromMappingText(text);
    }

    private RequestMethodEnum extractMethodFromMappingText(String text) {
        if (text.contains("GetMapping")) {
            return RequestMethodEnum.GET;
        }
        if (text.contains("PutMapping")) {
            return RequestMethodEnum.PUT;
        }
        if (text.contains("DeleteMapping")) {
            return RequestMethodEnum.DELETE;
        }
        if (text.contains("PatchMapping")) {
            return RequestMethodEnum.PATCH;
        }
        return RequestMethodEnum.POST;
    }

    private RequestMethodEnum extractMethodFromAttribute(PsiAnnotation annotation) {
        PsiNameValuePair[] psiNameValuePairs = annotation.getParameterList().getAttributes();
        for (PsiNameValuePair psiNameValuePair : psiNameValuePairs) {
            if ("method".equals(psiNameValuePair.getName())) {
                return RequestMethodEnum.valueOf(psiNameValuePair.getValue().getReference().resolve().getText());
            }
        }
        return RequestMethodEnum.POST;
    }

    private PsiAnnotation getMethodMapping(PsiMethod psiMethod) {
        for (PsiAnnotation annotation : psiMethod.getAnnotations()) {
            String text = annotation.getText();
            if (text.contains("Mapping")) {
                return annotation;
            }
        }
        return null;
    }

    private boolean hasMappingAnnotation(PsiMethod method) {
        PsiAnnotation[] annotations = method.getAnnotations();
        for (PsiAnnotation annotation : annotations) {
            if (annotation.getText().contains("Mapping")) {
                return true;
            }
        }
        return false;
    }

    private boolean haveControllerAnnotation(PsiClass psiClass) {
        PsiAnnotation[] annotations = psiClass.getAnnotations();
        for (PsiAnnotation annotation : annotations) {
            if (annotation.getText().contains("Controller")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void update(AnActionEvent e) {
        //perform action if and only if EDITOR != null
        boolean enabled = e.getData(CommonDataKeys.EDITOR) != null;
        e.getPresentation().setEnabledAndVisible(enabled);
    }

    private String getDirPath(Project project) {
        String dirPath = config.getState().dirPath;
        if (StringUtils.isEmpty(dirPath)) {
            return project.getBasePath() + "/target/api_docs";
        }
        if (dirPath.endsWith("/")) {
            return dirPath.substring(0, dirPath.lastIndexOf("/"));
        }
        return dirPath;
    }

    private void generateDocForClass(Project project, PsiClass psiClass, String dirPath) throws IOException {
        if (!mkDirectory(project, dirPath)) {
            return;
        }
        File apiDoc = new File(dirPath + "/" + psiClass.getName() + ".md");
        if (!apiDoc.exists()) {
            apiDoc.createNewFile();
        }
        Writer md = new FileWriter(apiDoc);
        List<FieldInfo> fieldInfos = listFieldInfos(psiClass);
        md.write("## 示例\n");
        if (CollectionUtils.isNotEmpty(fieldInfos)) {
            md.write("```json\n");
            md.write(JsonUtil.buildPrettyJson(fieldInfos) + "\n");
            md.write("```\n");
        }
        md.write("## 参数说明\n");
        if (CollectionUtils.isNotEmpty(fieldInfos)) {
            md.write("名称|类型|必填|值域范围|描述/示例\n");
            md.write("--|--|--|--|--\n");
            for (FieldInfo fieldInfo : fieldInfos) {
                writeFieldInfo(md, fieldInfo);
            }
        }
        md.close();
    }

    public List<FieldInfo> listFieldInfos(PsiClass psiClass) {
        List<FieldInfo> fieldInfos = new ArrayList<>();
        for (PsiField psiField : psiClass.getAllFields()) {
            if(FieldInfo.excludeFieldNames.contains(psiField.getName())) {
                continue;
            }
            fieldInfos.add(new FieldInfo(psiField.getName(), psiField.getType(), DesUtil.getDescription(psiField.getDocComment()), psiField.getAnnotations()));
        }
        return fieldInfos;
    }

    protected void generateDocForMethod(Project project, PsiMethod selectedMethod, String dirPath) throws IOException {
        if (!mkDirectory(project, dirPath)) {
            return;
        }
        MethodInfo methodInfo = MethodUtil.getMethodInfo(selectedMethod);
        String fileName = getFileName(methodInfo);
        File apiDoc = new File(dirPath + "/" + fileName + ".md");
        if (!apiDoc.exists()) {
            apiDoc.createNewFile();
        }
        Model pomModel = getPomModel(project);
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
        md.write("## 接口声明\n");
        md.write("```java\n");
        md.write("package " + methodInfo.getPackageName() + ";\n\n");
        md.write("public interface " + methodInfo.getClassName() + " {\n\n");
        md.write("\t" + methodInfo.getReturnStr() + " " + methodInfo.getMethodName() + methodInfo.getParamStr() + ";\n\n");
        md.write("}\n");
        md.write("```\n");
        md.write("## 请求参数\n");
        md.write("### 请求参数示例\n");
        if (CollectionUtils.isNotEmpty(methodInfo.getRequestFields())) {
            md.write("```json\n");
            md.write(JsonUtil.buildPrettyJson(methodInfo.getRequestFields()) + "\n");
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
        md.write("\n## 返回结果\n");
        md.write("### 返回结果示例\n");
        if (CollectionUtils.isNotEmpty(methodInfo.getResponseFields())) {
            md.write("```json\n");
            md.write(JsonUtil.buildPrettyJson(methodInfo.getResponseFields()) + "\n");
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

    private boolean mkDirectory(Project project, String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            boolean success = dir.mkdirs();
            if (!success) {
                NotificationUtil.errorNotify("invalid directory path!", project);
                return false;
            }
        }
        return true;
    }

    private Model getPomModel(Project project) {
        PsiFile pomFile = FilenameIndex.getFilesByName(project, "pom.xml", GlobalSearchScope.projectScope(project))[0];
        String pomPath = pomFile.getContainingDirectory().getVirtualFile().getPath() + "/pom.xml";
        return readPom(pomPath);
    }

    private String getFileName(MethodInfo methodInfo) {
        if (!config.getState().cnFileName) {
            return methodInfo.getMethodName();
        }
        if (StringUtils.isEmpty(methodInfo.getDesc()) || !methodInfo.getDesc().contains(" ")) {
            return methodInfo.getMethodName();
        }
        return methodInfo.getDesc().split(" ")[0];
    }

    private void writeFieldInfo(Writer writer, FieldInfo info) throws IOException {
        if (ParamTypeEnum.OBJECT.equals(info.getParamType())) {
            String str = "**" + info.getName() + "**" + "|Object|" + getRequireStr(info.isRequire()) + "|" + info.getRange() + "|" + info.getDesc() + "\n";
            writer.write(str);
            for (FieldInfo fieldInfo : info.getChildren()) {
                writeFieldInfo(writer, fieldInfo, getPrefix());
            }
        } else if (ParamTypeEnum.ARRAY.equals(info.getParamType())) {
            String str = "**" + info.getName() + "**" + "|" + getTypeInArray(info.getPsiType()) + "|" + getRequireStr(info.isRequire()) + "|" + info.getRange() + "|" + info.getDesc() + "\n";
            String iterableTypePresentableText = getIterableTypePresentableText(info.getPsiType());
            if (NormalTypes.isNormalType(iterableTypePresentableText)) {
                str = info.getName() + "|" + iterableTypePresentableText + "[]|" + getRequireStr(info.isRequire()) + "|" + info.getRange() + "|" + info.getDesc() + "\n";
                writer.write(str);
            } else {
                writer.write(str);
                for (FieldInfo fieldInfo : info.getChildren()) {
                    writeFieldInfo(writer, fieldInfo, getPrefix());
                }
            }
        } else {
            String str = info.getName() + "|" + info.getPsiType().getPresentableText() + "|" + getRequireStr(info.isRequire()) + "|" + info.getRange() + "|" + info.getDesc() + "\n";
            writer.write(str);
        }
    }

    private String getIterableTypePresentableText(PsiType psiType) {
        PsiType iterableType = PsiUtil.extractIterableTypeParameter(psiType, false);
        return iterableType == null ? "" : iterableType.getPresentableText();
    }

    private String getTypeInArray(PsiType psiType) {
        String iterableTypePresentableText = getIterableTypePresentableText(psiType);
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
                writeFieldInfo(writer, fieldInfo, getPrefix() + prefix);
            }
        } else {
            String str = info.getName() + "|" + getType(false, info.getPsiType()) + "|" + getRequireStr(info.isRequire()) + "|" + info.getRange() + "|" + info.getDesc() + "\n";
            writer.write(prefix + str);
        }
    }

    private String getPrefix() {
        String prefix = config.getState().prefix;
        if (" ".equals(prefix)) {
            return "&emsp";
        }
        return prefix;
    }

    private String getType(boolean isParent, PsiType psiType) {
        String presentableText = psiType.getPresentableText();
        if (isParent) {
            if (presentableText.contains("<")) {
                return "Object[]";
            }
            return "Object";
        }
        if (presentableText.contains("List") || presentableText.contains("Set")) {
            PsiType iterableType = PsiUtil.extractIterableTypeParameter(psiType, false);
            String iterableTypePresentableText = iterableType == null ? "" : iterableType.getPresentableText();
            return iterableTypePresentableText + "[]";
        }
        if (presentableText.contains("Map")) {
            return "{}";
        }
        return presentableText;
    }

    private String getRequireStr(boolean isRequire) {
        return isRequire ? "Y" : "N";
    }

    public Model readPom(String pom) {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        try {
            return reader.read(new FileReader(pom));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
