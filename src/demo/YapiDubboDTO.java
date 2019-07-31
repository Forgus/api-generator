package demo;

import java.io.Serializable;
public class YapiDubboDTO implements Serializable {
    /**
     * 路径
     */
    private String path;
    /**
     * 请求参数
     */
    private String params;
    /**
     * title
     */
    private String title;
    /**
     * 响应
     */
    private String response;
    /**
     * 描述
     */
    private String desc;


    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }


    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public YapiDubboDTO() {
    }
}
