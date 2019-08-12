package site.forgus.plugins.apigenerator.config;

public class NormalSetting {

    private String dirPath;
    private String prefix;

    public NormalSetting(String dirPath, String prefix) {
        this.dirPath = dirPath;
        this.prefix = prefix;
    }

    public static NormalSetting defaultConfig() {
        return new NormalSetting("","└");
    }


    public String getDirPath() {
        return dirPath;
    }

    public void setDirPath(String dirPath) {
        this.dirPath = dirPath;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

}
