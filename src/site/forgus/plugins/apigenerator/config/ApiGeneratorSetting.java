package site.forgus.plugins.apigenerator.config;

import com.intellij.openapi.options.Configurable;
import com.intellij.ui.components.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class ApiGeneratorSetting implements Configurable {


    /**
     * 持久化 配置
     */
    private PersistentConfig persistentConfig = PersistentConfig.getInstance();

    private PersistentConfig.State state;


    JBTextField dirPath;
    JBTextField prefix;
    OnOffButton onOffButton;


    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Api Generator Setting";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        state = persistentConfig.getState();
        if(state == null) {
            state = new PersistentConfig.State();
        }
        JBTabbedPane jbTabbedPane = new JBTabbedPane();
        GridBagLayout layout = new GridBagLayout();
        JBPanel normalPanel = new JBPanel(layout);

        normalPanel.add(buildLabel(layout,"dirPath:"));
        dirPath = buildTextField(layout, state.dirPath);
        normalPanel.add(dirPath);

        normalPanel.add(buildLabel(layout,"prefix:"));
        prefix = buildTextField(layout, state.prefix);
        normalPanel.add(prefix);

        normalPanel.add(buildLabel(layout,"chFileName:"));
        onOffButton = buildOnOffButton(layout,state.cnFileName);
        normalPanel.add(onOffButton);

        jbTabbedPane.addTab("Normal Setting",normalPanel);
        JBPanel yApiPanel = new JBPanel();
        jbTabbedPane.addTab("YApi Setting",yApiPanel);
        return jbTabbedPane;
    }

    @NotNull
    private OnOffButton buildOnOffButton(GridBagLayout layout,boolean selected) {
        OnOffButton onOffButton = new OnOffButton();
        onOffButton.setOnText("YES");
        onOffButton.setOffText("NO");
        onOffButton.setSelected(selected);
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.fill = GridBagConstraints.WEST;
        labelConstraints.gridwidth =  GridBagConstraints.REMAINDER;
        layout.setConstraints(onOffButton,labelConstraints);
        return onOffButton;
    }

    private JBLabel buildLabel(GridBagLayout layout,String name) {
        JBLabel jbLabel = new JBLabel(name);
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.fill = GridBagConstraints.EAST;
        labelConstraints.gridwidth = 1;
        layout.setConstraints(jbLabel,labelConstraints);
        return jbLabel;
    }

    private JBTextField buildTextField(GridBagLayout layout,String text) {
        GridBagConstraints textConstraints = new GridBagConstraints();
        textConstraints.fill = GridBagConstraints.WEST;
        textConstraints.gridwidth = GridBagConstraints.REMAINDER;
        JBTextField textField = new JBTextField(text);
        layout.setConstraints(textField,textConstraints);
        return textField;
    }

    @Override
    public boolean isModified() {
        return !state.dirPath.equals(dirPath.getText()) ||
                !state.prefix.equals(prefix.getText()) ||
                state.cnFileName != onOffButton.isSelected();
    }

    @Override
    public void apply() {
        persistentConfig.myState.dirPath = dirPath.getText();
        persistentConfig.myState.prefix = prefix.getText();
        persistentConfig.myState.cnFileName = onOffButton.isSelected();
    }

}
