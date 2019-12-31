package site.forgus.plugins.apigenerator.config;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;


@State(name = "ApiGeneratorConfig")
public class ApiGeneratorConfig implements PersistentStateComponent<ApiGeneratorConfig> {

    public Set<String> excludeFieldNames = new HashSet<>();
    public String excludeFields = "serialVersionUID";
    public String dirPath = "";
    public String prefix = "└";
    public Boolean cnFileName = false;
    public Boolean overwrite = true;

    public String yApiServerUrl = "";
    public String projectToken = "";
    public String projectId = "";
    public Boolean autoCat = false;
    public String defaultCat = "api_generator";

    @Nullable
    @Override
    public ApiGeneratorConfig getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull ApiGeneratorConfig state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
