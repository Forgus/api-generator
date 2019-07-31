package demo;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.compiled.ClsFileImpl;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.impl.source.PsiJavaFileImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import org.apache.commons.lang.StringUtils;
import site.forgus.plugins.apigenerator.normal.DesUtil;
import site.forgus.plugins.apigenerator.normal.KV;
import site.forgus.plugins.apigenerator.normal.NormalTypes;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 为了yApi 创建的
 */
public class BuildJsonForYApi {
    private static NotificationGroup notificationGroup;

    static {
        notificationGroup = new NotificationGroup("Java2Json.NotificationGroup", NotificationDisplayType.BALLOON, true);
    }

    private static Set<String> filePaths = new CopyOnWriteArraySet<>();

    /**
     * 批量生成 接口数据
     *
     * @param anActionEvent the an action event
     * @param attachUpload  the attach upload
     * @return array list
     */
    public ArrayList<YapiApiDTO> actionPerformedList(AnActionEvent anActionEvent, String attachUpload) {
        Editor editor = anActionEvent.getDataContext().getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = anActionEvent.getDataContext().getData(CommonDataKeys.PSI_FILE);
        // 选中的文字 （类名、方法名）
        String selectedText = anActionEvent.getRequiredData(CommonDataKeys.EDITOR).getSelectionModel().getSelectedText();
        assert editor != null;
        Project project = editor.getProject();
        if (Strings.isNullOrEmpty(selectedText)) {
            NotifyUtil.log(notificationGroup, project, "please select method or class", NotificationType.ERROR);
            return null;
        }
        // 所选的类
        assert psiFile != null;
        PsiClass selectedClass = (PsiClass) PsiTreeUtil.getContextOfType(psiFile.findElementAt(editor.getCaretModel().getOffset()), new Class[]{PsiClass.class});
        String classMenu = null;
        assert selectedClass != null;
        if (Objects.nonNull(selectedClass.getDocComment())) {
            classMenu = DesUtil.getMenu(selectedClass.getText());
        }
        ArrayList<YapiApiDTO> yApiApiDTOs = new ArrayList<>();
        if (selectedText.equals(selectedClass.getName())) {
            // 选择的是类名，则处理当前类中所有public方法
            return this.processingSelectedClass(attachUpload, project, selectedClass, classMenu, yApiApiDTOs);
        } else {
            // 选择的是方法 则处理此方法
            return this.processingSelectedMethod(attachUpload, selectedText, project, selectedClass, classMenu, yApiApiDTOs);
        }
    }

    /**
     * 处理已选择的方法
     *
     * @param attachUpload  the attach upload
     * @param selectedText  the selected text
     * @param project       the project
     * @param selectedClass the selected class
     * @param classMenu     the class menu
     * @param yapiApiDTOS   the yapi api dtos
     * @return the array list
     */
    private ArrayList<YapiApiDTO> processingSelectedMethod(String attachUpload, String selectedText, Project project, PsiClass selectedClass, String classMenu, ArrayList<YapiApiDTO> yapiApiDTOS) {
        PsiMethod[] psiMethods = selectedClass.getAllMethods();
        //寻找目标Method
        PsiMethod psiMethodTarget = Stream.of(psiMethods).filter(psiMethod -> psiMethod.getName().equals(selectedText)).findFirst().orElse(null);
        if (Objects.nonNull(psiMethodTarget)) {
            YapiApiDTO yapiApiDTO = this.actionPerformed(selectedClass, psiMethodTarget, project, attachUpload);
            if (Objects.nonNull(yapiApiDTO) && Objects.isNull(yapiApiDTO.getMenu())) {
                yapiApiDTO.setMenu(classMenu);
            }
            yapiApiDTOS.add(yapiApiDTO);
        } else {
            Notification error = notificationGroup.createNotification("can not find method:" + selectedText, NotificationType.ERROR);
            Notifications.Bus.notify(error, project);
            return null;
        }
        return yapiApiDTOS;
    }

    /**
     * 处理已选择的类中的全部方法
     *
     * @param attachUpload  the attach upload
     * @param project       the project
     * @param selectedClass the selected class
     * @param classMenu     the class menu
     * @param yapiApiDTOS   the yapi api dtos
     * @return the array list
     */
    private ArrayList<YapiApiDTO> processingSelectedClass(String attachUpload, Project project, PsiClass selectedClass, String classMenu, ArrayList<YapiApiDTO> yapiApiDTOS) {
        PsiMethod[] psiMethods = selectedClass.getMethods();
        for (PsiMethod psiMethodTarget : psiMethods) {
            // 如果选中的是类名的话，去除类中私有方法
            if (psiMethodTarget.getModifierList().hasModifierProperty("private")) {
                continue;
            }
            // 处理参数、返回类型、引用等
            YapiApiDTO yapiApiDTO = this.actionPerformed(selectedClass, psiMethodTarget, project, attachUpload);
            if (Objects.isNull(yapiApiDTO)) {
                continue;
            }
            if (Objects.isNull(yapiApiDTO.getMenu())) {
                yapiApiDTO.setMenu(classMenu);
            }
            yapiApiDTOS.add(yapiApiDTO);
        }
        return yapiApiDTOS;
    }


    /**
     * 处理参数、返回类型、引用等
     *
     * @param selectedClass   the selected class
     * @param psiMethodTarget the psi method target
     * @param project         the project
     * @param attachUpload    the attach upload
     * @return the yapi api dto
     */
    private YapiApiDTO  actionPerformed(PsiClass selectedClass, PsiMethod psiMethodTarget, Project project, String attachUpload) {
        YapiApiDTO yapiApiDTO = new YapiApiDTO();
        // 获得路径
        StringBuilder path = new StringBuilder();

        // 获取类上面的RequestMapping 中的value
        PsiAnnotation psiAnnotation = PsiAnnotationSearchUtil.findAnnotation(selectedClass, SpringMVCConstant.RequestMapping);
        if (psiAnnotation != null) {
            PsiNameValuePair[] psiNameValuePairs = psiAnnotation.getParameterList().getAttributes();
            if (psiNameValuePairs.length > 0) {
                if (psiNameValuePairs[0].getLiteralValue() != null) {
                    path.append(psiNameValuePairs[0].getLiteralValue());
                } else {
                    PsiAnnotationMemberValue psiAnnotationMemberValue = psiAnnotation.findAttributeValue("value");
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
                            yapiApiDTO.setTitle(DesUtil.getUrlReFerenceRDesc(psiReference.resolve().getText()));
                            yapiApiDTO.setMenu(DesUtil.getMenu(psiReference.resolve().getText()));
                            yapiApiDTO.setDesc("<pre><code>  " + psiReference.resolve().getText() + " </code></pre> <hr>");
                        }
                        yapiApiDTO.setPath(path.toString());
                    } else {
                        if ("method".equals(methodName)) {
                            // 判断是何种请求、get、post等
                            yapiApiDTO.setMethod(this.switchRequestMethod(requestMethod));
                        }
                    }
                }
            } else {
                yapiApiDTO.setPath(path.toString());
            }
        } else {
            // 分析请求方式
            RequestMethodUtil requestMethodUtil = new RequestMethodUtil().invoke(psiMethodTarget);

            yapiApiDTO.setMethod(requestMethodUtil.getRequestMethod());

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
                                yapiApiDTO.setTitle(DesUtil.getUrlReFerenceRDesc(psiReference.resolve().getText()));
                                yapiApiDTO.setMenu(DesUtil.getMenu(psiReference.resolve().getText()));
                                if (!Strings.isNullOrEmpty(psiReference.resolve().getText())) {
                                    String refernceDesc = psiReference.resolve().getText().replace("<", "&lt;").replace(">", "&gt;");
                                    yapiApiDTO.setDesc("<pre><code>  " + refernceDesc + " </code></pre> <hr>");
                                }
                            }
                            yapiApiDTO.setPath(path.toString().trim());
                        }
                    }
                } else {
                    yapiApiDTO.setPath(path.toString().trim());
                }
            }
        }
        String classDesc = psiMethodTarget.getText().replace(Objects.nonNull(psiMethodTarget.getBody()) ? psiMethodTarget.getBody().getText() : "", "");
        if (!Strings.isNullOrEmpty(classDesc)) {
            classDesc = classDesc.replace("<", "&lt;").replace(">", "&gt;");
        }
        yapiApiDTO.setDesc(Objects.nonNull(yapiApiDTO.getDesc()) ? yapiApiDTO.getDesc() : " <pre><code>  " + classDesc + "</code> </pre>");
        try {
            // 先清空之前的文件路径
            filePaths.clear();
            // 生成响应参数
            yapiApiDTO.setResponse(getResponse(project, psiMethodTarget.getReturnType()));
            Set<String> codeSet = new HashSet<>();
            Long time = System.currentTimeMillis();
            String responseFileName = "/response_" + time + ".zip";
            String requestFileName = "/request_" + time + ".zip";
            String codeFileName = "/code_" + time + ".zip";
            if (!Strings.isNullOrEmpty(attachUpload)) {
                // 打包响应参数文件
                if (filePaths.size() > 0) {
                    changeFilePath(project);
                    FileToZipUtil.toZip(filePaths, project.getBasePath() + responseFileName, true);
                    filePaths.clear();
                    codeSet.add(project.getBasePath() + responseFileName);
                }
                // 清空路径
                // 生成请求参数
            } else {
                filePaths.clear();
            }
            getRequest(project, yapiApiDTO, psiMethodTarget);
            if (!Strings.isNullOrEmpty(attachUpload)) {
                if (filePaths.size() > 0) {
                    changeFilePath(project);
                    FileToZipUtil.toZip(filePaths, project.getBasePath() + requestFileName, true);
                    filePaths.clear();
                    codeSet.add(project.getBasePath() + requestFileName);
                }
                // 打包请求参数文件
                if (codeSet.size() > 0) {
                    FileToZipUtil.toZip(codeSet, project.getBasePath() + codeFileName, true);
                    if (!Strings.isNullOrEmpty(attachUpload)) {
                        String fileUrl = new UploadYapi().uploadFile(attachUpload, project.getBasePath() + codeFileName);
                        if (!Strings.isNullOrEmpty(fileUrl)) {
                            yapiApiDTO.setDesc("java类:<a href='" + fileUrl + "'>下载地址</a><br/>" + yapiApiDTO.getDesc());
                        }
                    }
                }
            } else {
                filePaths.clear();
            }
            //清空打包文件
            if (!Strings.isNullOrEmpty(attachUpload)) {
                File file = new File(project.getBasePath() + codeFileName);
                if (file.exists() && file.isFile()) {
                    file.delete();
                    file = new File(project.getBasePath() + responseFileName);
                    file.delete();
                    file = new File(project.getBasePath() + requestFileName);
                    file.delete();
                }
                // 移除 文件
            }

            // 清空路径
            if (Strings.isNullOrEmpty(yapiApiDTO.getTitle())) {
                yapiApiDTO.setTitle(DesUtil.getDescription(psiMethodTarget));
                if (Objects.nonNull(psiMethodTarget.getDocComment())) {
                    yapiApiDTO.setMenu(DesUtil.getMenu(psiMethodTarget.getDocComment().getText()));
                }
            }
            return yapiApiDTO;
        } catch (Exception ex) {
            Notification error = notificationGroup.createNotification("Convert to JSON failed.", NotificationType.ERROR);
            Notifications.Bus.notify(error, project);
        }
        return null;
    }

    /**
     * Switch request method string.
     *
     * @param requestMethod the request method
     * @return the string
     */
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


    /**
     * @description: 获得请求参数
     * @param: [project, yapiApiDTO, psiMethodTarget]
     * @return: void
     * @author: chengsheng@qbb6.com
     * @date: 2019/2/19
     */
    public static void getRequest(Project project, YapiApiDTO yapiApiDTO, PsiMethod psiMethodTarget) {
        PsiParameter[] psiParameters = psiMethodTarget.getParameterList().getParameters();
        if (psiParameters.length > 0) {
            ArrayList list = new ArrayList<YapiQueryDTO>();
            List<YapiHeaderDTO> yapiHeaderDTOList = new ArrayList<>();
            List<YapiPathVariableDTO> yapiPathVariableDTOList = new ArrayList<>();
            for (PsiParameter psiParameter : psiParameters) {
                if (JavaConstant.HttpServletRequest.equals(psiParameter.getType().getCanonicalText()) || JavaConstant.HttpServletResponse.equals(psiParameter.getType().getCanonicalText())) {
                    continue;
                }
                PsiAnnotation psiAnnotation = PsiAnnotationSearchUtil.findAnnotation(psiParameter, SpringMVCConstant.RequestBody);
                if (psiAnnotation != null) {
                    yapiApiDTO.setRequestBody(getResponse(project, psiParameter.getType()));
                } else {
                    psiAnnotation = PsiAnnotationSearchUtil.findAnnotation(psiParameter, SpringMVCConstant.RequestParam);
                    YapiHeaderDTO yapiHeaderDTO = null;
                    YapiPathVariableDTO yapiPathVariableDTO = null;
                    if (psiAnnotation == null) {
                        psiAnnotation = PsiAnnotationSearchUtil.findAnnotation(psiParameter, SpringMVCConstant.RequestAttribute);
                        if (psiAnnotation == null) {
                            psiAnnotation = PsiAnnotationSearchUtil.findAnnotation(psiParameter, SpringMVCConstant.RequestHeader);
                            if (psiAnnotation == null) {
                                psiAnnotation = PsiAnnotationSearchUtil.findAnnotation(psiParameter, SpringMVCConstant.PathVariable);
                                yapiPathVariableDTO = new YapiPathVariableDTO();
                            } else {
                                yapiHeaderDTO = new YapiHeaderDTO();
                            }
                        }
                    }
                    if (psiAnnotation != null) {
                        PsiNameValuePair[] psiNameValuePairs = psiAnnotation.getParameterList().getAttributes();
                        YapiQueryDTO yapiQueryDTO = new YapiQueryDTO();

                        if (psiNameValuePairs.length > 0) {
                            for (PsiNameValuePair psiNameValuePair : psiNameValuePairs) {
                                if ("name".equals(psiNameValuePair.getName()) || "value".equals(psiNameValuePair.getName())) {
                                    if (yapiHeaderDTO != null) {
                                        yapiHeaderDTO.setName(psiNameValuePair.getValue().getText().replace("\"", ""));
                                    } else if (yapiPathVariableDTO != null) {
                                        yapiPathVariableDTO.setName(psiNameValuePair.getValue().getText().replace("\"", ""));
                                    } else {
                                        yapiQueryDTO.setName(psiNameValuePair.getValue().getText().replace("\"", ""));
                                    }
                                } else if ("required".equals(psiNameValuePair.getName())) {
                                    yapiQueryDTO.setName(psiParameter.getName());
                                    yapiQueryDTO.setRequired(psiNameValuePair.getValue().getText().replace("\"", "").replace("false", "0").replace("true", "1"));
                                } else if ("defaultValue".equals(psiNameValuePair.getName())) {
                                    if (yapiHeaderDTO != null) {
                                        yapiHeaderDTO.setExample(psiNameValuePair.getValue().getText().replace("\"", ""));
                                    } else {
                                        yapiQueryDTO.setExample(psiNameValuePair.getValue().getText().replace("\"", ""));
                                    }
                                } else {
                                    if (yapiHeaderDTO != null) {
                                        yapiHeaderDTO.setName(StringUtils.isNotBlank(psiNameValuePair.getLiteralValue()) ? psiNameValuePair.getLiteralValue() : psiParameter.getName());
                                        // 通过方法注释获得 描述 加上 类型
                                        yapiHeaderDTO.setDesc(DesUtil.getParamDesc(psiMethodTarget, psiParameter.getName()) + "(" + psiParameter.getType().getPresentableText() + ")");
                                    }
                                    if (yapiPathVariableDTO != null) {
                                        yapiPathVariableDTO.setName(StringUtils.isNotBlank(psiNameValuePair.getLiteralValue()) ? psiNameValuePair.getLiteralValue() : psiParameter.getName());
                                        // 通过方法注释获得 描述 加上 类型
                                        yapiPathVariableDTO.setDesc(DesUtil.getParamDesc(psiMethodTarget, psiParameter.getName()) + "(" + psiParameter.getType().getPresentableText() + ")");
                                    } else {
                                        yapiQueryDTO.setName(StringUtils.isNotBlank(psiNameValuePair.getLiteralValue()) ? psiNameValuePair.getLiteralValue() : psiParameter.getName());
                                        // 通过方法注释获得 描述 加上 类型
                                        yapiQueryDTO.setDesc(DesUtil.getParamDesc(psiMethodTarget, psiParameter.getName()) + "(" + psiParameter.getType().getPresentableText() + ")");
                                    }
                                    if (NormalTypes.normalTypes.containsKey(psiParameter.getType().getPresentableText())) {
                                        if (yapiHeaderDTO != null) {
                                            yapiHeaderDTO.setExample(NormalTypes.normalTypes.get(psiParameter.getType().getPresentableText()).toString());
                                        } else if (yapiPathVariableDTO != null) {
                                            yapiPathVariableDTO.setExample(NormalTypes.normalTypes.get(psiParameter.getType().getPresentableText()).toString());
                                        } else {
                                            yapiQueryDTO.setExample(NormalTypes.normalTypes.get(psiParameter.getType().getPresentableText()).toString());
                                        }
                                    } else {
                                        yapiApiDTO.setRequestBody(getResponse(project, psiParameter.getType()));
                                    }

                                }
                            }
                        } else {
                            if (yapiHeaderDTO != null) {
                                yapiHeaderDTO.setName(psiParameter.getName());
                                // 通过方法注释获得 描述 加上 类型
                                yapiHeaderDTO.setDesc(DesUtil.getParamDesc(psiMethodTarget, psiParameter.getName()) + "(" + psiParameter.getType().getPresentableText() + ")");
                            } else if (yapiPathVariableDTO != null) {
                                yapiPathVariableDTO.setName(psiParameter.getName());
                                // 通过方法注释获得 描述 加上 类型
                                yapiPathVariableDTO.setDesc(DesUtil.getParamDesc(psiMethodTarget, psiParameter.getName()) + "(" + psiParameter.getType().getPresentableText() + ")");
                            } else {
                                yapiQueryDTO.setName(psiParameter.getName());
                                // 通过方法注释获得 描述 加上 类型
                                yapiQueryDTO.setDesc(DesUtil.getParamDesc(psiMethodTarget, psiParameter.getName()) + "(" + psiParameter.getType().getPresentableText() + ")");
                            }
                            if (NormalTypes.normalTypes.containsKey(psiParameter.getType().getPresentableText())) {
                                if (yapiHeaderDTO != null) {
                                    yapiHeaderDTO.setExample(NormalTypes.normalTypes.get(psiParameter.getType().getPresentableText()).toString());
                                } else if (yapiPathVariableDTO != null) {
                                    yapiPathVariableDTO.setExample(NormalTypes.normalTypes.get(psiParameter.getType().getPresentableText()).toString());
                                } else {
                                    yapiQueryDTO.setExample(NormalTypes.normalTypes.get(psiParameter.getType().getPresentableText()).toString());
                                }
                            } else {
                                yapiApiDTO.setRequestBody(getResponse(project, psiParameter.getType()));
                            }
                        }
                        if (yapiHeaderDTO != null) {
                            if (Strings.isNullOrEmpty(yapiHeaderDTO.getDesc())) {
                                // 通过方法注释获得 描述  加上 类型
                                yapiHeaderDTO.setDesc(DesUtil.getParamDesc(psiMethodTarget, psiParameter.getName()) + "(" + psiParameter.getType().getPresentableText() + ")");
                            }
                            if (Strings.isNullOrEmpty(yapiHeaderDTO.getExample()) && NormalTypes.normalTypes.containsKey(psiParameter.getType().getPresentableText())) {
                                yapiHeaderDTO.setExample(NormalTypes.normalTypes.get(psiParameter.getType().getPresentableText()).toString());
                            }
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
                            if (Strings.isNullOrEmpty(yapiQueryDTO.getDesc())) {
                                // 通过方法注释获得 描述 加上 类型
                                yapiQueryDTO.setDesc(DesUtil.getParamDesc(psiMethodTarget, psiParameter.getName()) + "(" + psiParameter.getType().getPresentableText() + ")");
                            }
                            if (Strings.isNullOrEmpty(yapiQueryDTO.getExample()) && NormalTypes.normalTypes.containsKey(psiParameter.getType().getPresentableText())) {
                                yapiQueryDTO.setExample(NormalTypes.normalTypes.get(psiParameter.getType().getPresentableText()).toString());
                            }
                            list.add(yapiQueryDTO);
                        }
                    } else {
                        // 支持实体对象接收
                        yapiApiDTO.setReq_body_type("form");
                        if (yapiApiDTO.getReq_body_form() != null) {
                            yapiApiDTO.getReq_body_form().addAll(getRequestForm(project, psiParameter, psiMethodTarget));
                        } else {
                            yapiApiDTO.setReq_body_form(getRequestForm(project, psiParameter, psiMethodTarget));
                        }
                    }
                }
            }
            yapiApiDTO.setParams(list);
            yapiApiDTO.setHeader(yapiHeaderDTOList);
            yapiApiDTO.setReq_params(yapiPathVariableDTOList);
        }
    }

    /**
     * @description: 获得表单提交数据对象
     * @param: [requestClass]
     * @return: java.util.List<java.util.Map < java.lang.String, java.lang.String>>
     * @author: chengsheng@qbb6.com
     * @date: 2019/5/17
     */
    public static List<Map<String, String>> getRequestForm(Project project, PsiParameter psiParameter, PsiMethod psiMethodTarget) {
        List<Map<String, String>> requestForm = new ArrayList<>();
        if (NormalTypes.normalTypes.containsKey(psiParameter.getType().getPresentableText())) {
            Map<String, String> map = new HashMap<>();
            map.put("name", psiParameter.getName());
            map.put("type", "text");
            String remark = DesUtil.getParamDesc(psiMethodTarget, psiParameter.getName()) + "(" + psiParameter.getType().getPresentableText() + ")";
            map.put("desc", remark);
            map.put("example", NormalTypes.normalTypes.get(psiParameter.getType().getPresentableText()).toString());
            requestForm.add(map);
        } else {
            PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(psiParameter.getType().getCanonicalText(), GlobalSearchScope.allScope(project));
            for (PsiField field : psiClass.getAllFields()) {
                if (field.getModifierList().hasModifierProperty("final")) {
                    continue;
                }
                Map<String, String> map = new HashMap<>();
                map.put("name", field.getName());
                map.put("type", "text");
                String remark = DesUtil.getFiledDesc(field.getDocComment());
                remark = DesUtil.getLinkRemark(remark, project, field);
                map.put("desc", remark);
                if (Objects.nonNull(field.getType().getPresentableText())) {
                    Object obj = NormalTypes.normalTypes.get(field.getType().getPresentableText());
                    if (Objects.nonNull(obj)) {
                        map.put("example", NormalTypes.normalTypes.get(field.getType().getPresentableText()).toString());
                    }
                }
                requestForm.add(map);
            }
        }
        return requestForm;
    }

    /**
     * @description: 获得响应参数
     * @param: [project, psiType]
     * @return: java.lang.String
     * @author: chengsheng@qbb6.com
     * @date: 2019/2/19
     */
    public static String getResponse(Project project, PsiType psiType) {
        return getPojoJson(project, psiType);
    }


    public static String getPojoJson(Project project, PsiType psiType)  {
        if (psiType instanceof PsiPrimitiveType) {
            //如果是基本类型
            KV kvClass = KV.create();
            kvClass.set(psiType.getCanonicalText(), NormalTypes.normalTypes.get(psiType.getPresentableText()));
        } else if (NormalTypes.isNormalType(psiType.getPresentableText())) {
            //如果是包装类型
            KV kvClass = KV.create();
            kvClass.set(psiType.getCanonicalText(), NormalTypes.normalTypes.get(psiType.getPresentableText()));
        } else if (psiType.getPresentableText().startsWith("List")) {
            String[] types = psiType.getCanonicalText().split("<");
            KV listKv = new KV();
            if (types.length > 1) {
                String childPackage = types[1].split(">")[0];
                if (NormalTypes.noramlTypesPackages.keySet().contains(childPackage)) {
                    listKv.set("type", NormalTypes.noramlTypesPackages.get(childPackage));
                } else if (NormalTypes.collectTypesPackages.containsKey(childPackage)) {
                    listKv.set("type", NormalTypes.collectTypesPackages.get(childPackage));
                } else {
                    PsiClass psiClassChild = JavaPsiFacade.getInstance(project).findClass(childPackage, GlobalSearchScope.allScope(project));
                    List<String> requiredList = new ArrayList<>();
                    KV kvObject = getFields(psiClassChild, project, null, null, requiredList);
                    listKv.set("type", "object");
                    addFilePaths(filePaths, psiClassChild);
                    if (Objects.nonNull(psiClassChild.getSuperClass()) && !psiClassChild.getSuperClass().getName().toString().equals("Object")) {
                        addFilePaths(filePaths, psiClassChild.getSuperClass());
                    }
                    listKv.set("properties", kvObject);
                    listKv.set("required", requiredList);
                }
            }
            KV result = new KV();
            result.set("type", "array");
            result.set("title", psiType.getPresentableText());
            result.set("description", psiType.getPresentableText());
            result.set("items", listKv);
            String json = result.toPrettyJson();
            return json;
        } else if (psiType.getPresentableText().startsWith("Set")) {
            String[] types = psiType.getCanonicalText().split("<");
            KV listKv = new KV();
            if (types.length > 1) {
                String childPackage = types[1].split(">")[0];
                if (NormalTypes.noramlTypesPackages.keySet().contains(childPackage)) {
                    listKv.set("type", NormalTypes.noramlTypesPackages.get(childPackage));
                } else if (NormalTypes.collectTypesPackages.containsKey(childPackage)) {
                    listKv.set("type", NormalTypes.collectTypesPackages.get(childPackage));
                } else {
                    PsiClass psiClassChild = JavaPsiFacade.getInstance(project).findClass(childPackage, GlobalSearchScope.allScope(project));
                    List<String> requiredList = new ArrayList<>();
                    KV kvObject = getFields(psiClassChild, project, null, null, requiredList);
                    listKv.set("type", "object");
                    addFilePaths(filePaths, psiClassChild);
                    if (Objects.nonNull(psiClassChild.getSuperClass()) && !psiClassChild.getSuperClass().getName().toString().equals("Object")) {
                        addFilePaths(filePaths, psiClassChild.getSuperClass());
                    }
                    listKv.set("properties", kvObject);
                    listKv.set("required", requiredList);
                }
            }
            KV result = new KV();
            result.set("type", "array");
            result.set("title", psiType.getPresentableText());
            result.set("description", psiType.getPresentableText());
            result.set("items", listKv);
            String json = result.toPrettyJson();
            return json;
        } else if (psiType.getPresentableText().startsWith("Map")) {
            HashMap hashMapChild = new HashMap();
            String[] types = psiType.getCanonicalText().split("<");
            if (types.length > 1) {
                hashMapChild.put("paramMap", psiType.getPresentableText());
            }
            KV kvClass = KV.create();
            kvClass.set(types[0], hashMapChild);
            KV result = new KV();
            result.set("type", "object");
            result.set("title", psiType.getPresentableText());
            result.set("description", psiType.getPresentableText());
            result.set("properties", hashMapChild);
            String json = result.toPrettyJson();
            return json;
        } else if (NormalTypes.collectTypes.containsKey(psiType.getPresentableText())) {
            //如果是集合类型
            KV kvClass = KV.create();
            kvClass.set(psiType.getCanonicalText(), NormalTypes.collectTypes.get(psiType.getPresentableText()));
        } else {
            String[] types = psiType.getCanonicalText().split("<");
            if (types.length > 1) {
                PsiClass psiClassChild = JavaPsiFacade.getInstance(project).findClass(types[0], GlobalSearchScope.allScope(project));
                KV result = new KV();
                List<String> requiredList = new ArrayList<>();
                KV kvObject = getFields(psiClassChild, project, types, 1, requiredList);
                result.set("type", "object");
                result.set("title", psiType.getPresentableText());
                result.set("required", requiredList);
                addFilePaths(filePaths, psiClassChild);
                if (Objects.nonNull(psiClassChild.getSuperClass()) && !psiClassChild.getSuperClass().getName().toString().equals("Object")) {
                    addFilePaths(filePaths, psiClassChild.getSuperClass());
                }
                result.set("description", (psiType.getPresentableText() + " :" + psiClassChild.getName()).trim());
                result.set("properties", kvObject);
                String json = result.toPrettyJson();
                return json;
            } else {
                PsiClass psiClassChild = JavaPsiFacade.getInstance(project).findClass(psiType.getCanonicalText(), GlobalSearchScope.allScope(project));
                KV result = new KV();
                List<String> requiredList = new ArrayList<>();
                KV kvObject = getFields(psiClassChild, project, null, null, requiredList);
                addFilePaths(filePaths, psiClassChild);
                if (Objects.nonNull(psiClassChild.getSuperClass()) && !psiClassChild.getSuperClass().getName().toString().equals("Object")) {
                    addFilePaths(filePaths, psiClassChild.getSuperClass());
                }
                result.set("type", "object");
                result.set("required", requiredList);
                result.set("title", psiType.getPresentableText());
                result.set("description", (psiType.getPresentableText() + " :" + psiClassChild.getName()).trim());
                result.set("properties", kvObject);
                String json = result.toPrettyJson();
                return json;
            }
        }
        return null;
    }

    /**
     * @description: 获得属性列表
     * @param: [psiClass, project, childType, index]
     * @return: com.qbb.build.KV
     * @author: chengsheng@qbb6.com
     * @date: 2019/5/15
     */
    public static KV getFields(PsiClass psiClass, Project project, String[] childType, Integer index, List<String> requiredList) {
        KV kv = KV.create();
        if (psiClass != null) {
            if (Objects.nonNull(psiClass.getSuperClass()) && Objects.nonNull(NormalTypes.collectTypes.get(psiClass.getSuperClass().getName()))) {
                for (PsiField field : psiClass.getFields()) {
                    if (Objects.nonNull(PsiAnnotationSearchUtil.findAnnotation(field, JavaConstant.NotNull))) {
                        requiredList.add(field.getName());
                    }
                    getField(field, project, kv, childType, index, psiClass.getName());
                }
            } else {
                if (NormalTypes.genericList.contains(psiClass.getName()) && childType != null && childType.length > index) {
                    String child = childType[index].split(">")[0];
                    PsiClass psiClassChild = JavaPsiFacade.getInstance(project).findClass(child, GlobalSearchScope.allScope(project));
                    return getFields(psiClassChild, project, childType, index + 1, requiredList);
                } else {
                    for (PsiField field : psiClass.getAllFields()) {
                        if (Objects.nonNull(PsiAnnotationSearchUtil.findAnnotation(field, JavaConstant.NotNull))) {
                            requiredList.add(field.getName());
                        }
                        getField(field, project, kv, childType, index, psiClass.getName());
                    }
                }
            }
        }
        return kv;
    }

    /**
     * @description: 获得单个属性
     * @param: [field, project, kv, childType, index, pName]
     * @return: void
     * @author: chengsheng@qbb6.com
     * @date: 2019/5/15
     */
    public static void getField(PsiField field, Project project, KV kv, String[] childType, Integer index, String pName) {
        if (field.getModifierList().hasModifierProperty("final")) {
            return;
        }
        PsiType type = field.getType();
        String name = field.getName();
        String remark = "";
        if (field.getDocComment() != null) {
            remark = DesUtil.getFiledDesc(field.getDocComment());
            //获得link 备注
            remark = DesUtil.getLinkRemark(remark, project, field);
        }
        // 如果是基本类型
        if (type instanceof PsiPrimitiveType) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("type", javaTypeToJsType(type.getPresentableText()));
            if (!Strings.isNullOrEmpty(remark)) {
                jsonObject.addProperty("description", remark);
            }
            kv.set(name, jsonObject);
        } else {
            //reference Type
            String fieldTypeName = type.getPresentableText();
            //normal Type
            if (NormalTypes.isNormalType(fieldTypeName)) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("type", javaTypeToJsType(fieldTypeName));
                if (!Strings.isNullOrEmpty(remark)) {
                    jsonObject.addProperty("description", remark);
                }
                kv.set(name, jsonObject);
            } else if (!(type instanceof PsiArrayType) && ((PsiClassReferenceType) type).resolve().isEnum()) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("type", "enum");
                if (!Strings.isNullOrEmpty(remark)) {
                    jsonObject.addProperty("description", remark);
                }
                kv.set(name, jsonObject);
            } else if (NormalTypes.genericList.contains(fieldTypeName)) {
                if (childType != null) {
                    String child = childType[index].split(">")[0];
                    if (child.contains("java.util.List") || child.contains("java.util.Set") || child.contains("java.util.HashSet")) {
                        index = index + 1;
                        PsiClass psiClassChild = JavaPsiFacade.getInstance(project).findClass(childType[index].split(">")[0], GlobalSearchScope.allScope(project));
                        getCollect(kv, psiClassChild.getName(), remark, psiClassChild, project, name, pName, childType, index + 1);
                    } else {
                        //class type
                        KV kv1 = new KV();
                        kv1.set(KV.by("type", "object"));
                        PsiClass psiClassChild = JavaPsiFacade.getInstance(project).findClass(child, GlobalSearchScope.allScope(project));
                        kv1.set(KV.by("description", (Strings.isNullOrEmpty(remark) ? ("" + psiClassChild.getName().trim()) : remark + " ," + psiClassChild.getName().trim())));
                        if (!pName.equals(psiClassChild.getName())) {
                            List<String> requiredList = new ArrayList<>();
                            kv1.set(KV.by("properties", getFields(psiClassChild, project, childType, index + 1, requiredList)));
                            kv1.set("required", requiredList);
                            addFilePaths(filePaths, psiClassChild);
                        } else {
                            kv1.set(KV.by("type", pName));
                        }
                        kv.set(name, kv1);
                    }
                }
                //    getField()
            } else if (type instanceof PsiArrayType) {
                //array type
                PsiType deepType = type.getDeepComponentType();
                KV kvlist = new KV();
                String deepTypeName = deepType.getPresentableText();
                String cType = "";
                if (deepType instanceof PsiPrimitiveType) {
                    kvlist.set("type", javaTypeToJsType(type.getPresentableText()));
                    if (!Strings.isNullOrEmpty(remark)) {
                        kvlist.set("description", remark);
                    }
                } else if (NormalTypes.isNormalType(deepTypeName)) {
                    kvlist.set("type", javaTypeToJsType(deepTypeName));
                    if (!Strings.isNullOrEmpty(remark)) {
                        kvlist.set("description", remark);
                    }
                } else {
                    kvlist.set(KV.by("type", "object"));
                    PsiClass psiClass = PsiUtil.resolveClassInType(deepType);
                    cType = psiClass.getName();
                    kvlist.set(KV.by("description", (Strings.isNullOrEmpty(remark) ? ("" + psiClass.getName().trim()) : remark + " ," + psiClass.getName().trim())));
                    if (!pName.equals(PsiUtil.resolveClassInType(deepType).getName())) {
                        List<String> requiredList = new ArrayList<>();
                        kvlist.set("properties", getFields(psiClass, project, null, null, requiredList));
                        kvlist.set("required", requiredList);
                        addFilePaths(filePaths, psiClass);
                    } else {
                        kvlist.set(KV.by("type", javaTypeToJsType(pName)));
                    }
                }
                KV kv1 = new KV();
                kv1.set(KV.by("type", "array"));
                kv1.set(KV.by("description", (remark + " :" + cType).trim()));
                kv1.set("items", kvlist);
                kv.set(name, kv1);
            } else if (fieldTypeName.startsWith("List") || fieldTypeName.startsWith("Set") || fieldTypeName.startsWith("HashSet")) {
                //list type
                PsiType iterableType = PsiUtil.extractIterableTypeParameter(type, false);
                PsiClass iterableClass = PsiUtil.resolveClassInClassTypeOnly(iterableType);
                String classTypeName = iterableClass.getName();
                getCollect(kv, classTypeName, remark, iterableClass, project, name, pName, childType, index);
            } else if (fieldTypeName.startsWith("HashMap") || fieldTypeName.startsWith("Map") || fieldTypeName.startsWith("LinkedHashMap")) {
                //HashMap or Map
                CompletableFuture.runAsync(() -> {
                    try {
                        TimeUnit.MILLISECONDS.sleep(700);
                        Notification warning = notificationGroup.createNotification("Map Type Can not Change,So pass", NotificationType.WARNING);
                        Notifications.Bus.notify(warning, project);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            } else {
                //class type
                KV kv1 = new KV();
                PsiClass psiClass = PsiUtil.resolveClassInType(type);
                kv1.set(KV.by("type", "object"));
                kv1.set(KV.by("description", (Strings.isNullOrEmpty(remark) ? ("" + psiClass.getName().trim()) : (remark + " ," + psiClass.getName()).trim())));
                if (!pName.equals(((PsiClassReferenceType) type).getClassName())) {
                    addFilePaths(filePaths, psiClass);
                    List<String> requiredList = new ArrayList<>();
                    kv1.set(KV.by("properties", getFields(PsiUtil.resolveClassInType(type), project, childType, index, requiredList)));
                    kv1.set("required", requiredList);
                } else {
                    kv1.set(KV.by("type", javaTypeToJsType(pName)));
                }
                kv.set(name, kv1);
            }
        }
    }

    /**
     * Java type to js type string.
     *
     * @param fieldTypeName the field type name
     * @return the string
     */
    public static String javaTypeToJsType(String fieldTypeName) {
        switch (fieldTypeName) {
            case "String":
            case "Character":
            case "char":
                return "string";
            case "Float":
            case "float":
            case "Double":
            case "double":
            case "BigDecimal":
                return "number";
            case "Integer":
            case "int":
            case "long":
            case "Long":
            case "short":
            case "Short":
            case "Byte":
            case "byte":
            case "Date":
                return "integer";
            case "Boolean":
                return "boolean";
            default:
                return fieldTypeName;
        }
    }

    /**
     * 获得集合.
     *
     * @param kv            the kv
     * @param classTypeName the class type name
     * @param remark        the remark
     * @param psiClass      the psi class
     * @param project       the project
     * @param name          the name
     * @param pName         the p name
     * @param childType     the child type
     * @param index         the index
     */
    public static void getCollect(KV kv, String classTypeName, String remark, PsiClass psiClass, Project project, String name, String pName, String[] childType, Integer index) {
        KV<String, Object> kvList = new KV<>();
        if (NormalTypes.isNormalType(classTypeName) || NormalTypes.collectTypes.containsKey(classTypeName)) {
            kvList.set("type", javaTypeToJsType(classTypeName));
            if (!Strings.isNullOrEmpty(remark)) {
                kvList.set("description", remark);
            }
        } else {
            kvList.set(KV.by("type", "object"));
            kvList.set(KV.by("description", (Strings.isNullOrEmpty(remark) ? ("" + psiClass.getName().trim()) : remark + " ," + psiClass.getName().trim())));
            if (!pName.equals(psiClass.getName())) {
                List<String> requiredList = new ArrayList<>();
                kvList.set("properties", getFields(psiClass, project, childType, index, requiredList));
                kvList.set("required", requiredList);
                addFilePaths(filePaths, psiClass);
            } else {
                kvList.set(KV.by("type", pName));
            }
        }
        KV kv1 = new KV();
        kv1.set(KV.by("type", "array"));
        kv1.set(KV.by("description", (Strings.isNullOrEmpty(remark) ? ("" + psiClass.getName().trim()) : remark + " ," + psiClass.getName().trim())));
        kv1.set("items", kvList);
        kv.set(name, kv1);
    }


    /**
     * 添加到文件路径列表
     *
     * @param filePaths the file paths
     * @param psiClass  the psi class
     */
    public static void addFilePaths(Set<String> filePaths, PsiClass psiClass) {
        try {
            filePaths.add(((PsiJavaFileImpl) psiClass.getContext()).getViewProvider().getVirtualFile().getPath());
        } catch (Exception e) {
            try {
                filePaths.add(((ClsFileImpl) psiClass.getContext()).getViewProvider().getVirtualFile().getPath());
            } catch (Exception e1) {
            }
        }
    }


    /**
     * 转换文件路径
     *
     * @param project the project
     */
    public static void changeFilePath(Project project) {
        Set<String> changeFilePaths = filePaths.stream().map(filePath -> {
            if (filePath.contains(".jar")) {
                String[] filePathsubs = filePath.split("\\.jar");
                String jarPath = filePathsubs[0] + "-sources.jar";
                try {
                    //去解压源码包
                    FileUnZipUtil.uncompress(new File(jarPath), new File(filePathsubs[0]));
                    filePath = filePathsubs[0] + filePathsubs[1].replace("!", "");
                    return filePath.replace(".class", ".java");
                } catch (IOException e) {
                    Notification error = notificationGroup.createNotification("can not find sources java:" + jarPath, NotificationType.ERROR);
                    Notifications.Bus.notify(error, project);
                }
            }
            return filePath;
        }).collect(Collectors.toSet());
        filePaths.clear();
        filePaths.addAll(changeFilePaths);
    }


}
