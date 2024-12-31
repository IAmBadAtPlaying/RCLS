package com.iambadatplaying.modules;

import com.iambadatplaying.EXIT_CODE;
import com.iambadatplaying.Managable;
import com.iambadatplaying.Starter;
import com.iambadatplaying.logger.LogLevel;
import com.iambadatplaying.logger.Loggable;
import com.iambadatplaying.logger.SimpleLogger;
import com.iambadatplaying.modules.accounts.AccountModule;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class BasicModuleLoader implements Loggable, Managable {

    private static final String MODULES_FOLDER_NAME = "modules";

    private boolean isRunning = false;

    private final Starter                  starter;
    private final Map<String, BasicModule> modules;
    private       Path                     appFolderPath;
    private       Path                     dynamicModuleFolderPath;

    public BasicModuleLoader(Starter starter) {
        this.starter = starter;
        this.modules = new HashMap<>();
        registerModules();
        onInit();
    }

    private void registerModules() {
        registerModule(new AccountModule());
    }

    private void registerModule(BasicModule module) {
        modules.put(module.getClass().getSimpleName(), module);
    }

    private void onInit() {
        if (!initDirectoryStructure()) {
            log(LogLevel.CRITICAL, "Failed to initialize directory structure");
            starter.exit(EXIT_CODE.FAILED_TO_LOAD_MODULES);
        }
    }

    private boolean initDirectoryStructure() {
        //TODO: This currently only works on Windows
        Path path = Paths.get(System.getenv("LOCALAPPDATA"), Starter.APPLICATION_NAME);
        if (!Files.exists(path)) {
            try {
                File file = path.toFile();
                if (!file.mkdir()) {
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        appFolderPath = path;

        Path dynamicModulePath = path.resolve(MODULES_FOLDER_NAME);
        if (!Files.exists(dynamicModulePath)) {
            try {
                File file = dynamicModulePath.toFile();
                if (!file.mkdir()) {
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        dynamicModuleFolderPath = dynamicModulePath;

        return true;
    }

    public Path getAppFolderPath() {
        return appFolderPath;
    }

    public Path getDynamicModuleFolderPath() {
        return dynamicModuleFolderPath;
    }

    public BasicModule getModule(Class<?> clazz) {
        return modules.get(clazz.getSimpleName());
    }

    public BasicModule[] getModules() {
        return modules.values().toArray(new BasicModule[0]);
    }

    @Override
    public void start() {
        if (isRunning) {
            return;
        }
        log("Starting BasicModuleLoader");
        isRunning = true;
        for (BasicModule module : modules.values()) {
            if (!module.loadConfiguration(appFolderPath)) {
                log(LogLevel.INFO, "Failed to load configuration for module: " + module.getClass().getSimpleName());
                if (!module.loadStandardConfiguration()) {
                    log(LogLevel.ERROR, "Failed to load standard configuration for module: " + module.getClass().getSimpleName());
                }
            }
        }
    }

    @Override
    public void stop() {
        if (!isRunning) {
            return;
        }
        log("Stopping BasicModuleLoader");
        isRunning = false;
        for (BasicModule module : modules.values()) {
            if (!module.saveConfiguration(appFolderPath)) {
                log(LogLevel.ERROR, "Failed to save configuration for module: " + module.getClass().getSimpleName());
            }
        }
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public void log(Object o) {
        log(LogLevel.DEBUG, o);
    }

    @Override
    public void log(LogLevel level, Object o) {
        SimpleLogger.getInstance().log(level, this.getClass().getSimpleName() + ": " + o);
    }
}
