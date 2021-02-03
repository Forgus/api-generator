package site.forgus.plugins.apigenerator.normal;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiType;

/**
 * @author 孤峰
 * @since 2021/02/03
 */
public class FieldFactory {

    public static FieldInfo buildPsiType(Project project, PsiType psiType) {
        FieldInfo fieldInfo = new FieldInfo(project, psiType);
        fieldInfo.setFieldType(FieldType.PSI_TYPE);
        return fieldInfo;
    }

    public static FieldInfo buildField(Project project,String name, PsiType psiType, String desc, PsiAnnotation[] annotations) {
        FieldInfo fieldInfo = new FieldInfo(project, name, psiType, desc, annotations);
        fieldInfo.setFieldType(FieldType.FIELD);
        return fieldInfo;
    }

    public static FieldInfo buildFieldWithParent(Project project,FieldInfo parent,String name, PsiType psiType, String desc, PsiAnnotation[] annotations) {
        FieldInfo fieldInfo = new FieldInfo(project,parent, name, psiType, desc, annotations);
        fieldInfo.setFieldType(FieldType.FIELD);
        return fieldInfo;
    }

}
