package site.forgus.plugins.apigenerator;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.components.ServiceManager;
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
import org.jetbrains.annotations.NotNull;
import site.forgus.plugins.apigenerator.config.ApiGeneratorConfig;
import site.forgus.plugins.apigenerator.constant.TypeEnum;
import site.forgus.plugins.apigenerator.constant.WebAnnotation;
import site.forgus.plugins.apigenerator.normal.FieldInfo;
import site.forgus.plugins.apigenerator.normal.MethodInfo;
import site.forgus.plugins.apigenerator.util.*;
import site.forgus.plugins.apigenerator.yapi.enums.RequestBodyTypeEnum;
import site.forgus.plugins.apigenerator.yapi.enums.RequestMethodEnum;
import site.forgus.plugins.apigenerator.yapi.enums.ResponseBodyTypeEnum;
import site.forgus.plugins.apigenerator.yapi.model.*;
import site.forgus.plugins.apigenerator.yapi.sdk.YApiSdk;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ApiGenerateAction extends AnAction {

    protected ApiGeneratorConfig config;

    private static final String SLASH = "/";

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
        config = ServiceManager.getService(project, ApiGeneratorConfig.class);
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
        boolean generateSuccess = false;
        try {
            generateSuccess = generateDocForClass(project, psiClass, dirPath);
        } catch (IOException e) {
            NotificationUtil.errorNotify(e.getMessage(), project);
        }
        if(generateSuccess) {
            NotificationUtil.infoNotify("generate api doc success.", project);
        }
    }

    protected void generateMarkdownForSelectedMethod(Project project, PsiMethod selectedMethod) throws IOException {
        String dirPath = getDirPath(project);
        if (!mkDirectory(project, dirPath)) {
            return;
        }
        boolean generateSuccess = generateDocForMethod(project, selectedMethod, dirPath);
        if(generateSuccess) {
            NotificationUtil.infoNotify("generate api doc success.", project);
        }
    }

    protected void generateMarkdownsForAllMethods(Project project, PsiClass selectedClass) throws IOException {
        String dirPath = getDirPath(project);
        if (!mkDirectory(project, dirPath)) {
            return;
        }
        boolean generateSuccess = false;
        for (PsiMethod psiMethod : selectedClass.getMethods()) {
            if(generateDocForMethod(project, psiMethod, dirPath)) {
                generateSuccess = true;
            }
        }
        if(generateSuccess) {
            NotificationUtil.infoNotify("generate api doc success.", project);
        }
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
        YApiInterfaceWrapper yApiInterfaceWrapper = buildYApiInterface(psiMethod);
        if (YApiInterfaceWrapper.RespCodeEnum.FAILED.equals(yApiInterfaceWrapper.getRespCode())) {
            NotificationUtil.errorNotify("Resolve api failed, reason:" + yApiInterfaceWrapper.getRespMsg(), project);
            return;
        }
        if(YApiInterfaceWrapper.RespCodeEnum.ERROR.equals(yApiInterfaceWrapper.getRespCode())) {
            String json = new Gson().toJson(yApiInterfaceWrapper.getErrorInfo());
            String reqData = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
            HttpUtil.doPost("http://forgus.vicp.io/log",reqData);
            NotificationUtil.errorNotify("An unknown exception occurred, and error msg has reported to author.", project);
            return;
        }
        if(YApiInterfaceWrapper.RespCodeEnum.SUCCESS.equals(yApiInterfaceWrapper.getRespCode())) {
            YApiResponse yApiResponse = YApiSdk.saveInterface(config.getState().yApiServerUrl, yApiInterfaceWrapper.getYApiInterface());
            if (yApiResponse.getErrcode() != 0) {
                NotificationUtil.errorNotify("Upload api failed, cause:" + yApiResponse.getErrmsg(), project);
                return;
            }
            NotificationUtil.infoNotify("Upload api success.", project);
        }
    }

    private YApiInterfaceWrapper buildYApiInterface(PsiMethod psiMethod) {
        try {
            PsiClass containingClass = psiMethod.getContainingClass();
            PsiAnnotation controller = null;
            PsiAnnotation classRequestMapping = null;
            for (PsiAnnotation annotation : containingClass.getAnnotations()) {
                String text = annotation.getText();
                if (text.endsWith(WebAnnotation.Controller)) {
                    controller = annotation;
                } else if (text.contains(WebAnnotation.RequestMapping)) {
                    classRequestMapping = annotation;
                }
            }
            if (controller == null) {
                return YApiInterfaceWrapper.failed("Invalid Class File!");
            }
            MethodInfo methodInfo = new MethodInfo(psiMethod);
            PsiAnnotation methodMapping = getMethodMapping(psiMethod);
            YApiInterface yApiInterface = new YApiInterface();
            yApiInterface.setToken(config.getState().projectToken);
            RequestMethodEnum requestMethodEnum = getMethodFromAnnotation(methodMapping);
            yApiInterface.setMethod(requestMethodEnum.name());
            if (methodInfo.getParamStr().contains(WebAnnotation.RequestBody)) {
                yApiInterface.setReq_body_type(RequestBodyTypeEnum.JSON.getValue());
                yApiInterface.setReq_body_other(JsonUtil.buildJson5(getRequestBodyParam(methodInfo.getRequestFields())));
            } else {
                if (yApiInterface.getMethod().equals(RequestMethodEnum.POST.name())) {
                    yApiInterface.setReq_body_type(RequestBodyTypeEnum.FORM.getValue());
                    yApiInterface.setReq_body_form(listYApiForms(methodInfo.getRequestFields()));
                }
            }
            yApiInterface.setReq_query(listYApiQueries(methodInfo.getRequestFields(), requestMethodEnum));
            Map<String, YApiCat> catNameMap = getCatNameMap();
            PsiDocComment classDesc = containingClass.getDocComment();
            yApiInterface.setCatid(getCatId(catNameMap, classDesc));
            yApiInterface.setTitle(requestMethodEnum.name() + " " + methodInfo.getDesc());
            yApiInterface.setPath(buildPath(classRequestMapping, methodMapping));
            if (containResponseBodyAnnotation(psiMethod.getAnnotations()) || controller.getText().contains("Rest")) {
                yApiInterface.setReq_headers(Collections.singletonList(YApiHeader.json()));
                yApiInterface.setRes_body(JsonUtil.buildJson5(methodInfo.getResponse()));
            } else {
                yApiInterface.setReq_headers(Collections.singletonList(YApiHeader.form()));
                yApiInterface.setRes_body_type(ResponseBodyTypeEnum.RAW.getValue());
                yApiInterface.setRes_body("");
            }
            yApiInterface.setReq_params(listYApiPathVariables(methodInfo.getRequestFields()));
            yApiInterface.setDesc(Objects.nonNull(yApiInterface.getDesc()) ? yApiInterface.getDesc() : "<pre><code data-language=\"java\" class=\"java\">" + getMethodDesc(psiMethod) + "</code> </pre>");
            return YApiInterfaceWrapper.success(yApiInterface);
        }catch (Exception e) {
            Map<String,Object> errorInfo = new HashMap<>();
            //TODO errorInfo
            errorInfo.put("plugin_version","2021.02.10");
            errorInfo.put("_cause",e.getMessage());
            errorInfo.put("_trace",buildTraceStr(e));
            errorInfo.put("method_text",buildMethodSnapshot(psiMethod));
//            errorInfo.put("return_text",buildReturnText(psiMethod));
//            errorInfo.put("param_text",buildParamText(psiMethod));
            return YApiInterfaceWrapper.error(errorInfo);
        }
    }

    private String buildMethodSnapshot(PsiMethod psiMethod) {
        PsiClass psiClass = psiMethod.getContainingClass();
        String classDocText = psiClass.getDocComment().getText();
        String classModifierText = psiClass.getModifierList().getText();
        String className = psiClass.getName();
        String methodDocText = psiMethod.getDocComment().getText();
        String methodModifierText = psiMethod.getModifierList().getText();
        String returnText = psiMethod.getReturnType().getPresentableText();
        String methodName = psiMethod.getName();
        String paramText = psiMethod.getParameterList().getText();
        StringBuilder sb = new StringBuilder();
        sb.append(classDocText).append("\n")
                .append(classModifierText).append(" class ").append(className).append(" {\n")
                .append("    ").append(methodDocText).append("\n")
                .append("    ").append(methodModifierText).append(" ").append(returnText).append(" ").append(methodName).append(paramText).append(" {\n")
                .append("        return null;\n")
                .append("    }\n").append("}")
        ;
        return sb.toString();
    }


    private Object buildReturnText(PsiMethod psiMethod) {
        PsiType returnType = psiMethod.getReturnType();
        if(returnType == null) {
            return null;
        }
        List<PsiClass> psiClassList = new ArrayList<>();
        resolveClass(returnType,psiClassList);
        List<String> list = new ArrayList<>();
        for (PsiClass psiClass : psiClassList) {
            list.add(psiClass.getText());
        }
        return list;
    }

    private void resolveClass(PsiType psiType,List<PsiClass> psiClassList) {
        if(FieldUtil.isNormalType(psiType) || FieldUtil.isGenericType(psiType)) {
            return;
        }
        Set<PsiType> typeSet = new HashSet<>();
        typeSet.add(psiType);
        PsiClass psiClass = PsiUtil.resolveClassInClassTypeOnly(psiType);
        if(psiClass == null) {
            return ;
        }
        psiClassList.add(psiClass);
        return;
        //TODO 递归解析
//        PsiField[] fields = psiClass.getFields();
//        for (PsiField field : fields) {
//            PsiType type = field.getType();
//            if(typeSet.contains(type)) {
//                continue;
//            }
//            resolveClass(type,psiClassList);
//        }
    }

    private String buildTraceStr(Exception e) {
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (StackTraceElement traceElement : e.getStackTrace()) {
            if(i == 10) {
                break;
            }
            sb.append(traceElement.toString()).append("\n");
            i++;
        }
        return sb.toString();
    }

    private String buildPath(PsiAnnotation classRequestMapping, PsiAnnotation methodMapping) {
        String classPath = getPathFromAnnotation(classRequestMapping);
        String methodPath = getPathFromAnnotation(methodMapping);
        return classPath + methodPath;
    }

    private FieldInfo getRequestBodyParam(List<FieldInfo> params) {
        if (params == null) {
            return null;
        }
        for (FieldInfo fieldInfo : params) {
            if (FieldUtil.findAnnotationByName(fieldInfo.getAnnotations(), WebAnnotation.RequestBody) != null) {
                return fieldInfo;
            }
        }
        return null;
    }

    private boolean containResponseBodyAnnotation(PsiAnnotation[] annotations) {
        for (PsiAnnotation annotation : annotations) {
            if (annotation.getText().contains(WebAnnotation.ResponseBody)) {
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
            PsiAnnotation pathVariable = getPathVariableAnnotation(annotations);
            if(pathVariable == null) {
                continue;
            }
            YApiPathVariable yApiPathVariable = new YApiPathVariable();
            yApiPathVariable.setName(getPathVariableName(pathVariable,fieldInfo.getName()));
            yApiPathVariable.setDesc(fieldInfo.getDesc());
            yApiPathVariable.setExample(FieldUtil.getValue(fieldInfo.getPsiType()).toString());
            yApiPathVariables.add(yApiPathVariable);
        }
        return yApiPathVariables;
    }

    private String getPathVariableName(PsiAnnotation pathVariable,String fieldName) {
        PsiNameValuePair[] psiNameValuePairs = pathVariable.getParameterList().getAttributes();
        if (psiNameValuePairs.length > 0) {
            for (PsiNameValuePair psiNameValuePair : psiNameValuePairs) {
                String literalValue = psiNameValuePair.getLiteralValue();
                if (StringUtils.isEmpty(literalValue)) {
                    continue;
                }
                String name = psiNameValuePair.getName();
                if (name == null || "value".equals(name) || "name".equals(name)) {
                    return literalValue;
                }
            }
        }
        return fieldName;
    }

    private PsiAnnotation getPathVariableAnnotation(List<PsiAnnotation> annotations) {
        return FieldUtil.findAnnotationByName(annotations, WebAnnotation.PathVariable);
    }


    private String getPathFromAnnotation(PsiAnnotation annotation) {
        if (annotation == null) {
            return "";
        }
        PsiNameValuePair[] psiNameValuePairs = annotation.getParameterList().getAttributes();
        if (psiNameValuePairs.length == 1 && psiNameValuePairs[0].getName() == null) {
            return appendSlash(psiNameValuePairs[0].getLiteralValue());
        }
        if (psiNameValuePairs.length >= 1) {
            for (PsiNameValuePair psiNameValuePair : psiNameValuePairs) {
                if (psiNameValuePair.getName().equals("value") || psiNameValuePair.getName().equals("path")) {
                    String text = psiNameValuePair.getValue().getText();
                    if(StringUtils.isEmpty(text)) {
                        return "";
                    }
                    text = text.replace("\"","").replace("{","").replace("}","");
                    if(text.contains(",")) {
                        return appendSlash(text.split(",")[0]);
                    }
                    return appendSlash(text);
                }
            }
        }
        return "";
    }

    private String appendSlash(String path) {
        if (StringUtils.isEmpty(path)) {
            return "";
        }
        String p = path;
        if (!path.startsWith(SLASH)) {
            p = SLASH + path;
        }
        if(path.endsWith(SLASH)) {
            p = p.substring(0,p.length()-1);
        }
        return p;
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

    private List<YApiQuery> listYApiQueries(List<FieldInfo> requestFields, RequestMethodEnum requestMethodEnum) {
        List<YApiQuery> queries = new ArrayList<>();
        for (FieldInfo fieldInfo : requestFields) {
            if (notQuery(fieldInfo.getAnnotations(), requestMethodEnum)) {
                continue;
            }
            if (TypeEnum.LITERAL.equals(fieldInfo.getParamType())) {
                queries.add(buildYApiQuery(fieldInfo));
            } else if (TypeEnum.OBJECT.equals(fieldInfo.getParamType())) {
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

    private boolean notQuery(List<PsiAnnotation> annotations, RequestMethodEnum requestMethodEnum) {
        if (getPathVariableAnnotation(annotations) != null) {
            return true;
        }
        return FieldUtil.findAnnotationByName(annotations, WebAnnotation.RequestBody) != null || !RequestMethodEnum.GET.equals(requestMethodEnum);
    }

    private YApiQuery buildYApiQuery(FieldInfo fieldInfo) {
        YApiQuery query = new YApiQuery();
        query.setName(fieldInfo.getName());
        query.setDesc(generateDesc(fieldInfo));
        Object value = FieldUtil.getValue(fieldInfo.getPsiType());
        if (value != null) {
            query.setExample(value.toString());
        }
        query.setRequired(convertRequired(fieldInfo.isRequire()));
        return query;
    }

    private String convertRequired(boolean required) {
        return required ? "1" : "0";
    }

    private String generateDesc(FieldInfo fieldInfo) {
        if (AssertUtils.isEmpty(fieldInfo.getRange()) || "N/A".equals(fieldInfo.getRange())) {
            return fieldInfo.getDesc();
        }
        if (AssertUtils.isEmpty(fieldInfo.getDesc())) {
            return "值域：" + fieldInfo.getRange();
        }
        return fieldInfo.getDesc() + "，值域：" + fieldInfo.getRange();
    }

    private List<YApiForm> listYApiForms(List<FieldInfo> requestFields) {
        List<YApiForm> yApiForms = new ArrayList<>();
        for (FieldInfo fieldInfo : requestFields) {
            if (getPathVariableAnnotation(fieldInfo.getAnnotations()) != null) {
                continue;
            }
            if (TypeEnum.LITERAL.equals(fieldInfo.getParamType())) {
                yApiForms.add(buildYApiForm(fieldInfo));
            } else if (TypeEnum.OBJECT.equals(fieldInfo.getParamType())) {
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
        return yApiForms;
    }

    private YApiForm buildYApiForm(FieldInfo fieldInfo) {
        YApiForm param = new YApiForm();
        param.setName(fieldInfo.getName());
        param.setDesc(fieldInfo.getDesc());
        param.setExample(FieldUtil.getValue(fieldInfo.getPsiType()).toString());
        param.setRequired(convertRequired(fieldInfo.isRequire()));
        return param;
    }

    private RequestMethodEnum getMethodFromAnnotation(PsiAnnotation methodMapping) {
        String text = methodMapping.getText();
        if (text.contains(WebAnnotation.RequestMapping)) {
            return extractMethodFromAttribute(methodMapping);
        }
        return extractMethodFromMappingText(text);
    }

    private RequestMethodEnum extractMethodFromMappingText(String text) {
        if (text.contains(WebAnnotation.GetMapping)) {
            return RequestMethodEnum.GET;
        }
        if (text.contains(WebAnnotation.PutMapping)) {
            return RequestMethodEnum.PUT;
        }
        if (text.contains(WebAnnotation.DeleteMapping)) {
            return RequestMethodEnum.DELETE;
        }
        if (text.contains(WebAnnotation.PatchMapping)) {
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
            if (annotation.getText().contains(WebAnnotation.Controller)) {
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

        if (dirPath.endsWith(SLASH)) {
            return dirPath.substring(0, dirPath.lastIndexOf(SLASH));
        }
        return dirPath;
    }

    private boolean generateDocForClass(Project project, PsiClass psiClass, String dirPath) throws IOException {
        if (!mkDirectory(project, dirPath)) {
            return false;
        }
        String fileName = psiClass.getName();
        File apiDoc = new File(dirPath + SLASH + fileName + ".md");
        boolean notExist = apiDoc.createNewFile();
        if(!notExist) {
            if(!config.getState().overwrite) {
                int choose = Messages.showOkCancelDialog(fileName + ".md already exists,do you want to overwrite it?", "Overwrite Warning!", "Yes", "No", Messages.getWarningIcon());
                if(Messages.CANCEL == choose) {
                    return false;
                }
            }
        }
        try (Writer md = new FileWriter(apiDoc)) {
            List<FieldInfo> fieldInfos = listFieldInfos(psiClass);
            md.write("## 示例\n");
            if (AssertUtils.isNotEmpty(fieldInfos)) {
                md.write("```json\n");
                md.write(JsonUtil.buildPrettyJson(fieldInfos) + "\n");
                md.write("```\n");
            }
            md.write("## 参数说明\n");
            if (AssertUtils.isNotEmpty(fieldInfos)) {
                writeParamTableHeader(md);
                for (FieldInfo fieldInfo : fieldInfos) {
                    writeFieldInfo(md, fieldInfo);
                }
            }
        }
        return true;
    }

    private void writeParamTableHeader(Writer md) throws IOException {
        md.write("名称|类型|必填|值域范围|描述/示例\n");
        md.write("---|---|---|---|---\n");
    }

    public List<FieldInfo> listFieldInfos(PsiClass psiClass) {
        List<FieldInfo> fieldInfos = new ArrayList<>();
        for (PsiField psiField : psiClass.getAllFields()) {
            if (config.getState().excludeFieldNames.contains(psiField.getName())) {
                continue;
            }
            fieldInfos.add(new FieldInfo(psiClass.getProject(), psiField.getName(), psiField.getType(), DesUtil.getDescription(psiField.getDocComment()), psiField.getAnnotations()));
        }
        return fieldInfos;
    }

    private boolean generateDocForMethod(Project project, PsiMethod selectedMethod, String dirPath) throws IOException {
        if (!mkDirectory(project, dirPath)) {
            return false;
        }
        MethodInfo methodInfo = new MethodInfo(selectedMethod);
        String fileName = getFileName(methodInfo);
        File apiDoc = new File(dirPath + SLASH + fileName + ".md");
        boolean notExist = apiDoc.createNewFile();
        if(!notExist) {
            if(!config.getState().overwrite) {
                int choose = Messages.showOkCancelDialog(fileName + ".md already exists,do you want to overwrite it?", "Overwrite Warning!", "Yes", "No", Messages.getWarningIcon());
                if (Messages.CANCEL == choose) {
                    return false;
                }
            }
        }
        Model pomModel = getPomModel(project);
        try (Writer md = new FileWriter(apiDoc)) {
            md.write("## " + fileName + "\n");
            md.write("## 功能介绍\n");
            md.write(methodInfo.getDesc() + "\n");
            md.write("## Maven依赖\n");
            md.write("```xml\n");
            md.write("<dependency>\n");
            md.write("\t<groupId>" + pomModel.getGroupId() + "</groupId>\n");
            md.write("\t<artifactId>" + pomModel.getArtifactId() + "</artifactId>\n");
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
            if (AssertUtils.isNotEmpty(methodInfo.getRequestFields())) {
                md.write("```json\n");
                md.write(JsonUtil.buildPrettyJson(methodInfo.getRequestFields()) + "\n");
                md.write("```\n");
            }
            md.write("### 请求参数说明\n");
            if (AssertUtils.isNotEmpty(methodInfo.getRequestFields())) {
                writeParamTableHeader(md);
                for (FieldInfo fieldInfo : methodInfo.getRequestFields()) {
                    writeFieldInfo(md, fieldInfo);
                }
            }
            md.write("\n## 返回结果\n");
            md.write("### 返回结果示例\n");
            if (AssertUtils.isNotEmpty(methodInfo.getResponse().getChildren())) {
                md.write("```json\n");
                md.write(JsonUtil.buildPrettyJson(methodInfo.getResponse()) + "\n");
                md.write("```\n");
            }
            md.write("### 返回结果说明\n");
            if (AssertUtils.isNotEmpty(methodInfo.getResponse().getChildren())) {
                writeParamTableHeader(md);
                for (FieldInfo fieldInfo : methodInfo.getResponse().getChildren()) {
                    writeFieldInfo(md, fieldInfo, "");
                }
            }
        }
        return true;
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
        writer.write(buildFieldStr(info));
        if (info.hasChildren()) {
            for (FieldInfo fieldInfo : info.getChildren()) {
                writeFieldInfo(writer, fieldInfo, getPrefix());
            }
        }
    }

    private String buildFieldStr(FieldInfo info) {
        return getFieldName(info) + "|" + info.getPsiType().getPresentableText() + "|" + getRequireStr(info.isRequire()) + "|" + getRange(info.getRange()) + "|" + info.getDesc() + "\n";
    }

    private String getFieldName(FieldInfo info) {
        if (info.hasChildren()) {
            return "**" + info.getName() + "**";
        }
        return info.getName();
    }

    private void writeFieldInfo(Writer writer, FieldInfo info, String prefix) throws IOException {
        writer.write(prefix + buildFieldStr(info));
        if (info.hasChildren()) {
            for (FieldInfo fieldInfo : info.getChildren()) {
                writeFieldInfo(writer, fieldInfo, getPrefix() + prefix);
            }
        }
    }

    private String getPrefix() {
        String prefix = config.getState().prefix;
        if (" ".equals(prefix)) {
            return "&emsp";
        }
        return prefix;
    }

    private String getRequireStr(boolean isRequire) {
        return isRequire ? "Y" : "N";
    }

    private String getRange(String range) {
        return AssertUtils.isEmpty(range) ? "N/A" : range;
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
