package site.forgus.plugins.apigenerator.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@State(name = "ApiGeneratorConfig", storages = {@com.intellij.openapi.components.Storage(value = "$APP_CONFIG$/ApiGeneratorConfig.xml")})
public class PersistentConfig implements PersistentStateComponent<PersistentConfig.State> {

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
    private State myState = new State();

    /**
     * 服务管理器获取实例
     *
     * @return PersistentState instance
     */
    public static PersistentConfig getInstance() {
        return ServiceManager.getService(PersistentConfig.class);
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
