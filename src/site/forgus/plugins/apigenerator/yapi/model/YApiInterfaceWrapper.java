package site.forgus.plugins.apigenerator.yapi.model;

import lombok.Data;

import java.util.Map;

@Data
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

}
