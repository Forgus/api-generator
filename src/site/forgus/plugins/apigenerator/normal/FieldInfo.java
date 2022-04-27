package site.forgus.plugins.apigenerator.normal;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.*;
import com.intellij.psi.impl.compiled.ClsClassImpl;
import com.intellij.psi.impl.java.stubs.impl.PsiJavaFileStubImpl;
import com.intellij.psi.impl.source.PsiJavaFileImpl;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import site.forgus.plugins.apigenerator.config.ApiGeneratorConfig;
import site.forgus.plugins.apigenerator.constant.TypeEnum;
import site.forgus.plugins.apigenerator.util.AssertUtils;
import site.forgus.plugins.apigenerator.util.DesUtil;
import site.forgus.plugins.apigenerator.util.FieldUtil;

import java.util.*;

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
                children = null;
                return;
            }
            //兼容泛型
            PsiType realType = resolveGeneric(psiType);
            PsiClass psiClass = PsiUtil.resolveClassInType(realType);
            if (psiClass == null) {
                return;
            }
            //兼容第三方jar包
            if (psiClass instanceof ClsClassImpl){
                StubElement parentStub = ((ClsClassImpl) psiClass).getStub().getParentStub();
                if(parentStub instanceof  PsiJavaFileStubImpl) {
                    String sourcePath = ((PsiJavaFileStubImpl) parentStub)
                            .getPsi().getViewProvider().getVirtualFile().toString()
                            .replace(".jar!", "-sources.jar!");
                    sourcePath = sourcePath.substring(0, sourcePath.length() - 5)+"java";
                    VirtualFile virtualFile =
                            VirtualFileManager.getInstance().findFileByUrl(sourcePath);
                    if(virtualFile != null) {
                        FileViewProvider fileViewProvider = new SingleRootFileViewProvider(PsiManager.getInstance(project), virtualFile);
                        PsiFile psiFile1 = new PsiJavaFileImpl(fileViewProvider);
                        psiClass = PsiTreeUtil.findChildOfAnyType(psiFile1.getOriginalElement(), PsiClass.class);
                    }
                }
            }
            for (PsiField psiField : psiClass.getAllFields()) {
                if (config.getState().excludeFields.contains(psiField.getName())) {
                    continue;
                }
                if(FieldUtil.isStaticField(psiField)) {
                    continue;
                }
                if(FieldUtil.isIgnoredField(psiField)) {
                    continue;
                }
                PsiType fieldType = psiField.getType();
                //兼容泛型
                PsiType realFieldType = resolveGeneric(fieldType);
                FieldInfo fieldInfo = FieldFactory.buildFieldWithParent(project,this,psiField.getName(), realFieldType,DesUtil.getDescription(psiField), psiField.getAnnotations());
                children.add(fieldInfo);
            }
        }
    }

}
