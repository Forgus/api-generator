package site.forgus.plugins.apigenerator.yapi;

import com.google.common.base.Strings;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.search.GlobalSearchScope;
import demo.*;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import site.forgus.plugins.apigenerator.normal.DesUtil;
import site.forgus.plugins.apigenerator.normal.*;
import site.forgus.plugins.apigenerator.yapi.model.*;
import site.forgus.plugins.apigenerator.yapi.sdk.YApiSdk;
import site.forgus.plugins.apigenerator.util.JsonUtil;
import site.forgus.plugins.apigenerator.util.MethodUtil;

import java.io.IOException;
import java.util.*;

public class YApiGenerateAction extends ApiGenerateAction {

//    @Override
//    public void actionPerformed(AnActionEvent actionEvent) {
//        Editor editor = actionEvent.getDataContext().getData(CommonDataKeys.EDITOR);
//        if (editor == null) {
//            return;
//        }
//        PsiFile psiFile = actionEvent.getData(CommonDataKeys.PSI_FILE);
//        if (psiFile == null) {
//            return;
//        }
//        Project project = editor.getProject();
//        if (project == null) {
//            return;
//        }
//        PersistentConfig.State state = config.getState();
//        webApiUpload(actionEvent,project, state.token, state.projectId,state.yApiUrl,null,state.defaultCat);
//    }


    @Override
    protected void generateDocsWithClass(Project project, PsiClass selectedClass) throws IOException {
        PsiMethod[] methods = selectedClass.getMethods();
        for (PsiMethod method : methods) {
            if (hasMappingAnnotation(method)) {
                uploadToYApi(project, method);
            }
        }
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

    @Override
    protected void generateDocWithMethod(Project project, PsiMethod selectedMethod) throws IOException {
        if (!hasMappingAnnotation(selectedMethod)) {
            NotifyUtil.log(notificationGroup, project, "Upload api failed, reason:\n not http method!", NotificationType.ERROR);
            return;
        }
        uploadToYApi(project, selectedMethod);
    }

    private void uploadToYApi(Project project, PsiMethod psiMethod) throws IOException {
        YApiInterface yApiInterface = buildYApiInterface(project, psiMethod);
        if (yApiInterface == null) {
            return;
        }
        YApiResponse yApiResponse = YApiSdk.saveInterface(yApiInterface);
        if (yApiResponse.getErrcode() != 0) {
            NotifyUtil.log(notificationGroup, project, "Upload api failed, cause:" + yApiResponse.getErrmsg(), NotificationType.ERROR);
            return;
        }
        NotifyUtil.log(notificationGroup, project, "Upload api success.", NotificationType.INFORMATION);
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
            NotifyUtil.log(notificationGroup, project, "Invalid Class File!", NotificationType.INFORMATION);
            return null;
        }
        MethodInfo methodInfo = MethodUtil.getMethodInfo(project, psiMethod);
        PsiAnnotation methodMapping = getMethodMapping(psiMethod);
        YApiInterface yApiInterface = new YApiInterface();
        yApiInterface.setToken(config.getState().token);

        yApiInterface.setMethod(getMethodFromAnnotation(methodMapping));
        if(methodInfo.getParamStr().contains("RequestBody")) {
            yApiInterface.setReq_headers(Collections.singletonList(new YApiHeader("Content-Type","application/json")));
            yApiInterface.setReq_body_type("json");
            yApiInterface.setReq_body_other(JsonUtil.buildJson5(methodInfo.getRequestFields()));
        }else {
            yApiInterface.setReq_headers(Collections.singletonList(new YApiHeader("Content-Type","application/x-www-form-urlencoded")));
            if(yApiInterface.getMethod().equals("POST")) {
                yApiInterface.setReq_body_type("form");
                yApiInterface.setReq_body_form(listYApiForms(methodInfo.getRequestFields()));
            }else if("GET".equals(yApiInterface.getMethod())) {
                yApiInterface.setReq_query(listYApiQueries(methodInfo.getRequestFields()));
            }
        }
        Map<String, YApiCat> catNameMap = getCatNameMap();
        PsiDocComment classDesc = containingClass.getDocComment();
        yApiInterface.setCatid(getCatId(catNameMap, classDesc));
        yApiInterface.setTitle(methodInfo.getDesc());
        yApiInterface.setPath(getPathFromAnnotation(classRequestMapping) + getPathFromAnnotation(methodMapping));
        yApiInterface.setRes_body(JsonUtil.buildJson5(methodInfo.getResponseFields()));
        yApiInterface.setReq_params(listYApiPathVariables(methodInfo.getRequestFields()));
        yApiInterface.setDesc(Objects.nonNull(yApiInterface.getDesc()) ? yApiInterface.getDesc() : "<pre><code data-language=\"java\" class=\"java\">" + getMethodDesc(psiMethod) + "</code> </pre>");
        return yApiInterface;
    }

    private String getCatId(Map<String, YApiCat> catNameMap, PsiDocComment classDesc) throws IOException {
        String catId = null;
        String catName = config.getState().defaultCat;
        if (classDesc != null) {
            catName = DesUtil.getDescription(classDesc).split(" ")[0];
        }
        YApiCat apiCat = catNameMap.get(catName);
        if (apiCat != null) {
            catId = apiCat.get_id().toString();
        }
        if (catId == null) {
            YApiResponse<YApiCat> yApiResponse = YApiSdk.addCategory(config.getState().token, config.getState().projectId, catName);
            catId = yApiResponse.getData().get_id().toString();
        }
        return catId;
    }

    private List<YApiForm> listYApiForms(List<FieldInfo> requestFields) {
        List<YApiForm> yApiForms = new ArrayList<>();
        for (FieldInfo fieldInfo : requestFields) {
            if (fieldInfo.getAnnotations().size() == 0) {
                if(ParamTypeEnum.LITERAL.equals(fieldInfo.getParamType())) {
                    yApiForms.add(buildYApiForm(fieldInfo));
                }else if(ParamTypeEnum.OBJECT.equals(fieldInfo.getParamType())) {
                    List<FieldInfo> children = fieldInfo.getChildren();
                    for(FieldInfo info : children) {
                        yApiForms.add(buildYApiForm(info));
                    }
                }else {
                    YApiForm apiQuery = buildYApiForm(fieldInfo);
                    apiQuery.setExample("1,1,1");
                    yApiForms.add(apiQuery);
                }
            }
        }
        return yApiForms;
    }

    private List<YApiHeader> listRequestHeaders(String paramStr) {
        List<YApiHeader> headers = new ArrayList<>();
        if(paramStr.contains("RequestBody")) {
            headers.add(new YApiHeader("Content-Type","application/json"));
        }else {
            headers.add(new YApiHeader("Content-Type","application/x-www-form-urlencoded"));
        }
        return headers;
    }

    private List<YApiQuery> listYApiQueries(List<FieldInfo> requestFields) {
        List<YApiQuery> queries = new ArrayList<>();
        for (FieldInfo fieldInfo : requestFields) {
            if (fieldInfo.getAnnotations().size() == 0) {
                if(ParamTypeEnum.LITERAL.equals(fieldInfo.getParamType())) {
                    queries.add(buildYApiQuery(fieldInfo));
                }else if(ParamTypeEnum.OBJECT.equals(fieldInfo.getParamType())) {
                    List<FieldInfo> children = fieldInfo.getChildren();
                    for(FieldInfo info : children) {
                        queries.add(buildYApiQuery(info));
                    }
                }else {
                    YApiQuery apiQuery = buildYApiQuery(fieldInfo);
                    apiQuery.setExample("1,1,1");
                    queries.add(apiQuery);
                }
            }
        }
        return queries;
    }


    private YApiQuery buildYApiQuery(FieldInfo fieldInfo) {
        YApiQuery query = new YApiQuery();
        query.setName(fieldInfo.getName());
        query.setDesc(fieldInfo.getDesc());
        if(fieldInfo.getValue() != null) {
            query.setExample(fieldInfo.getValue().toString());
        }
        query.setRequired(Boolean.toString(fieldInfo.isRequire()));
        return query;
    }

    private YApiForm buildYApiForm(FieldInfo fieldInfo) {
        YApiForm param = new YApiForm();
        param.setName(fieldInfo.getName());
        param.setDesc(fieldInfo.getDesc());
        if(fieldInfo.getValue() != null) {
            param.setExample(fieldInfo.getValue().toString());
        }
        param.setRequired(Boolean.toString(fieldInfo.isRequire()));
        return param;
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
        for (PsiAnnotation annotation : annotations) {
            if (annotation.getText().contains(text)) {
                return annotation;
            }
        }
        return null;
    }

    private List<PsiAnnotation> listPathVariableAnnotations(PsiMethod psiMethod) {
        List<PsiAnnotation> annotations = new ArrayList<>();
        for (PsiParameter parameter : psiMethod.getParameterList().getParameters()) {
            for (PsiAnnotation psiAnnotation : parameter.getAnnotations()) {
                if (psiAnnotation.getText().contains("PathVariable")) {
                    annotations.add(psiAnnotation);
                }
            }
        }
        return annotations;
    }

    private String getPathFromAnnotation(PsiAnnotation methodMapping, PsiAnnotation pathVariable) {
        //TODO
        return "0";
    }

    private PsiAnnotation getMethodMapping(PsiMethod psiMethod) {
        PsiAnnotation methodMapping = null;
        for (PsiAnnotation annotation : psiMethod.getAnnotations()) {
            String text = annotation.getText();
            if (text.contains("Mapping")) {
                methodMapping = annotation;
            }
        }
        return methodMapping;
    }

    @NotNull
    private String getMethodDesc(PsiMethod psiMethod) {
        String methodDesc = psiMethod.getText().replace(Objects.nonNull(psiMethod.getBody()) ? psiMethod.getBody().getText() : "", "");
        if (!Strings.isNullOrEmpty(methodDesc)) {
            methodDesc = methodDesc.replace("<", "&lt;").replace(">", "&gt;");
        }
        return methodDesc;
    }

    private String getMethodFromAnnotation(PsiAnnotation methodMapping) {
        String text = methodMapping.getText();
        if (text.contains("RequestMapping")) {
            return extractMethodFromAttribute(methodMapping);
        }
        return extractMethodFromMappingText(text);
    }

    private String extractMethodFromAttribute(PsiAnnotation annotation) {
        PsiNameValuePair[] psiNameValuePairs = annotation.getParameterList().getAttributes();
        for (PsiNameValuePair psiNameValuePair : psiNameValuePairs) {
            if ("method".equals(psiNameValuePair.getName())) {
                return psiNameValuePair.getValue().getReference().resolve().getText();
            }
        }
        return "POST";
    }

    private String extractMethodFromMappingText(String text) {
        if (text.contains("GetMapping")) {
            return "GET";
        }
        if (text.contains("PutMapping")) {
            return "PUT";
        }
        if (text.contains("DeleteMapping")) {
            return "DELETE";
        }
        if (text.contains("PatchMapping")) {
            return "PATCH";
        }
        return "POST";
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

    private Map<String, YApiCat> getCatNameMap() throws IOException {
        List<YApiCat> yApiCats = YApiSdk.listCategories(config.getState().token);
        Map<String, YApiCat> catNameMap = new HashMap<>();
        for (YApiCat cat : yApiCats) {
            catNameMap.put(cat.getName(), cat);
        }
        return catNameMap;
    }

    public static void getRequest(Project project, YApiInterface yApiInterface, PsiMethod psiMethodTarget, List<FieldInfo> requestFields) {
        PsiParameter[] psiParameters = psiMethodTarget.getParameterList().getParameters();
        if (psiParameters.length > 0) {
            List<YApiQuery> list = new ArrayList<>();
            List<YApiHeader> yapiHeaderDTOList = new ArrayList<>();
            List<YApiPathVariable> yapiPathVariableDTOList = new ArrayList<>();
            for (PsiParameter psiParameter : psiParameters) {
                if (JavaConstant.HttpServletRequest.equals(psiParameter.getType().getCanonicalText()) || JavaConstant.HttpServletResponse.equals(psiParameter.getType().getCanonicalText())) {
                    continue;
                }
                PsiAnnotation psiAnnotation = PsiAnnotationSearchUtil.findAnnotation(psiParameter, SpringMVCConstant.RequestBody);
                if (psiAnnotation != null) {
                    yApiInterface.setReq_body_other(JsonUtil.buildJson5(requestFields));
                } else {
                    psiAnnotation = PsiAnnotationSearchUtil.findAnnotation(psiParameter, SpringMVCConstant.RequestParam);
                    YApiHeader yapiHeaderDTO = null;
                    YApiPathVariable yapiPathVariableDTO = null;
                    if (psiAnnotation == null) {
                        psiAnnotation = PsiAnnotationSearchUtil.findAnnotation(psiParameter, SpringMVCConstant.RequestAttribute);
                        if (psiAnnotation == null) {
                            psiAnnotation = PsiAnnotationSearchUtil.findAnnotation(psiParameter, SpringMVCConstant.RequestHeader);
                            if (psiAnnotation == null) {
                                psiAnnotation = PsiAnnotationSearchUtil.findAnnotation(psiParameter, SpringMVCConstant.PathVariable);
                                yapiPathVariableDTO = new YApiPathVariable();
                            } else {
                                yapiHeaderDTO = new YApiHeader();
                            }
                        }
                    }
                    if (psiAnnotation != null) {
                        PsiNameValuePair[] psiNameValuePairs = psiAnnotation.getParameterList().getAttributes();
                        YApiQuery yApiQuery = new YApiQuery();

                        if (psiNameValuePairs.length > 0) {
                            for (PsiNameValuePair psiNameValuePair : psiNameValuePairs) {
                                if ("name".equals(psiNameValuePair.getName()) || "value".equals(psiNameValuePair.getName())) {
                                    if (yapiHeaderDTO != null) {
                                        yapiHeaderDTO.setName(psiNameValuePair.getValue().getText().replace("\"", ""));
                                    } else if (yapiPathVariableDTO != null) {
                                        yapiPathVariableDTO.setName(psiNameValuePair.getValue().getText().replace("\"", ""));
                                    } else {
                                        yApiQuery.setName(psiNameValuePair.getValue().getText().replace("\"", ""));
                                    }
                                } else if ("required".equals(psiNameValuePair.getName())) {
                                    yApiQuery.setName(psiParameter.getName());
                                    yApiQuery.setRequired(psiNameValuePair.getValue().getText().replace("\"", "").replace("false", "0").replace("true", "1"));
                                } else if ("defaultValue".equals(psiNameValuePair.getName())) {
                                    yApiQuery.setExample(psiNameValuePair.getValue().getText().replace("\"", ""));
                                } else {
                                    if (yapiHeaderDTO != null) {
                                        yapiHeaderDTO.setName(StringUtils.isNotBlank(psiNameValuePair.getLiteralValue()) ? psiNameValuePair.getLiteralValue() : psiParameter.getName());
                                    }
                                    if (yapiPathVariableDTO != null) {
                                        yapiPathVariableDTO.setName(StringUtils.isNotBlank(psiNameValuePair.getLiteralValue()) ? psiNameValuePair.getLiteralValue() : psiParameter.getName());
                                        // 通过方法注释获得 描述 加上 类型
                                        yapiPathVariableDTO.setDesc(DesUtil.getParamDesc(psiMethodTarget, psiParameter.getName()) + "(" + psiParameter.getType().getPresentableText() + ")");
                                    } else {
                                        yApiQuery.setName(StringUtils.isNotBlank(psiNameValuePair.getLiteralValue()) ? psiNameValuePair.getLiteralValue() : psiParameter.getName());
                                        // 通过方法注释获得 描述 加上 类型
                                        yApiQuery.setDesc(DesUtil.getParamDesc(psiMethodTarget, psiParameter.getName()) + "(" + psiParameter.getType().getPresentableText() + ")");
                                    }
                                    if (NormalTypes.normalTypes.containsKey(psiParameter.getType().getPresentableText())) {
                                        if (yapiHeaderDTO != null) {
                                        } else if (yapiPathVariableDTO != null) {
                                            yapiPathVariableDTO.setExample(NormalTypes.normalTypes.get(psiParameter.getType().getPresentableText()).toString());
                                        } else {
                                            yApiQuery.setExample(NormalTypes.normalTypes.get(psiParameter.getType().getPresentableText()).toString());
                                        }
                                    } else {
                                        yApiInterface.setReq_body_other(JsonUtil.buildJson5(requestFields));
                                    }

                                }
                            }
                        } else {
                            if (yapiHeaderDTO != null) {
                                yapiHeaderDTO.setName(psiParameter.getName());
                            } else if (yapiPathVariableDTO != null) {
                                yapiPathVariableDTO.setName(psiParameter.getName());
                                // 通过方法注释获得 描述 加上 类型
                                yapiPathVariableDTO.setDesc(DesUtil.getParamDesc(psiMethodTarget, psiParameter.getName()) + "(" + psiParameter.getType().getPresentableText() + ")");
                            } else {
                                yApiQuery.setName(psiParameter.getName());
                                // 通过方法注释获得 描述 加上 类型
                                yApiQuery.setDesc(DesUtil.getParamDesc(psiMethodTarget, psiParameter.getName()) + "(" + psiParameter.getType().getPresentableText() + ")");
                            }
                            if (NormalTypes.normalTypes.containsKey(psiParameter.getType().getPresentableText())) {
                                if (yapiHeaderDTO != null) {
                                } else if (yapiPathVariableDTO != null) {
                                    yapiPathVariableDTO.setExample(NormalTypes.normalTypes.get(psiParameter.getType().getPresentableText()).toString());
                                } else {
                                    yApiQuery.setExample(NormalTypes.normalTypes.get(psiParameter.getType().getPresentableText()).toString());
                                }
                            } else {
                                yApiInterface.setReq_body_other(JsonUtil.buildJson5(requestFields));
                            }
                        }
                        if (yapiHeaderDTO != null) {
                            yapiHeaderDTOList.add(yapiHeaderDTO);
                        } else if (yapiPathVariableDTO != null) {
                            if (Strings.isNullOrEmpty(yapiPathVariableDTO.getDesc())) {
                                // 通过方法注释获得 描述  加上 类型
                                yapiPathVariableDTO.setDesc(DesUtil.getParamDesc(psiMethodTarget, psiParameter.getName()) + "(" + psiParameter.getType().getPresentableText() + ")");
                            }
                            if (Strings.isNullOrEmpty(yapiPathVariableDTO.getExample()) && NormalTypes.normalTypes.containsKey(psiParameter.getType().getPresentableText())) {
                                yapiPathVariableDTO.setExample(NormalTypes.normalTypes.get(psiParameter.getType().getPresentableText()).toString());
                            }
                            yapiPathVariableDTOList.add(yapiPathVariableDTO);
                        } else {
                            if (Strings.isNullOrEmpty(yApiQuery.getDesc())) {
                                // 通过方法注释获得 描述 加上 类型
                                yApiQuery.setDesc(DesUtil.getParamDesc(psiMethodTarget, psiParameter.getName()) + "(" + psiParameter.getType().getPresentableText() + ")");
                            }
                            if (Strings.isNullOrEmpty(yApiQuery.getExample()) && NormalTypes.normalTypes.containsKey(psiParameter.getType().getPresentableText())) {
                                yApiQuery.setExample(NormalTypes.normalTypes.get(psiParameter.getType().getPresentableText()).toString());
                            }
                            list.add(yApiQuery);
                        }
                    } else {
                        // 支持实体对象接收
                        yApiInterface.setReq_body_type("form");
                        if (yApiInterface.getReq_body_form() != null) {
                            yApiInterface.getReq_body_form().addAll(getRequestForm(project, psiParameter, psiMethodTarget));
                        } else {
                            yApiInterface.setReq_body_form(getRequestForm(project, psiParameter, psiMethodTarget));
                        }
                    }
                }
            }
            yApiInterface.setReq_query(list);
            yApiInterface.setReq_headers(yapiHeaderDTOList);
            yApiInterface.setReq_params(yapiPathVariableDTOList);
        }
    }

    public static List<YApiForm> getRequestForm(Project project, PsiParameter psiParameter, PsiMethod psiMethodTarget) {
        List<YApiForm> requestForm = new ArrayList<>();
        if (NormalTypes.normalTypes.containsKey(psiParameter.getType().getPresentableText())) {
            YApiForm yApiForm = new YApiForm();
            yApiForm.setName(psiParameter.getName());
            yApiForm.setType("text");
            String remark = DesUtil.getParamDesc(psiMethodTarget, psiParameter.getName()) + "(" + psiParameter.getType().getPresentableText() + ")";
            yApiForm.setDesc(remark);
            yApiForm.setExample(NormalTypes.normalTypes.get(psiParameter.getType().getPresentableText()).toString());
            requestForm.add(yApiForm);
        } else {
            PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(psiParameter.getType().getCanonicalText(), GlobalSearchScope.allScope(project));
            for (PsiField field : psiClass.getAllFields()) {
                if (field.getModifierList().hasModifierProperty("final")) {
                    continue;
                }
                YApiForm yApiForm = new YApiForm();
                yApiForm.setName(field.getName());
                yApiForm.setType("text");
                String remark = DesUtil.getFiledDesc(field.getDocComment());
                remark = DesUtil.getLinkRemark(remark, project, field);
                yApiForm.setDesc(remark);
                if (Objects.nonNull(field.getType().getPresentableText())) {
                    Object obj = NormalTypes.normalTypes.get(field.getType().getPresentableText());
                    if (Objects.nonNull(obj)) {
                        yApiForm.setExample(NormalTypes.normalTypes.get(field.getType().getPresentableText()).toString());
                    }
                }
                requestForm.add(yApiForm);
            }
        }
        return requestForm;
    }

    private String switchRequestMethod(String requestMethod) {
        if (requestMethod.contains("GET")) {
            return "GET";
        } else if (requestMethod.contains("POST")) {
            return "POST";
        } else if (requestMethod.contains("PUT")) {
            return "PUT";
        } else if (requestMethod.contains("DELETE")) {
            return "DELETE";
        } else if (requestMethod.contains("PATCH")) {
            return "PATCH";
        } else {
            return "";
        }
    }


    private void webApiUpload(AnActionEvent anActionEvent, Project project, String projectToken, String projectId, String yApiUrl, String attachUpload, String menu) {
        //获得api 需上传的接口列表 参数对象
        ArrayList<YapiApiDTO> yApiApiDTOS = new BuildJsonForYApi().actionPerformedList(anActionEvent, attachUpload);
        if (Objects.nonNull(yApiApiDTOS)) {
            for (YapiApiDTO yapiApiDTO : yApiApiDTOS) {
                YApiSaveParam yapiSaveParam = new YApiSaveParam(projectToken, yapiApiDTO.getTitle(), yapiApiDTO.getPath(), yapiApiDTO.getParams(), yapiApiDTO.getRequestBody(), yapiApiDTO.getResponse(), Integer.valueOf(projectId), yApiUrl, false, yapiApiDTO.getMethod(), yapiApiDTO.getDesc(), yapiApiDTO.getHeader());
                yapiSaveParam.setReq_body_form(yapiApiDTO.getReq_body_form());
                yapiSaveParam.setReq_body_type(yapiApiDTO.getReq_body_type());
                yapiSaveParam.setReq_params(yapiApiDTO.getReq_params());
                if (!Strings.isNullOrEmpty(menu)) {
                    yapiSaveParam.setMenu(menu);
                } else {
                    yapiSaveParam.setMenu(YapiConstant.menu);
                }
                try {
                    // 上传
                    YapiResponse yapiResponse = new UploadYapi().uploadSave(yapiSaveParam, attachUpload, project.getBasePath());
                    if (yapiResponse.getErrcode() != 0) {
                        NotifyUtil.log(notificationGroup, project, "sorry ,upload api error cause:" + yapiResponse.getErrmsg(), NotificationType.ERROR);
                    } else {
                        String url = yApiUrl + "/project/" + projectId + "/interface/api/cat_" + UploadYapi.catMap.get(projectId).get(yapiSaveParam.getMenu());
                        NotifyUtil.log(notificationGroup, project, "success ,url:  " + url, NotificationType.INFORMATION);
                    }
                } catch (Exception e) {
                    NotifyUtil.log(notificationGroup, project, "sorry ,upload api error cause:" + e, NotificationType.ERROR);
                }
            }
        }
    }
}
