package site.forgus.plugins.apigenerator.yapi.model;

import java.util.Map;

public class YApiInterfaceWrapper {
    private static final long serialVersionUID = 1587901232801702983L;

    private RespCodeEnum respCode;
    private YApiInterface yApiInterface;
    private Map<String,String> envInfo;
    private String respMsg;

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

    public static YApiInterfaceWrapper error(Map<String,String> envInfo,String respMsg) {
        YApiInterfaceWrapper wrapper = new YApiInterfaceWrapper();
        wrapper.respCode = RespCodeEnum.ERROR;
        wrapper.envInfo = envInfo;
        wrapper.respMsg = respMsg;
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

    public Map<String, String> getEnvInfo() {
        return envInfo;
    }

    public void setEnvInfo(Map<String, String> envInfo) {
        this.envInfo = envInfo;
    }

    public String getRespMsg() {
        return respMsg;
    }

    public void setRespMsg(String respMsg) {
        this.respMsg = respMsg;
    }
}
