package site.forgus.plugins.apigenerator.yapi;

import com.google.common.base.Strings;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import demo.*;
import org.apache.commons.lang.StringUtils;
import site.forgus.plugins.apigenerator.normal.DesUtil;
import site.forgus.plugins.apigenerator.normal.*;
import site.forgus.plugins.apigenerator.yapi.model.*;
import site.forgus.plugins.apigenerator.yapi.sdk.YApiSdk;
import site.forgus.plugins.apigenerator.yapi.util.JsonUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class YApiGenerateAction extends ApiGenerateAction {


    @Override
    protected void generateDocsWithClass(Project project, PsiClass selectedClass) throws IOException {
        super.generateDocsWithClass(project, selectedClass);
    }

    @Override
    protected void generateDocWithMethod(Project project, PsiMethod selectedMethod) throws IOException {
        YApiInterface yApiInterface = buildYApiInterface(project, selectedMethod);
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

    private YApiInterface buildYApiInterface(Project project, PsiMethod psiMethodTarget) {
        PsiClass containingClass = psiMethodTarget.getContainingClass();
        PsiAnnotation controller = null;
        PsiAnnotation classRequestMapping = null;
        for (PsiAnnotation annotation : containingClass.getAnnotations()) {
            if (annotation.getText().contains("Controller")) {
                controller = annotation;
            } else if (annotation.getText().contains("RequestMapping")) {
                classRequestMapping = annotation;
            }
        }
        if (controller == null) {
            NotifyUtil.log(notificationGroup, project, "Invalid Class File!", NotificationType.INFORMATION);
            return null;
        }
        YApiInterface yApiInterface = new YApiInterface();
        yApiInterface.setToken(config.getState().token);
        StringBuilder path = new StringBuilder();
        if (classRequestMapping != null) {
            PsiNameValuePair[] psiNameValuePairs = classRequestMapping.getParameterList().getAttributes();
            if (psiNameValuePairs.length > 0) {
                if (psiNameValuePairs[0].getLiteralValue() != null) {
                    path.append(psiNameValuePairs[0].getLiteralValue());
                } else {
                    PsiAnnotationMemberValue psiAnnotationMemberValue = classRequestMapping.findAttributeValue("value");
                    if (Objects.nonNull(psiAnnotationMemberValue) && Objects.nonNull(psiAnnotationMemberValue.getReference())) {
                        String[] results = psiAnnotationMemberValue.getReference().resolve().getText().split("=");
                        path.append(results[results.length - 1].split(";")[0].replace("\"", "").trim());
                    }
                }
            }
        }

        PsiAnnotation psiAnnotationMethod = PsiAnnotationSearchUtil.findAnnotation(psiMethodTarget, SpringMVCConstant.RequestMapping);
        if (psiAnnotationMethod != null) {
            PsiNameValuePair[] psiNameValuePairs = psiAnnotationMethod.getParameterList().getAttributes();
            if (psiNameValuePairs.length > 0) {
                for (PsiNameValuePair psiNameValuePair : psiNameValuePairs) {
                    String requestMethod = psiNameValuePair.getValue().toString().toUpperCase();
                    String methodName = psiNameValuePair.getName();

                    //获得方法上的路径
                    if (Objects.isNull(psiNameValuePair.getName()) || "value".equals(psiNameValuePair.getName())) {
                        PsiReference psiReference = psiNameValuePair.getValue().getReference();
                        if (psiReference == null) {
                            path.append(psiNameValuePair.getLiteralValue());
                        } else {
                            String[] results = psiReference.resolve().getText().split("=");
                            path.append(results[results.length - 1].split(";")[0].replace("\"", "").trim());
                            yApiInterface.setTitle(site.forgus.plugins.apigenerator.normal.DesUtil.getUrlReFerenceRDesc(psiReference.resolve().getText()));
                            yApiInterface.setDesc("<pre><code>  " + psiReference.resolve().getText() + " </code></pre> <hr>");
                        }
                        yApiInterface.setPath(path.toString());
                    } else {
                        if ("method".equals(methodName)) {
                            // 判断是何种请求、get、post等
                            yApiInterface.setMethod(this.switchRequestMethod(requestMethod));
                        }
                    }
                }
            } else {
                yApiInterface.setPath(path.toString());
            }
        } else {
            // 分析请求方式
            RequestMethodUtil requestMethodUtil = new RequestMethodUtil().invoke(psiMethodTarget);

            yApiInterface.setMethod(requestMethodUtil.getRequestMethod());

            if (Objects.nonNull(requestMethodUtil.getPsiAnnotationMethodSemple())) {
                PsiNameValuePair[] psiNameValuePairs = requestMethodUtil.getPsiAnnotationMethodSemple().getParameterList().getAttributes();
                if (psiNameValuePairs.length > 0) {
                    for (PsiNameValuePair psiNameValuePair : psiNameValuePairs) {
                        //获得方法上的路径
                        if (Objects.isNull(psiNameValuePair.getName()) || psiNameValuePair.getName().equals("value")) {
                            PsiReference psiReference = psiNameValuePair.getValue().getReference();
                            if (psiReference == null) {
                                path.append(psiNameValuePair.getLiteralValue());
                            } else {
                                String[] results = psiReference.resolve().getText().split("=");
                                path.append(results[results.length - 1].split(";")[0].replace("\"", "").trim());
                                yApiInterface.setTitle(site.forgus.plugins.apigenerator.normal.DesUtil.getUrlReFerenceRDesc(psiReference.resolve().getText()));
                                if (!Strings.isNullOrEmpty(psiReference.resolve().getText())) {
                                    String refernceDesc = psiReference.resolve().getText().replace("<", "&lt;").replace(">", "&gt;");
                                    yApiInterface.setDesc("<pre><code>  " + refernceDesc + " </code></pre> <hr>");
                                }
                            }
                            yApiInterface.setPath(path.toString().trim());
                        }
                    }
                } else {
                    yApiInterface.setPath(path.toString().trim());
                }
            }
        }
        String classDesc = psiMethodTarget.getText().replace(Objects.nonNull(psiMethodTarget.getBody()) ? psiMethodTarget.getBody().getText() : "", "");
        if (!Strings.isNullOrEmpty(classDesc)) {
            classDesc = classDesc.replace("<", "&lt;").replace(">", "&gt;");
        }
        yApiInterface.setDesc(Objects.nonNull(yApiInterface.getDesc()) ? yApiInterface.getDesc() : " <pre><code>  " + classDesc + "</code> </pre>");
        try {
            // 生成响应参数
            MethodInfo methodInfo = BuildMdForDubbo.getMethodInfo(project, psiMethodTarget);
            yApiInterface.setRes_body(JsonUtil.buildJson5(methodInfo.getResponseFields()));
            yApiInterface.setReq_body_other(JsonUtil.buildJson5(methodInfo.getRequestFields()));
            //TODO
            yApiInterface.setCatid("838");
            //TODO
            yApiInterface.setTitle("swerwerwer");
            return yApiInterface;
        } catch (Exception ex) {
            Notification error = notificationGroup.createNotification("Convert to JSON failed.", NotificationType.ERROR);
            Notifications.Bus.notify(error, project);
        }
        return null;
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
