package site.forgus.plugins.apigenerator.yapi.model;

import java.util.Map;

public class YApiInterfaceWrapper {
    private static final long serialVersionUID = 1587901232801702983L;

    private RespCodeEnum respCode;
    private String respMsg;
    private YApiInterface yApiInterface;
    private Map<String,Object> errorInfo;

    public enum RespCodeEnum {
        SUCCESS,FAILED,ERROR
    }

    private YApiInterfaceWrapper() {

    }

    public static YApiInterfaceWrapper success(YApiInterface yApiInterface) {
        YApiInterfaceWrapper wrapper = new YApiInterfaceWrapper();
        wrapper.respCode = RespCodeEnum.SUCCESS;
        wrapper.yApiInterface = yApiInterface;
        return wrapper;
    }

    public static YApiInterfaceWrapper failed(String respMsg) {
        YApiInterfaceWrapper wrapper = new YApiInterfaceWrapper();
        wrapper.respCode = RespCodeEnum.FAILED;
        wrapper.respMsg = respMsg;
        return wrapper;
    }

    public static YApiInterfaceWrapper error(Map<String,Object> errorInfo) {
        YApiInterfaceWrapper wrapper = new YApiInterfaceWrapper();
        wrapper.respCode = RespCodeEnum.ERROR;
        wrapper.errorInfo = errorInfo;
        return wrapper;
    }

    public RespCodeEnum getRespCode() {
        return respCode;
    }

    public void setRespCode(RespCodeEnum respCode) {
        this.respCode = respCode;
    }

    public YApiInterface getYApiInterface() {
        return yApiInterface;
    }

    public void setYApiInterface(YApiInterface yApiInterface) {
        this.yApiInterface = yApiInterface;
    }

    public Map<String, Object> getErrorInfo() {
        return errorInfo;
    }

    public void setErrorInfo(Map<String, Object> errorInfo) {
        this.errorInfo = errorInfo;
    }

    public String getRespMsg() {
        return respMsg;
    }

    public void setRespMsg(String respMsg) {
        this.respMsg = respMsg;
    }
}
