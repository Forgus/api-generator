package site.forgus.plugins.apigenerator.normal;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtil;
import site.forgus.plugins.apigenerator.config.ApiGeneratorConfig;
import site.forgus.plugins.apigenerator.constant.TypeEnum;
import site.forgus.plugins.apigenerator.util.AssertUtils;
import site.forgus.plugins.apigenerator.util.DesUtil;
import site.forgus.plugins.apigenerator.util.FieldUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FieldInfo {

    String name;
    PsiType psiType;
    boolean require;
    String range;
    String desc = "";
    TypeEnum paramType;
    FieldType fieldType;
    List<FieldInfo> children = new ArrayList<>();
    FieldInfo parent;
    List<PsiAnnotation> annotations;
    Project project;
    Map<PsiTypeParameter, PsiType> genericTypeMap;

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

    protected ApiGeneratorConfig config;

    /**
     * 根据泛型获取对应的PsiType
     * @param psiType
     * @return
     */
    private PsiType resolveGeneric(PsiType psiType){
        if(null == psiType){
            return null;
        }
        Map<PsiTypeParameter, PsiType> map;
        if(this.parent != null){
            map = this.parent.genericTypeMap;
        }else {
            map = this.genericTypeMap;
        }
        if(null != map){
            for (PsiTypeParameter psiTypeParameter : map.keySet()) {
                if(Objects.equals(psiTypeParameter.getName(), psiType.getPresentableText())){
                    return map.get(psiTypeParameter);
                }
            }
        }
        return psiType;
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

    public boolean isRequire() {
        return require;
    }

    public String getRange() {
        return range;
    }

    public String getDesc() {
        return desc;
    }

    public TypeEnum getParamType() {
        return paramType;
    }

    public List<FieldInfo> getChildren() {
        return children;
    }

    public List<PsiAnnotation> getAnnotations() {
        return annotations;
    }

    public void resolveChildren() {
        PsiType psiType = this.psiType;
        if (FieldUtil.isNormalType(psiType.getPresentableText())) {
            //基础类或基础包装类没有子域
            return;
        }
        //如果是数组
        if(psiType instanceof PsiArrayType) {
            PsiType componentType = ((PsiArrayType) psiType).getComponentType();
            if (FieldUtil.isNormalType(componentType.getPresentableText()) || FieldUtil.isMapType(componentType)) {
                return;
            }
            FieldInfo fieldInfo = FieldFactory.buildPsiType(project,componentType);
            children =  fieldInfo.children;
            return;
        }
        if (psiType instanceof PsiClassType) {
            //如果是集合类型
            if (FieldUtil.isCollectionType(psiType)) {
                PsiType iterableType = PsiUtil.extractIterableTypeParameter(psiType, false);
                if (iterableType == null || FieldUtil.isNormalType(iterableType.getPresentableText()) || FieldUtil.isMapType(iterableType)) {
                    return;
                }
                //兼容泛型
                PsiType realType = resolveGeneric(iterableType);
                FieldInfo fieldInfo = FieldFactory.buildPsiType(project,realType);
                children = fieldInfo.children;
                return ;
            }
            String typeName = psiType.getPresentableText();
            if (typeName.startsWith("Map")) {
                return;
            }
            //兼容泛型
            PsiType realType = resolveGeneric(psiType);
            PsiClass psiClass = PsiUtil.resolveClassInType(realType);
            if (psiClass == null) {
                return;
            }
            for (PsiField psiField : psiClass.getAllFields()) {
                if (config.getState().excludeFields.contains(psiField.getName())) {
                    continue;
                }
                PsiType fieldType = psiField.getType();
                //兼容泛型
                PsiType realFieldType = resolveGeneric(fieldType);
                FieldInfo fieldInfo = FieldFactory.buildFieldWithParent(project,this,psiField.getName(), realFieldType,DesUtil.getDescription(psiField), psiField.getAnnotations());
                children.add(fieldInfo);
            }
            return;
        }
        return;
    }
}
