package demo;

import java.io.Serializable;
import java.util.List;

/**
 * yApi 保存请求参数
 */
public class YApiSaveParam implements Serializable {
    /**
     * 项目 token  唯一标识
     */
    private String token;

    /**
     * 请求参数
     */
    private List req_query;
    /**
     * header
     */
    private List req_headers;
    /**
     * 请求参数 form 类型
     */
    private List req_body_form;
    /**
     * 标题
     */
    private String title;
    /**
     * 分类id
     */
    private String catid;
    /**
     * 请求数据类型   raw,form,json
     */
    private String req_body_type = "json";
    /**
     * 请求数据body
     */
    private String req_body_other;
    /**
     * 请求参数body 是否为json_schema
     */
    private boolean req_body_is_json_schema;
    /**
     * 路径
     */
    private String path;
    /**
     * 状态 undone,默认done
     */
    private String status = "done";
    /**
     * 返回参数类型  json
     */
    private String res_body_type = "json";

    /**
     * 返回参数
     */
    private String res_body;

    /**
     * 返回参数是否为json_schema
     */
    private boolean res_body_is_json_schema = true;

    /**
     * 创建的用户名
     */
    private Integer edit_uid = 11;
    /**
     * 用户名称
     */
    private String username;

    /**
     * 邮件开关
     */
    private boolean switch_notice;

    private String message = " ";
    /**
     * 文档描述
     */
    private String desc = "<h3>请补充描述</h3>";

    /**
     * 请求方式
     */
    private String method = "POST";
    /**
     * 请求参数
     */
    private List req_params;


    private String id;
    /**
     * 项目id
     */
    private Integer projectId;

    /**
     * yapi 地址
     */
    private String yapiUrl;
    /**
     * 菜单名称
     */
    private String menu;


    /**
     * Gets token.
     *
     * @return the token
     */
    public String getToken() {
        return token;
    }

    /**
     * Sets token.
     *
     * @param token the token
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * Gets req query.
     *
     * @return the req query
     */
    public List getReq_query() {
        return req_query;
    }

    /**
     * Sets req query.
     *
     * @param req_query the req query
     */
    public void setReq_query(List req_query) {
        this.req_query = req_query;
    }

    /**
     * Gets req headers.
     *
     * @return the req headers
     */
    public List getReq_headers() {
        return req_headers;
    }

    /**
     * Sets req headers.
     *
     * @param req_headers the req headers
     */
    public void setReq_headers(List req_headers) {
        this.req_headers = req_headers;
    }

    /**
     * Gets req body form.
     *
     * @return the req body form
     */
    public List getReq_body_form() {
        return req_body_form;
    }

    /**
     * Sets req body form.
     *
     * @param req_body_form the req body form
     */
    public void setReq_body_form(List req_body_form) {
        this.req_body_form = req_body_form;
    }

    /**
     * Gets title.
     *
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets title.
     *
     * @param title the title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Gets catid.
     *
     * @return the catid
     */
    public String getCatid() {
        return catid;
    }

    /**
     * Sets catid.
     *
     * @param catid the catid
     */
    public void setCatid(String catid) {
        this.catid = catid;
    }

    /**
     * Gets path.
     *
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets path.
     *
     * @param path the path
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Gets status.
     *
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets status.
     *
     * @param status the status
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Gets res body type.
     *
     * @return the res body type
     */
    public String getRes_body_type() {
        return res_body_type;
    }

    /**
     * Sets res body type.
     *
     * @param res_body_type the res body type
     */
    public void setRes_body_type(String res_body_type) {
        this.res_body_type = res_body_type;
    }

    /**
     * Gets res body.
     *
     * @return the res body
     */
    public String getRes_body() {
        return res_body;
    }

    /**
     * Sets res body.
     *
     * @param res_body the res body
     */
    public void setRes_body(String res_body) {
        this.res_body = res_body;
    }

    /**
     * Is switch notice boolean.
     *
     * @return the boolean
     */
    public boolean isSwitch_notice() {
        return switch_notice;
    }

    /**
     * Sets switch notice.
     *
     * @param switch_notice the switch notice
     */
    public void setSwitch_notice(boolean switch_notice) {
        this.switch_notice = switch_notice;
    }

    /**
     * Gets message.
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets message.
     *
     * @param message the message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Gets desc.
     *
     * @return the desc
     */
    public String getDesc() {
        return desc;
    }

    /**
     * Sets desc.
     *
     * @param desc the desc
     */
    public void setDesc(String desc) {
        this.desc = desc;
    }

    /**
     * Gets method.
     *
     * @return the method
     */
    public String getMethod() {
        return method;
    }

    /**
     * Sets method.
     *
     * @param method the method
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * Gets req params.
     *
     * @return the req params
     */
    public List getReq_params() {
        return req_params;
    }

    /**
     * Sets req params.
     *
     * @param req_params the req params
     */
    public void setReq_params(List req_params) {
        this.req_params = req_params;
    }

    /**
     * Gets id.
     *
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets id.
     *
     * @param id the id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets req body type.
     *
     * @return the req body type
     */
    public String getReq_body_type() {
        return req_body_type;
    }

    /**
     * Sets req body type.
     *
     * @param req_body_type the req body type
     */
    public void setReq_body_type(String req_body_type) {
        this.req_body_type = req_body_type;
    }

    /**
     * Gets req body other.
     *
     * @return the req body other
     */
    public String getReq_body_other() {
        return req_body_other;
    }

    /**
     * Sets req body other.
     *
     * @param req_body_other the req body other
     */
    public void setReq_body_other(String req_body_other) {
        this.req_body_other = req_body_other;
    }

    /**
     * Is req body is json schema boolean.
     *
     * @return the boolean
     */
    public boolean isReq_body_is_json_schema() {
        return req_body_is_json_schema;
    }

    /**
     * Sets req body is json schema.
     *
     * @param req_body_is_json_schema the req body is json schema
     */
    public void setReq_body_is_json_schema(boolean req_body_is_json_schema) {
        this.req_body_is_json_schema = req_body_is_json_schema;
    }

    /**
     * Is res body is json schema boolean.
     *
     * @return the boolean
     */
    public boolean isRes_body_is_json_schema() {
        return res_body_is_json_schema;
    }

    /**
     * Sets res body is json schema.
     *
     * @param res_body_is_json_schema the res body is json schema
     */
    public void setRes_body_is_json_schema(boolean res_body_is_json_schema) {
        this.res_body_is_json_schema = res_body_is_json_schema;
    }

    /**
     * Gets edit uid.
     *
     * @return the edit uid
     */
    public Integer getEdit_uid() {
        return edit_uid;
    }

    /**
     * Sets edit uid.
     *
     * @param edit_uid the edit uid
     */
    public void setEdit_uid(Integer edit_uid) {
        this.edit_uid = edit_uid;
    }

    /**
     * Gets project id.
     *
     * @return the project id
     */
    public Integer getProjectId() {
        return projectId;
    }

    /**
     * Sets project id.
     *
     * @param projectId the project id
     */
    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    /**
     * Gets yapi url.
     *
     * @return the yapi url
     */
    public String getYapiUrl() {
        return yapiUrl;
    }

    /**
     * Sets yapi url.
     *
     * @param yapiUrl the yapi url
     */
    public void setYapiUrl(String yapiUrl) {
        this.yapiUrl = yapiUrl;
    }

    /**
     * Gets username.
     *
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets username.
     *
     * @param username the username
     */
    public void setUsername(String username) {
        this.username = username;
    }


    /**
     * Gets menu.
     *
     * @return the menu
     */
    public String getMenu() {
        return menu;
    }

    /**
     * Sets menu.
     *
     * @param menu the menu
     */
    public void setMenu(String menu) {
        this.menu = menu;
    }

    /**
     * Instantiates a new Y api save param.
     */
    public YApiSaveParam() {
    }

    /**
     * Instantiates a new Y api save param.
     *
     * @param token          the token
     * @param title          the title
     * @param path           the path
     * @param req_body_other the req body other
     * @param res_body       the res body
     * @param projectId      the project id
     * @param yapiUrl        the yapi url
     * @param desc           the desc
     */
    public YApiSaveParam(String token, String title, String path, String req_body_other, String res_body, Integer projectId, String yapiUrl, String desc) {
        this.token = token;
        this.title = title;
        this.path = path;
        this.res_body = res_body;
        this.req_body_other = req_body_other;
        this.projectId = projectId;
        this.yapiUrl = yapiUrl;
        this.desc = desc;
    }


    /**
     * Instantiates a new Y api save param.
     *
     * @param token                   the token
     * @param title                   the title
     * @param path                    the path
     * @param req_query               the req query
     * @param req_body_other          the req body other
     * @param res_body                the res body
     * @param projectId               the project id
     * @param yapiUrl                 the yapi url
     * @param req_body_is_json_schema the req body is json schema
     * @param method                  the method
     * @param desc                    the desc
     * @param header                  the header
     */
    public YApiSaveParam(String token, String title, String path, List req_query, String req_body_other, String res_body, Integer projectId, String yapiUrl, boolean req_body_is_json_schema, String method, String desc, List header) {
        this.token = token;
        this.title = title;
        this.path = path;
        this.req_query = req_query;
        this.res_body = res_body;
        this.req_body_other = req_body_other;
        this.projectId = projectId;
        this.yapiUrl = yapiUrl;
        this.req_body_is_json_schema = req_body_is_json_schema;
        this.method = method;
        this.desc = desc;
        this.req_headers = header;
    }


}
