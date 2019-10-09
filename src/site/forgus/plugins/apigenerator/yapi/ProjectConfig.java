package site.forgus.plugins.apigenerator.yapi;

public class ProjectConfig {

    private String token;
    private Integer projectId;
    private String yApiUrl;
    private String menu = "api_generator";

    private static ProjectConfig projectConfig = new ProjectConfig();

    private ProjectConfig() {

    }

    public static ProjectConfig getInstance() {
        return projectConfig;
    }

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

    public static ProjectConfig getProjectConfig() {
        return projectConfig;
    }

    public static void setProjectConfig(ProjectConfig projectConfig) {
        ProjectConfig.projectConfig = projectConfig;
    }
}
