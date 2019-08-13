package site.forgus.plugins.apigenerator.config;

import com.intellij.openapi.options.Configurable;
import com.intellij.ui.components.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class ApiGeneratorSetting implements Configurable {

    /**
     * 持久化 配置
     */
    private PersistentConfig config = PersistentConfig.getInstance();

    private PersistentConfig.State state;


    JBTextField dirPathTextField;
    JBTextField prefixTextField;
    OnOffButton cnFileNameButton;

    JBTextField yApiUrlTextField;
    JBTextField tokenTextField;
    JBTextField projectIdTextField;
    JBTextField defaultCatTextField;
    OnOffButton autoCatButton;


    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Api Generator Setting";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        state = config.getState();
        if(state == null) {
            state = new PersistentConfig.State();
        }
        JBTabbedPane jbTabbedPane = new JBTabbedPane();
        GridBagLayout layout = new GridBagLayout();
        //normal setting
        JBPanel normalPanel = new JBPanel(layout);

        normalPanel.add(buildLabel(layout,"Directory Path:"));
        dirPathTextField = buildTextField(layout, state.dirPath);
        normalPanel.add(dirPathTextField);

        normalPanel.add(buildLabel(layout,"Indent Style:"));
        prefixTextField = buildTextField(layout, state.prefix);
        normalPanel.add(prefixTextField);

        normalPanel.add(buildLabel(layout,"Use Chinese as API Doc Name:"));
        cnFileNameButton = buildOnOffButton(layout,state.cnFileName);
        normalPanel.add(cnFileNameButton);

        jbTabbedPane.addTab("Normal Setting",normalPanel);

        //YApi setting
        JBPanel yApiPanel = new JBPanel(layout);

        yApiPanel.add(buildLabel(layout,"YApi Url:"));
        yApiUrlTextField = buildTextField(layout,state.yApiUrl);
        yApiPanel.add(yApiUrlTextField);

        yApiPanel.add(buildLabel(layout,"Token:"));
        tokenTextField = buildTextField(layout,state.token);
        yApiPanel.add(tokenTextField);

        yApiPanel.add(buildLabel(layout,"ProjectId:"));
        projectIdTextField = buildTextField(layout,state.projectId);
        yApiPanel.add(projectIdTextField);

        yApiPanel.add(buildLabel(layout,"Default Category:"));
        defaultCatTextField = buildTextField(layout,state.defaultCat);
        yApiPanel.add(defaultCatTextField);

        yApiPanel.add(buildLabel(layout,"Auto Categorization:"));
        autoCatButton = buildOnOffButton(layout,state.autoCat);
        yApiPanel.add(autoCatButton);

        jbTabbedPane.addTab("YApi Setting",yApiPanel);
        return jbTabbedPane;
    }

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
        return !state.dirPath.equals(dirPathTextField.getText()) ||
                !state.prefix.equals(prefixTextField.getText()) ||
                state.cnFileName != cnFileNameButton.isSelected() ||
                !state.yApiUrl.equals(yApiUrlTextField.getText()) ||
                !state.token.equals(yApiUrlTextField.getText()) ||
                !state.projectId.equals(projectIdTextField.getText()) ||
                !state.defaultCat.equals(defaultCatTextField.getText()) ||
                state.autoCat != autoCatButton.isSelected();
    }

    @Override
    public void apply() {
        config.myState.dirPath = dirPathTextField.getText();
        config.myState.prefix = prefixTextField.getText();
        config.myState.cnFileName = cnFileNameButton.isSelected();
        config.myState.yApiUrl = yApiUrlTextField.getText();
        config.myState.token = tokenTextField.getText();
        config.myState.projectId = projectIdTextField.getText();
        config.myState.defaultCat = defaultCatTextField.getText();
        config.myState.autoCat = autoCatButton.isSelected();
    }

}
