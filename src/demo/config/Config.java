package demo.config;

import java.io.Serializable;
import java.util.Map;

/**
 * Description: Config
 * Copyright (c) Department of Research and Development/Beijing
 * All Rights Reserved.
 *
 * @version 1.0 2019年06月14日 18:42
 */
public class Config implements Serializable {

    /**
     *
     */
    private boolean isSingle;
    private ConfigEntity singleConfig;
    private Map<String, ConfigEntity> multipleConfig;

    /**
     * Is single boolean.
     *
     * @return the boolean
     */
    public boolean isSingle() {
        return isSingle;
    }

    /**
     * Sets single.
     *
     * @param single the single
     */
    public void setSingle(boolean single) {
        isSingle = single;
    }

    /**
     * Gets single config.
     *
     * @return the single config
     */
    public ConfigEntity getSingleConfig() {
        return singleConfig;
    }

    /**
     * Sets single config.
     *
     * @param singleConfig the single config
     */
    public void setSingleConfig(ConfigEntity singleConfig) {
        this.singleConfig = singleConfig;
    }

    /**
     * Gets multiple config.
     *
     * @return the multiple config
     */
    public Map<String, ConfigEntity> getMultipleConfig() {
        return multipleConfig;
    }

    /**
     * Sets multiple config.
     *
     * @param multipleConfig the multiple config
     */
    public void setMultipleConfig(Map<String, ConfigEntity> multipleConfig) {
        this.multipleConfig = multipleConfig;
    }


}
