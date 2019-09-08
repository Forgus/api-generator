package site.forgus.plugins.apigenerator.normal;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FieldInfo {

    private String name;
    private Object value;
    private PsiType psiType;
    private boolean require;
    private String range;
    private String desc;
    private boolean hasChildren;
    private ParamTypeEnum paramType;
    private List<FieldInfo> children;
    private List<PsiAnnotation> annotations;

    private FieldInfo(String name, PsiType psiType, boolean require, String range, String desc) {
        this.name = name == null ? "N/A" : name;
        this.psiType = psiType;
        this.require = require;
        this.range = range == null ? "N/A" : range;
        this.desc = desc == null ? "N/A" : desc;
        String presentableText = psiType.getPresentableText();
        if(NormalTypes.isNormalType(presentableText)) {
            paramType = ParamTypeEnum.LITERAL;
            value = NormalTypes.normalTypes.get(presentableText);
        }else if(presentableText.contains("<") && (presentableText.startsWith("List") || presentableText.startsWith("Set"))) {
            paramType = ParamTypeEnum.ARRAY;
        }else {
            paramType = ParamTypeEnum.OBJECT;
        }
    }

    public static FieldInfo normal(String name,PsiType type,String desc,List<FieldInfo> children,PsiAnnotation[] annotations) {
        RequireAndRange requireAndRange = getRequireAndRange(annotations);
        FieldInfo fieldInfo = new FieldInfo(name, type, requireAndRange.isRequire(), requireAndRange.getRange(), desc);
        fieldInfo.setChildren(children);
        fieldInfo.setHasChildren(CollectionUtils.isNotEmpty(children));
        fieldInfo.setAnnotations(Arrays.asList(annotations));
        return fieldInfo;
    }

    public static FieldInfo resolve(String name, PsiType type, String desc, Project project, PsiAnnotation[] annotations) {
        return normal(name,type,desc,listFieldInfos(project, type), annotations);
    }

    public static List<FieldInfo> listFieldInfos(Project project, PsiType psiType) {
        if(psiType == null) {
            return new ArrayList<>();
        }
        List<FieldInfo> fieldInfos = new ArrayList<>();
        if(psiType instanceof PsiClassReferenceType) {
            PsiClass psiClass = PsiUtil.resolveClassInType(psiType);
            String typeName = psiType.getPresentableText();
            if (NormalTypes.isNormalType(typeName)) {
                return new ArrayList<>();
            }
            if (typeName.startsWith("List") || typeName.startsWith("Set")) {
                PsiType iterableType = PsiUtil.extractIterableTypeParameter(psiType, false);
                if (iterableType == null || NormalTypes.isNormalType(iterableType.getPresentableText())) {
                    return new ArrayList<>();
                }
                PsiClass iterableClass = PsiUtil.resolveClassInType(iterableType);
                if (iterableClass == null) {
                    return listFieldInfos(project,iterableType);
                }
                for (PsiField psiField : iterableClass.getAllFields()) {
                    resolveFields(project, fieldInfos, psiField);
                }
                return fieldInfos;
            }
            if(typeName.startsWith("Map")) {
                fieldInfos.add(FieldInfo.child(typeName, psiType, "", new PsiAnnotation[0]));
                return fieldInfos;
            }
            if (typeName.contains("<")) {
                PsiClass outerClass = PsiUtil.resolveClassInType(psiType);
                PsiType innerType = PsiUtil.substituteTypeParameter(psiType, outerClass, 0, false);
                PsiElementFactory elementFactory = JavaPsiFacade.getInstance(project).getElementFactory();
                for (PsiField outField : outerClass.getAllFields()) {
                    if (NormalTypes.genericList.contains(outField.getType().getPresentableText())) {
                        resolveFields(project, fieldInfos, elementFactory.createField(outField.getName(), innerType));
                    } else {
                        resolveFields(project, fieldInfos, outField);
                    }
                }
                return fieldInfos;
            }
            if (psiClass == null) {
                return new ArrayList<>();
            }
            for (PsiField psiField : psiClass.getAllFields()) {
                resolveFields(project, fieldInfos, psiField);
            }
            return fieldInfos;
        }
        return new ArrayList<>();
    }

    private static void resolveFields(Project project, List<FieldInfo> fieldInfos, PsiField psiField) {
        String name = psiField.getName();
        PsiType type = psiField.getType();
        String typeName = type.getPresentableText();
        if (NormalTypes.genericList.contains(typeName) || type instanceof PsiEnumConstant) {
            return;
        }
        String desc = DesUtil.getFiledDesc(psiField.getDocComment()).replace("@see", "见");
        if (NormalTypes.isNormalType(typeName)) {
            fieldInfos.add(FieldInfo.child(name, type, desc,psiField.getAnnotations()));
            return;
        }
        if (typeName.startsWith("List") || typeName.startsWith("Set")) {
            //list type
            PsiType iterableType = PsiUtil.extractIterableTypeParameter(type, false);
            if(iterableType == null || NormalTypes.isNormalType(iterableType.getPresentableText())) {
                fieldInfos.add(FieldInfo.child(name, type, desc,psiField.getAnnotations()));
                return;
            }
            fieldInfos.add(FieldInfo.parent(name, type, desc, listFieldInfos(project,iterableType),psiField.getAnnotations()));
            return;
        }
        if (typeName.startsWith("Map")) {
            PsiType keyType = PsiUtil.substituteTypeParameter(type, PsiUtil.resolveClassInType(type), 0, false);
            PsiType valueType = PsiUtil.substituteTypeParameter(type, PsiUtil.resolveClassInType(type), 1, false);
            String containingClassName = psiField.getContainingClass().getName();
            String valueClassName = valueType.getPresentableText();
            if(containingClassName.equals(valueClassName)) {
                fieldInfos.add(child(name,type,desc,new PsiAnnotation[0]));
                return;
            }
            List<FieldInfo> children = new ArrayList<>();
            children.add(normal(keyType.getPresentableText(),valueType,"",listFieldInfos(project,valueType),new PsiAnnotation[0]));
            fieldInfos.add(parent(name, type, desc, children,psiField.getAnnotations()));
            return;
        }
        if (typeName.contains("<")) {
            PsiClass outerClass = PsiUtil.resolveClassInType(type);
            PsiType innerType = PsiUtil.substituteTypeParameter(type, outerClass, 0, false);
            PsiElementFactory elementFactory = JavaPsiFacade.getInstance(project).getElementFactory();
            for (PsiField outField : outerClass.getAllFields()) {
                if (NormalTypes.genericList.contains(outField.getType().getPresentableText())) {
                    resolveFields(project, fieldInfos, elementFactory.createField(outField.getName(), innerType));
                } else {
                    resolveFields(project, fieldInfos, outField);
                }
            }
            return;
        }
        PsiClass containingClass = psiField.getContainingClass();
        PsiClass psiClass = PsiUtil.resolveClassInType(type);
        if(psiClass.isEnum() || containingClass.getName().equals(psiClass.getName())) {
            fieldInfos.add(FieldInfo.normal(name,type,desc,new ArrayList<>(),psiField.getAnnotations()));
            return;
        }
        fieldInfos.add(FieldInfo.parent(name, type, desc, listFieldInfos(psiClass, project),psiField.getAnnotations()));
    }

    public static List<FieldInfo> listFieldInfos(PsiClass psiClass, Project project) {
        List<FieldInfo> fieldInfos = new ArrayList<>();
        for (PsiField psiField : psiClass.getAllFields()) {
            resolveFields(project, fieldInfos, psiField);
        }
        return fieldInfos;
    }

    public static FieldInfo child(String name, PsiType type, String desc,PsiAnnotation[] annotations) {
        RequireAndRange requireAndRange = getRequireAndRange(annotations);
        return new FieldInfo(name,type,requireAndRange.isRequire(),requireAndRange.getRange(),desc);
    }

    public static FieldInfo parent(String name, PsiType type, String desc,List<FieldInfo> children,PsiAnnotation[] annotations) {
        RequireAndRange requireAndRange = getRequireAndRange(annotations);
        FieldInfo fieldInfo = new FieldInfo(name, type, requireAndRange.isRequire(),requireAndRange.getRange(), desc);
        fieldInfo.setHasChildren(true);
        fieldInfo.setChildren(children);
        return fieldInfo;
    }

    private static RequireAndRange getRequireAndRange(PsiAnnotation[] annotations) {
        if(annotations.length == 0) {
            return RequireAndRange.instance();
        }
        boolean require = false;
        String min = "";
        String max = "";
        String range = "N/A";
        for (PsiAnnotation annotation : annotations) {
            String qualifiedName = annotation.getText();
            if (qualifiedName.contains("NotNull") || qualifiedName.contains("NotBlank") || qualifiedName.contains("NotEmpty")) {
                require = true;
            }
            if (qualifiedName.contains("Length") || qualifiedName.contains("Range")) {
                PsiAnnotationMemberValue maxValue = annotation.findAttributeValue("max");
                PsiAnnotationMemberValue minValue = annotation.findAttributeValue("min");
                if (maxValue != null) {
                    max = maxValue.getText();
                }
                if (minValue != null) {
                    min = minValue.getText();
                }
            }
            if(qualifiedName.contains("Min")) {
                PsiAnnotationMemberValue minValue = annotation.findAttributeValue("value");
                if(minValue != null) {
                    min = minValue.getText();
                }
            }
            if(qualifiedName.contains("Max")) {
                PsiAnnotationMemberValue maxValue = annotation.findAttributeValue("value");
                if(maxValue != null) {
                    max = maxValue.getText();
                }
            }
        }
        if (StringUtils.isNotEmpty(min) || StringUtils.isNotEmpty(max)) {
            range = "[" + min + "," + max + "]";
        }
        return new RequireAndRange(require, range);
    }

    public List<PsiAnnotation> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<PsiAnnotation> annotations) {
        this.annotations = annotations;
    }

    public ParamTypeEnum getParamType() {
        return paramType;
    }

    public void setParamType(ParamTypeEnum paramType) {
        this.paramType = paramType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
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

    public boolean isHasChildren() {
        return hasChildren;
    }

    public void setHasChildren(boolean hasChildren) {
        this.hasChildren = hasChildren;
    }

    public List<FieldInfo> getChildren() {
        return children;
    }

    public void setChildren(List<FieldInfo> children) {
        this.children = children;
    }
}
