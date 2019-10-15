package site.forgus.plugins.apigenerator.yapi.model;

import lombok.Data;
import site.forgus.plugins.apigenerator.yapi.enums.ResponseBodyTypeEnum;
import site.forgus.plugins.apigenerator.yapi.enums.YApiInterfaceStatusEnum;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class YApiInterface implements Serializable {
    private static final long serialVersionUID = 1354881275679278513L;

    private String token;
    private List<YApiQuery> req_query = new ArrayList<>();
    private List<YApiHeader> req_headers = new ArrayList<>();
    private String req_body_type;
    private List<YApiForm> req_body_form = new ArrayList<>();
    private Boolean req_body_is_json_schema = false;
    private String req_body_other = "";
    private String title;
    private String catid;
    private String path;
    private String status = YApiInterfaceStatusEnum.UNDONE.getValue();
    private String res_body_type = ResponseBodyTypeEnum.JSON.getValue();
    private Boolean res_body_is_json_schema = false;
    private String res_body = "";
    private Boolean switch_notice = false;
    private String message;
    private String desc = "";
    private String method;
    private List<YApiPathVariable> req_params = new ArrayList<>();
    private String markdown = "";

}
