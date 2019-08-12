package demo.config;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.JBUI;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Description: YApiSetting
 * Copyright (c) Department of Research and Development/Beijing
 * All Rights Reserved.
 *
 * @version 1.0 2019年06月14日 17:32
 */
public class YApiSetting implements Configurable {



    /**
     * 配置 json
     */
    private JTextArea config;

    /**
     * 持久化 配置
     */
    private PersistentState persistentState = PersistentState.getInstance();


    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "YApiSetting";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        JPanel yApi = new JPanel();
        yApi.setLayout(new GridLayoutManager(13, 3, JBUI.emptyInsets(), -1, -1));
        yApi.add(new JLabel("字段说明"), new GridConstraints(1, 0, 10, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        yApi.add(new JLabel("config"), new GridConstraints(11, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        yApi.add(new JLabel("。。。"), new GridConstraints(12, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        config = new JTextArea();
        config.setText(StringUtils.isNotBlank(persistentState.getConfig()) ? persistentState.getConfig() : this.getDefaultConfig());
        yApi.add(config, new GridConstraints(11, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        yApi.add(new JLabel("isSingle : 是否单模块配置，true使用singleConfig，false则使用multipleConfig"), new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        yApi.add(new JLabel("项目id 获取方式 ：点击项目，查看url 中project 后面的数字为项目id http://127.0.0.1:3000/project/72/interface/api"), new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        yApi.add(new JLabel("token 获取方式 ： 打开yapi ->具体项目->设置->token 配置"), new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        yApi.add(new JLabel("multipleConfig ： 多模块配置， key = 项目名"), new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        yApi.add(new JLabel("singleConfig ： 单模块配置"), new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        yApi.add(new JLabel("projectType 填写方式： 根据你要上传的接口类型决定，如果为dubbo 接口就填dubbo ，如果是api 接口就填api "), new GridConstraints(8, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        yApi.add(new JLabel("yapiUrl 获取方式：部署的yapi 地址"), new GridConstraints(7, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        yApi.add(new JLabel("menu 接口所在的分类"), new GridConstraints(9, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        yApi.add(new JLabel(""), new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        return yApi;
    }

    @Override
    public boolean isModified() {
        if (StringUtils.isBlank(config.getText())) {
            return false;
        }
        if (StringUtils.isBlank(persistentState.getConfig())) {
            return true;
        }
        return persistentState.getConfig().hashCode() != config.getText().hashCode();
    }

    @Override
    public void apply() {
        persistentState.setConfig(config.getText());
    }

    /**
     * Gets default config.
     *
     * @return the default config
     */
    private String getDefaultConfig() {
        return "{\n" +
                "  \"isSingle\": true,\n" +
                "  \"singleConfig\": {\n" +
                "    \"projectToken\": \"f10526011aa0231a6a7fd6b9ac09dea8896d2e0bc072524e5ba1ce61ef232503\",\n" +
                "    \"projectId\": \"82\",\n" +
                "    \"yApiUrl\": \"http://10.0.60.8:3300\",\n" +
                "    \"menu\": \"api\",\n" +
                "    \"projectType\": \"api\"\n" +
                "  },\n" +
                "  \"multipleConfig\": {\n" +
                "    \"llb-api\": {\n" +
                "      \"projectToken\": \"\",\n" +
                "      \"projectId\": \"\",\n" +
                "      \"yApiUrl\": \"\",\n" +
                "      \"menu\": \"api\",\n" +
                "      \"projectType\": \"\"\n" +
                "    },\n" +
                "    \"llb-admin-api\": {\n" +
                "      \"projectToken\": \"\",\n" +
                "      \"projectId\": \"\",\n" +
                "      \"yApiUrl\": \"\",\n" +
                "      \"menu\": \"api\",\n" +
                "      \"projectType\": \"\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }

}
