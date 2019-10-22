package site.forgus.plugins.apigenerator.config;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;


@State(name = "ApiGeneratorConfig")
public class ApiGeneratorConfig implements PersistentStateComponent<ApiGeneratorConfig.State> {

    public static class State {
        public Set<String> excludeFieldNames = new HashSet<>();
        public String excludeFields = "serialVersionUID";
        public String dirPath = "";
        public String prefix = "└";
        public Boolean cnFileName = false;

        public String yApiServerUrl = "";
        public String projectToken = "";
        public String projectId = "";
        public Boolean autoCat = false;
        public String defaultCat = "api_generator";
    }

    private static State myState = new State();

    public static ApiGeneratorConfig getInstance(Project project) {
        return ServiceManager.getService(project,ApiGeneratorConfig.class);
    }

    @Nullable
    @Override
    public State getState() {
        return myState;
    }

    @Override
    public void loadState(@NotNull State state) {
        myState = state;
    }


}
