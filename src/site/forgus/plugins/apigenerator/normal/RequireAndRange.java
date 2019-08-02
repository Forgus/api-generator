package site.forgus.plugins.apigenerator.normal;

public class RequireAndRange {

    private boolean require;
    private String range;

    public RequireAndRange(boolean require, String range) {
        this.require = require;
        this.range = range;
    }

    public static RequireAndRange instance() {
        return new RequireAndRange(true,"N/A");
    }

    public boolean isRequire() {
        return require;
    }

    public void setRequire(boolean require) {
        this.require = require;
    }

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }
}
