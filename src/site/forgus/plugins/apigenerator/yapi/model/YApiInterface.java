package site.forgus.plugins.apigenerator.yapi.model;

import site.forgus.plugins.apigenerator.yapi.enums.ResponseBodyTypeEnum;
import site.forgus.plugins.apigenerator.yapi.enums.YApiInterfaceStatusEnum;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public List<YApiQuery> getReq_query() {
        return req_query;
    }

    public void setReq_query(List<YApiQuery> req_query) {
        this.req_query = req_query;
    }

    public List<YApiHeader> getReq_headers() {
        return req_headers;
    }

    public void setReq_headers(List<YApiHeader> req_headers) {
        this.req_headers = req_headers;
    }

    public String getReq_body_type() {
        return req_body_type;
    }

    public void setReq_body_type(String req_body_type) {
        this.req_body_type = req_body_type;
    }

    public List<YApiForm> getReq_body_form() {
        return req_body_form;
    }

    public void setReq_body_form(List<YApiForm> req_body_form) {
        this.req_body_form = req_body_form;
    }

    public Boolean getReq_body_is_json_schema() {
        return req_body_is_json_schema;
    }

    public void setReq_body_is_json_schema(Boolean req_body_is_json_schema) {
        this.req_body_is_json_schema = req_body_is_json_schema;
    }

    public String getReq_body_other() {
        return req_body_other;
    }

    public void setReq_body_other(String req_body_other) {
        this.req_body_other = req_body_other;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCatid() {
        return catid;
    }

    public void setCatid(String catid) {
        this.catid = catid;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRes_body_type() {
        return res_body_type;
    }

    public void setRes_body_type(String res_body_type) {
        this.res_body_type = res_body_type;
    }

    public Boolean getRes_body_is_json_schema() {
        return res_body_is_json_schema;
    }

    public void setRes_body_is_json_schema(Boolean res_body_is_json_schema) {
        this.res_body_is_json_schema = res_body_is_json_schema;
    }

    public String getRes_body() {
        return res_body;
    }

    public void setRes_body(String res_body) {
        this.res_body = res_body;
    }

    public Boolean getSwitch_notice() {
        return switch_notice;
    }

    public void setSwitch_notice(Boolean switch_notice) {
        this.switch_notice = switch_notice;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public List<YApiPathVariable> getReq_params() {
        return req_params;
    }

    public void setReq_params(List<YApiPathVariable> req_params) {
        this.req_params = req_params;
    }

    public String getMarkdown() {
        return markdown;
    }

    public void setMarkdown(String markdown) {
        this.markdown = markdown;
    }
}
