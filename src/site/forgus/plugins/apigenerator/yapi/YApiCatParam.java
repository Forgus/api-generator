package site.forgus.plugins.apigenerator.yapi;

import java.io.Serializable;

/**
 * 新增菜单
 *
 * @author chengsheng@qbb6.com
 * @date 2019/2/1 10:44 AM
 */
public class YApiCatParam implements Serializable {
    private static final long serialVersionUID = 3501505521564701448L;
    /**
     * 名字
     */
    private String name;
    /**
     * token
     */
    private String token;

    public YApiCatParam(String name, String token) {
        this.name = name;
        this.token = token;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
