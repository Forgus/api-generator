package site.forgus.plugins.apigenerator.normal;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;
import org.apache.commons.lang.StringUtils;
import site.forgus.plugins.apigenerator.config.ApiGeneratorConfig;
import site.forgus.plugins.apigenerator.constant.TypeEnum;
import site.forgus.plugins.apigenerator.constant.WebAnnotation;
import site.forgus.plugins.apigenerator.util.AssertUtils;
import site.forgus.plugins.apigenerator.util.DesUtil;
import site.forgus.plugins.apigenerator.util.FieldUtil;

import java.util.*;

public class FieldInfo {

    private String name;
    private PsiType psiType;
    private boolean require;
    private String range;
    private String desc = "";
    private TypeEnum paramType;
    private FieldType fieldType;
    private List<FieldInfo> children;
    private FieldInfo parent;
    private List<PsiAnnotation> annotations;
    private Project project;
    private Map<PsiTypeParameter, PsiType> genericTypeMap;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldInfo fieldInfo = (FieldInfo) o;
        return name.equals(fieldInfo.name) &&
                Objects.equals(parent, fieldInfo.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, parent);
    }

    private static List<String> requiredTexts = Arrays.asList("@NotNull", "@NotBlank", "@NotEmpty", "@PathVariable");

    protected ApiGeneratorConfig config;

    private Map<PsiTypeParameter, PsiType> resolveGenerics(PsiType psiType){
        if(psiType instanceof PsiArrayType) {
            return new HashMap<>();
        }
        if(psiType instanceof  PsiClassType) {
            PsiClassType psiClassType = (PsiClassType) psiType;
            PsiType[] realParameters = psiClassType.getParameters();
            PsiTypeParameter[] formParameters = psiClassType.resolve().getTypeParameters();
            int i = 0;
            Map<PsiTypeParameter, PsiType> map = new HashMap<>();
            for (PsiType realParameter : realParameters) {
                map.put(formParameters[i], realParameter);
                i ++;
            }
            return map;
        }
        return new HashMap<>();
    }


    public FieldType getFieldType() {
        return fieldType;
    }

    public void setFieldType(FieldType fieldType) {
        this.fieldType = fieldType;
    }

    public FieldInfo(Project project,PsiType psiType) {
        this.project = project;
        config = ServiceManager.getService(project,ApiGeneratorConfig.class);
        this.psiType = psiType;
        this.genericTypeMap = resolveGenerics(psiType);
        if (FieldUtil.isNormalType(psiType)) {
            paramType = TypeEnum.LITERAL;
        } else if (FieldUtil.isIterableType(psiType)) {
            paramType = TypeEnum.ARRAY;
        } else {
            paramType = TypeEnum.OBJECT;
        }
        if (needResolveChildren(psiType)) {
            this.children = listChildren(this);
        }
    }

    public FieldInfo(Project project,String name, PsiType psiType, String desc, PsiAnnotation[] annotations) {
        this.project = project;
        config = ServiceManager.getService(project,ApiGeneratorConfig.class);
        RequireAndRange requireAndRange = getRequireAndRange(annotations);
        String fieldName = getParamName(name, annotations);
        this.name = fieldName == null ? "N/A" : fieldName;
        this.psiType = psiType;
        this.require = requireAndRange.isRequire();
        this.range = requireAndRange.getRange();
        this.desc = desc == null ? "" : desc;
        this.annotations = Arrays.asList(annotations);
        if (psiType != null) {
            if (FieldUtil.isNormalType(psiType)) {
                paramType = TypeEnum.LITERAL;
            } else if (FieldUtil.isIterableType(psiType)) {
                paramType = TypeEnum.ARRAY;
            } else {
                paramType = TypeEnum.OBJECT;
            }
            if (needResolveChildren(psiType)) {
                this.children = listChildren(this);
            }
        } else {
            paramType = TypeEnum.OBJECT;
        }
    }

    public FieldInfo(Project project,FieldInfo parent, String name, PsiType psiType, String desc, PsiAnnotation[] annotations) {
        this.project = project;
        config = ServiceManager.getService(project,ApiGeneratorConfig.class);
        RequireAndRange requireAndRange = getRequireAndRange(annotations);
        String fieldName = getParamName(name, annotations);
        this.name = fieldName == null ? "N/A" : fieldName;
        this.psiType = psiType;
        this.require = requireAndRange.isRequire();
        this.range = requireAndRange.getRange();
        this.desc = desc == null ? "" : desc;
        this.annotations = Arrays.asList(annotations);
        this.genericTypeMap = resolveGenerics(psiType);
        this.parent = parent;
        if (psiType != null) {
            if (FieldUtil.isNormalType(psiType)) {
                paramType = TypeEnum.LITERAL;
            } else if (FieldUtil.isIterableType(psiType)) {
                paramType = TypeEnum.ARRAY;
            } else {
                paramType = TypeEnum.OBJECT;
            }
            if (needResolveChildren(parent, psiType)) {
                this.children = listChildren(this);
            }
        } else {
            paramType = TypeEnum.OBJECT;
        }
    }

    private static String getParamName(String name, PsiAnnotation[] annotations) {
        PsiAnnotation requestParamAnnotation = getRequestParamAnnotation(annotations);
        if (requestParamAnnotation == null) {
            return name;
        }
        PsiNameValuePair[] attributes = requestParamAnnotation.getParameterList().getAttributes();
        if (attributes.length == 1 && attributes[0].getName() == null) {
            return attributes[0].getLiteralValue();
        }
        for (PsiNameValuePair psiNameValuePair : attributes) {
            String pairName = psiNameValuePair.getName();
            if ("value".equals(pairName) || "name".equals(pairName)) {
                return psiNameValuePair.getLiteralValue();
            }
        }
        return name;
    }

    private static PsiAnnotation getRequestParamAnnotation(PsiAnnotation[] annotations) {
        for (PsiAnnotation annotation : annotations) {
            if (annotation.getText().contains(WebAnnotation.RequestParam)) {
                return annotation;
            }
        }
        return null;
    }

    private List<FieldInfo> listChildren(FieldInfo fieldInfo) {
        PsiType psiType = fieldInfo.getPsiType();
        if (FieldUtil.isNormalType(psiType.getPresentableText())) {
            //基础类或基础包装类没有子域
            return new ArrayList<>();
        }
        //如果是数组
        if(psiType instanceof PsiArrayType) {
            PsiType componentType = ((PsiArrayType) psiType).getComponentType();
            if (FieldUtil.isNormalType(componentType.getPresentableText()) || isMapType(componentType)) {
                return new ArrayList<>();
            }
            return listChildren(FieldFactory.buildPsiType(fieldInfo.getProject(),componentType));
        }
        if (psiType instanceof PsiClassType) {
            //如果是集合类型
            if (FieldUtil.isCollectionType(psiType)) {
                PsiType iterableType = PsiUtil.extractIterableTypeParameter(psiType, false);
                if (iterableType == null || FieldUtil.isNormalType(iterableType.getPresentableText()) || isMapType(iterableType)) {
                    return new ArrayList<>();
                }
                //兼容泛型
                iterableType = getTypeByGenerics(iterableType);
                return listChildren(FieldFactory.buildPsiType(fieldInfo.getProject(),iterableType));
            }
            String typeName = psiType.getPresentableText();
            if (typeName.startsWith("Map")) {
                return new ArrayList<>();
            }
            //兼容泛型
            psiType = getTypeByGenerics(psiType);
            PsiClass psiClass = PsiUtil.resolveClassInType(psiType);
            if (psiClass == null) {
                return new ArrayList<>();
            }
            List<FieldInfo> fieldInfos = new ArrayList<>();
            for (PsiField psiField : psiClass.getAllFields()) {
                if (config.getState().excludeFields.contains(psiField.getName())) {
                    continue;
                }
                PsiType fieldType = psiField.getType();
                //兼容泛型
                fieldType = getTypeByGenerics(fieldType);
                fieldInfos.add(FieldFactory.buildFieldWithParent(fieldInfo.getProject(),fieldInfo,psiField.getName(), fieldType,DesUtil.getDescription(psiField.getDocComment()), psiField.getAnnotations()));
            }
            return fieldInfos;
        }
        return new ArrayList<>();
    }

    /**
     * 根据泛型获取对应的PsiType
     * @param psiType
     * @return
     */
    private PsiType getTypeByGenerics(PsiType psiType){
        if(null == psiType){
            return null;
        }
        if(this.parent != null){
            return this.parent.getTypeByGenerics(psiType);
        }
        if(null != genericTypeMap){
            for (PsiTypeParameter psiTypeParameter : genericTypeMap.keySet()) {
                if(Objects.equals(psiTypeParameter.getName(), psiType.getPresentableText())){
                    return genericTypeMap.get(psiTypeParameter);
                }
            }
        }
        return psiType;
    }

    private boolean needResolveChildren(PsiType psiType) {
        PsiClass psiClass = PsiUtil.resolveClassInType(psiType);
        if(psiClass != null) {
            if(psiClass.isEnum()) {
                return false;
            }
        }
        return !isMapType(psiType);
    }

    private boolean needResolveChildren(FieldInfo parent, PsiType psiType) {
        if (parent == null) {
            return true;
        }
        PsiClass psiClass = PsiUtil.resolveClassInType(psiType);
        if(psiClass != null) {
            if(psiClass.isEnum()) {
                return false;
            }
        }
        if (isMapType(psiType)) {
            return false;
        }
        Set<PsiType> resolvedTypeSet = new HashSet<>();
        FieldInfo p = parent;
        while (p != null) {
            resolvedTypeSet.add(p.getPsiType());
            p = p.getParent();
        }
        if (TypeEnum.ARRAY.equals(paramType)) {
            if(psiType instanceof PsiArrayType) {
                psiType = ((PsiArrayType)psiType).getComponentType();
            }else {
                psiType = PsiUtil.extractIterableTypeParameter(psiType, false);
            }
        }
        if(psiType instanceof PsiArrayType) {
            psiType = ((PsiArrayType)psiType).getComponentType();
        }else {
            psiType = PsiUtil.extractIterableTypeParameter(psiType, false);
        }
        if(resolvedTypeSet.contains(psiType)) {
            return false;
        }
        return true;
    }

    private static boolean isMapType(PsiType psiType) {
        String presentableText = psiType.getPresentableText();
        List<String> mapList = Arrays.asList("Map","HashMap","LinkedHashMap","JSONObject");
        if(mapList.contains(presentableText)) {
            return true;
        }
        return presentableText.startsWith("Map<") || presentableText.startsWith("HashMap<") || presentableText.startsWith("LinkedHashMap<");
    }

    private boolean containGeneric(String str) {
        for (String generic : FieldUtil.genericList) {
            if (str.contains(generic)) {
                return true;
            }
        }
        return false;
    }

    private RequireAndRange getRequireAndRange(PsiAnnotation[] annotations) {
        if (annotations.length == 0) {
            return RequireAndRange.instance();
        }
        boolean require = false;
        String min = "";
        String max = "";
        String range = "N/A";
        for (PsiAnnotation annotation : annotations) {
            if (isParamRequired(annotation)) {
                require = true;
                break;
            }
        }
        for (PsiAnnotation annotation : annotations) {
            String qualifiedName = annotation.getText();
            if (qualifiedName.contains("Length") || qualifiedName.contains("Range") || qualifiedName.contains("Size")) {
                PsiAnnotationMemberValue minValue = annotation.findAttributeValue("min");
                if (minValue != null) {
                    min = minValue.getText();
                    break;
                }
            }
            if (qualifiedName.contains("Min")) {
                PsiAnnotationMemberValue minValue = annotation.findAttributeValue("value");
                if (minValue != null) {
                    min = minValue.getText();
                    break;
                }
            }
        }
        for (PsiAnnotation annotation : annotations) {
            String qualifiedName = annotation.getText();
            if (qualifiedName.contains("Length") || qualifiedName.contains("Range") || qualifiedName.contains("Size")) {
                PsiAnnotationMemberValue maxValue = annotation.findAttributeValue("max");
                if (maxValue != null) {
                    max = maxValue.getText();
                    break;
                }
            }
            if (qualifiedName.contains("Max")) {
                PsiAnnotationMemberValue maxValue = annotation.findAttributeValue("value");
                if (maxValue != null) {
                    max = maxValue.getText();
                    break;
                }
            }
        }
        if (StringUtils.isNotEmpty(min) || StringUtils.isNotEmpty(max)) {
            range = "[" + min + "," + max + "]";
        }
        return new RequireAndRange(require, range);
    }

    private boolean isParamRequired(PsiAnnotation annotation) {
        String annotationText = annotation.getText();
        if (annotationText.contains(WebAnnotation.RequestParam)) {
            PsiNameValuePair[] psiNameValuePairs = annotation.getParameterList().getAttributes();
            for (PsiNameValuePair psiNameValuePair : psiNameValuePairs) {
                if ("required".equals(psiNameValuePair.getName()) && "false".equals(psiNameValuePair.getLiteralValue())) {
                    return false;
                }
            }
            return true;
        }
        return requiredTexts.contains(annotationText.split("\\(")[0]);
    }

    public boolean hasChildren() {
        return AssertUtils.isNotEmpty(children);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PsiType getPsiType() {
        return psiType;
    }

    public void setPsiType(PsiType psiType) {
        this.psiType = psiType;
    }

    public boolean isRequire() {
        return require;
    }

    public void setRequire(boolean require) {
        this.require = require;
    }

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public TypeEnum getParamType() {
        return paramType;
    }

    public void setParamType(TypeEnum paramType) {
        this.paramType = paramType;
    }

    public List<FieldInfo> getChildren() {
        return children;
    }

    public void setChildren(List<FieldInfo> children) {
        this.children = children;
    }

    public FieldInfo getParent() {
        return parent;
    }

    public void setParent(FieldInfo parent) {
        this.parent = parent;
    }

    public List<PsiAnnotation> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<PsiAnnotation> annotations) {
        this.annotations = annotations;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Map<PsiTypeParameter, PsiType> getGenericTypeMap() {
        return genericTypeMap;
    }

    public void setGenericTypeMap(Map<PsiTypeParameter, PsiType> genericTypeMap) {
        this.genericTypeMap = genericTypeMap;
    }

    public static List<String> getRequiredTexts() {
        return requiredTexts;
    }

    public static void setRequiredTexts(List<String> requiredTexts) {
        FieldInfo.requiredTexts = requiredTexts;
    }

    public ApiGeneratorConfig getConfig() {
        return config;
    }

    public void setConfig(ApiGeneratorConfig config) {
        this.config = config;
    }
}
