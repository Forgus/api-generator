package site.forgus.plugins.apigenerator.normal;

import lombok.Data;

@Data
public class RequireAndRange {

    private boolean require;
    private String range;

    public RequireAndRange(boolean require, String range) {
        this.require = require;
        this.range = range;
    }

    public static RequireAndRange instance() {
        return new RequireAndRange(false, "N/A");
    }
}
