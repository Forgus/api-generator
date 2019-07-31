package demo;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiMethod;

/**
 * Description: 解析请求方式
 * Copyright (c) Department of Research and Development/Beijing
 * All Rights Reserved.
 *
 * @version 1.0 2019年06月21日 16:03
 */
public class RequestMethodUtil {

    private String requestMethod;
    private PsiAnnotation psiAnnotationMethodSemple;

    /**
     * Instantiates a new Request method util.
     *
     */
    public RequestMethodUtil() {

    }

    /**
     * Gets request method.
     *
     * @return the request method
     */
    public String getRequestMethod() {
        return requestMethod;
    }

    /**
     * Gets psi annotation method semple.
     *
     * @return the psi annotation method semple
     */
    public PsiAnnotation getPsiAnnotationMethodSemple() {
        return psiAnnotationMethodSemple;
    }

    /**
     * Invoke request method util.
     *
     * @param psiMethodTarget the psi method target
     * @return the request method util
     */
    public RequestMethodUtil invoke(PsiMethod psiMethodTarget) {
        this.requestMethod = "";
        psiAnnotationMethodSemple = PsiAnnotationSearchUtil.findAnnotation(psiMethodTarget, SpringMVCConstant.GetMapping);
        if (PsiAnnotationSearchUtil.findAnnotation(psiMethodTarget, SpringMVCConstant.GetMapping) != null) {
            requestMethod = "GET";
            return this;
        }
        psiAnnotationMethodSemple = PsiAnnotationSearchUtil.findAnnotation(psiMethodTarget, SpringMVCConstant.PostMapping);
        if (psiAnnotationMethodSemple != null) {
            requestMethod = "POST";
            return this;
        }
        psiAnnotationMethodSemple = PsiAnnotationSearchUtil.findAnnotation(psiMethodTarget, SpringMVCConstant.PutMapping);
        if (psiAnnotationMethodSemple != null) {
            requestMethod = "PUT";
            return this;
        }
        psiAnnotationMethodSemple = PsiAnnotationSearchUtil.findAnnotation(psiMethodTarget, SpringMVCConstant.DeleteMapping);
        if (psiAnnotationMethodSemple != null) {
            requestMethod = "DELETE";
            return this;
        }
        psiAnnotationMethodSemple = PsiAnnotationSearchUtil.findAnnotation(psiMethodTarget, SpringMVCConstant.PatchMapping);
        if (psiAnnotationMethodSemple != null) {
            requestMethod = "PATCH";
            return this;
        }
        return this;
    }


}
