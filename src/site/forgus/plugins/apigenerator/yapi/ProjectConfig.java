package site.forgus.plugins.apigenerator.yapi;

public class ProjectConfig {

    private String token = "6b7971300cd908c425588d957b9700eed83c2f06347bbc3e9f8ca549d929348d";
    private Integer projectId = 241;
    private String yApiUrl = "http://yapi.cai-inc.com";
    private String menu = "api_generator";

    private static ProjectConfig projectConfig = new ProjectConfig();

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public String getyApiUrl() {
        return yApiUrl;
    }

    public void setyApiUrl(String yApiUrl) {
        this.yApiUrl = yApiUrl;
    }

    public String getMenu() {
        return menu;
    }

    public void setMenu(String menu) {
        this.menu = menu;
    }

    private ProjectConfig() {

    }

    public static ProjectConfig getInstance() {
        return projectConfig;
    }

}
