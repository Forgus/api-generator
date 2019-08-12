package site.forgus.plugins.apigenerator.config;

import com.intellij.openapi.options.Configurable;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.Nls;
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
        jbTabbedPane.addTab("Normal Setting",normalPanel);
        JBPanel yApiPanel = new JBPanel();
        jbTabbedPane.addTab("YApi Setting",yApiPanel);
        return jbTabbedPane;
    }

    private JBLabel buildLabel(GridBagLayout layout,String name) {
        JBLabel jbLabel = new JBLabel(name);
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.fill = GridBagConstraints.WEST;
        labelConstraints.gridwidth = 1;
        layout.setConstraints(jbLabel,labelConstraints);
        return jbLabel;
    }

    private JBTextField buildTextField(GridBagLayout layout,String text) {
        GridBagConstraints textConstraints = new GridBagConstraints();
        textConstraints.fill = GridBagConstraints.BOTH;
        textConstraints.gridwidth = GridBagConstraints.REMAINDER;
        JBTextField textField = new JBTextField(text);
        layout.setConstraints(textField,textConstraints);
        return textField;
    }

    @Override
    public boolean isModified() {
        return !state.dirPath.equals(dirPath.getText()) || !state.prefix.equals(prefix.getText());
    }

    @Override
    public void apply() {
        persistentConfig.myState.dirPath = dirPath.getText();
        persistentConfig.myState.prefix = prefix.getText();
    }

}
