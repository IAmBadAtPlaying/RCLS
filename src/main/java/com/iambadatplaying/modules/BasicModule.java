package com.iambadatplaying.modules;

import com.google.gson.JsonElement;

import java.nio.file.Path;
import java.util.Optional;

public interface BasicModule {
    String CONFIG_NAME = "config.json";
    default String getConfigName() {
        return CONFIG_NAME;
    }

    boolean loadConfiguration(Path basePath);

    boolean saveConfiguration(Path basePath);
    /**
     * This should load the standard configuration of the module that gets loaded in case the configuration is corrupt
     */
    boolean loadStandardConfiguration();

    /**
     * @return The configuration of the module
     */
    Optional<BasicConfig> getConfiguration();

    /**
     * @return The path to the REST endpoint
     */
    String getRestPath();

    /**
     * @return The configuration of the servlet (i.e. Filters, etc.) but not the servlet itself
     */
    Class<?>[] getServletConfiguration();

    Class<? extends BasicServlet> getRestServlet();
}
