package site.forgus.plugins.apigenerator.config;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.*;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;
import site.forgus.plugins.apigenerator.util.AssertUtils;
import site.forgus.plugins.apigenerator.yapi.sdk.YApiSdk;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class ApiGeneratorSetting implements Configurable {

    private ApiGeneratorConfig oldState;


    JBTextField dirPathTextField;
    JBTextField prefixTextField;
    JBCheckBox cnFileNameCheckBox;

    JBTextField yApiUrlTextField;
    JBTextField tokenTextField;
    JBLabel projectIdLabel;
    JBTextField defaultCatTextField;
    JBCheckBox autoCatCheckBox;
    JBTextField excludeFields;

    public ApiGeneratorSetting(Project project) {
        oldState = ServiceManager.getService(project,ApiGeneratorConfig.class);
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Api Generator Setting";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        JBTabbedPane jbTabbedPane = new JBTabbedPane();
        GridBagLayout layout = new GridBagLayout();
        //normal setting
        JBPanel normalPanel = new JBPanel(layout);

        normalPanel.add(buildLabel(layout, "Exclude Fields:"));
        excludeFields = new JBTextField(oldState.excludeFields);
        layout.setConstraints(excludeFields, getValueConstraints());
        normalPanel.add(excludeFields);

        normalPanel.add(buildLabel(layout, "Save Directory:"));
        dirPathTextField = buildTextField(layout, oldState.dirPath);
        normalPanel.add(dirPathTextField);

        normalPanel.add(buildLabel(layout, "Indent Style:"));
        prefixTextField = buildTextField(layout, oldState.prefix);
        normalPanel.add(prefixTextField);

        cnFileNameCheckBox = buildJBCheckBox(layout, "Extract filename from doc comments", oldState.cnFileName);
        normalPanel.add(cnFileNameCheckBox);

        jbTabbedPane.addTab("Api Setting", normalPanel);

        //YApi setting
        JBPanel yApiPanel = new JBPanel(layout);

        yApiPanel.add(buildLabel(layout, "YApi server url:"));
        yApiUrlTextField = buildTextField(layout, oldState.yApiServerUrl);
        yApiPanel.add(yApiUrlTextField);

        yApiPanel.add(buildLabel(layout, "Project token:"));
        tokenTextField = buildTextField(layout, oldState.projectToken);
        yApiPanel.add(tokenTextField);

        yApiPanel.add(buildLabel(layout, "Project id:"));
        GridBagConstraints textConstraints = getValueConstraints();
        projectIdLabel = new JBLabel(oldState.projectId);
        layout.setConstraints(projectIdLabel, textConstraints);
        yApiPanel.add(projectIdLabel);

        yApiPanel.add(buildLabel(layout, "Default save category:"));
        defaultCatTextField = buildTextField(layout, oldState.defaultCat);
        yApiPanel.add(defaultCatTextField);

        autoCatCheckBox = buildJBCheckBox(layout, "Classify API automatically", oldState.autoCat);
        yApiPanel.add(autoCatCheckBox);

        jbTabbedPane.addTab("YApi Setting", yApiPanel);
        return jbTabbedPane;
    }

    private JBCheckBox buildJBCheckBox(GridBagLayout layout, String text, boolean selected) {
        JBCheckBox checkBox = new JBCheckBox();
        checkBox.setText(text);
        checkBox.setSelected(selected);
        layout.setConstraints(checkBox, getValueConstraints());
        return checkBox;
    }

    private JBLabel buildLabel(GridBagLayout layout, String name) {
        JBLabel jbLabel = new JBLabel(name);
        layout.setConstraints(jbLabel, getLabelConstraints());
        return jbLabel;
    }

    private JBTextField buildTextField(GridBagLayout layout, String text) {
        JBTextField textField = new JBTextField(text);
        layout.setConstraints(textField, getValueConstraints());
        return textField;
    }

    private GridBagConstraints getLabelConstraints() {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.fill = GridBagConstraints.EAST;
        labelConstraints.gridwidth = 1;
        return labelConstraints;
    }

    private GridBagConstraints getValueConstraints() {
        GridBagConstraints textConstraints = new GridBagConstraints();
        textConstraints.fill = GridBagConstraints.WEST;
        textConstraints.gridwidth = GridBagConstraints.REMAINDER;
        return textConstraints;
    }

    @Override
    public boolean isModified() {
        return !oldState.prefix.equals(prefixTextField.getText()) ||
                oldState.cnFileName != cnFileNameCheckBox.isSelected() ||
                !oldState.yApiServerUrl.equals(yApiUrlTextField.getText()) ||
                !oldState.projectToken.equals(tokenTextField.getText()) ||
                !oldState.projectId.equals(projectIdLabel.getText()) ||
                !oldState.defaultCat.equals(defaultCatTextField.getText()) ||
                oldState.autoCat != autoCatCheckBox.isSelected() ||
                !oldState.dirPath.equals(dirPathTextField.getText()) ||
                !oldState.excludeFields.equals(excludeFields.getText());
    }

    @Override
    public void apply() {
        oldState.excludeFields = excludeFields.getText();
        if (!StringUtils.isEmpty(excludeFields.getText())) {
            String[] split = excludeFields.getText().split(",");
            for (String str : split) {
                oldState.excludeFieldNames.add(str);
            }
        }
        oldState.dirPath = dirPathTextField.getText();
        oldState.prefix = prefixTextField.getText();
        oldState.cnFileName = cnFileNameCheckBox.isSelected();
        oldState.yApiServerUrl = yApiUrlTextField.getText();
        oldState.projectToken = tokenTextField.getText();
        if(AssertUtils.isNotEmpty(yApiUrlTextField.getText()) && AssertUtils.isNotEmpty(tokenTextField.getText())) {
            try {
                oldState.projectId = YApiSdk.getProjectInfo(yApiUrlTextField.getText(), tokenTextField.getText()).get_id().toString();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        oldState.defaultCat = defaultCatTextField.getText();
        oldState.autoCat = autoCatCheckBox.isSelected();
    }

}
