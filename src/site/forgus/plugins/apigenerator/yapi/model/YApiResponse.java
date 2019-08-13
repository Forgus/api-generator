package site.forgus.plugins.apigenerator.yapi.model;

import java.io.Serializable;

public class YApiResponse<T> implements Serializable {

    private static final long serialVersionUID = -8895912143584647957L;

    private Integer errcode;
    private String errmsg;
    private T data;

    public Integer getErrcode() {
        return errcode;
    }

    public void setErrcode(Integer errcode) {
        this.errcode = errcode;
    }

    public String getErrmsg() {
        return errmsg;
    }

    public void setErrmsg(String errmsg) {
        this.errmsg = errmsg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
