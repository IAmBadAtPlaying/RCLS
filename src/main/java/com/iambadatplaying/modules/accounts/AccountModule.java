package com.iambadatplaying.modules.accounts;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.iambadatplaying.logger.LogLevel;
import com.iambadatplaying.logger.Loggable;
import com.iambadatplaying.logger.SimpleLogger;
import com.iambadatplaying.modules.BasicConfig;
import com.iambadatplaying.modules.BasicModule;
import com.iambadatplaying.modules.BasicServlet;
import com.iambadatplaying.modules.accounts.structs.AccountList;
import com.iambadatplaying.modules.accounts.structs.accessMap.ValidatingAccessMap;
import com.iambadatplaying.server.rest.providers.GsonJsonElementMessageBodyReader;
import com.iambadatplaying.server.rest.providers.GsonJsonElementMessageBodyWriter;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

public class AccountModule implements BasicModule, Loggable {

    private static final String REST_PATH = "/accounts";

    private static final String MODULE_FOLDER = "accounts";

    private AccountConfiguration                     configuration;
    private ValidatingAccessMap<String, AccountList> accountMaps;

    public AccountModule() {
        accountMaps = new ValidatingAccessMap<>();
    }

    public ValidatingAccessMap<String, AccountList> getAccountMaps() {
        return accountMaps;
    }

    @Override
    public boolean loadConfiguration(Path basePath) {
        Path moduleFolderPath = basePath.resolve(MODULE_FOLDER);
        if (!moduleFolderPath.toFile().exists()) {
            log("Module folder does not exist");
            if (!moduleFolderPath.toFile().mkdirs()) {
                log("Failed to create module folder");
                return false;
            }
        }
        File configFile = moduleFolderPath.resolve(getConfigName()).toFile();
        if (!configFile.exists()) {
            log("Config file does not exist");
            return false;
        }
        try (FileReader configReader = new FileReader(configFile)) {
            configuration = new Gson().fromJson(configReader, AccountConfiguration.class);
        } catch (Exception e) {
            log("Failed to parse config file");
            return false;
        }

        try (FileReader accountListReader = new FileReader(moduleFolderPath.resolve(configuration.getAccountListFileName()).toFile())) {
            JsonElement accountListElement = JsonParser.parseReader(accountListReader);
            if (!accountListElement.isJsonObject()) {
                log("Account list file is not a json object");
                return false;
            }

            JsonObject accountListObject = accountListElement.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : accountListObject.entrySet()) {
                AccountList accountList = new Gson().fromJson(entry.getValue(), AccountList.class);
                accountMaps.put(entry.getKey(), accountList);
            }
        } catch (Exception e) {
            log("Failed to parse account list file");
            return false;
        }

        return true;
    }

    @Override
    public boolean saveConfiguration(Path basePath) {
        Path moduleFolderPath = basePath.resolve(MODULE_FOLDER);
        if (!moduleFolderPath.toFile().exists()) {
            if (!moduleFolderPath.toFile().mkdirs()) {
                return false;
            }
        }

        File configFile = moduleFolderPath.resolve(getConfigName()).toFile();
        try {
            if (!configFile.exists()) {
                log("Config file does not exist");
                if (!configFile.createNewFile()) {
                    log("Failed to create config file");
                    return false;
                }
            }
        } catch (Exception e) {
            return false;
        }

        try {
            Files.write(configFile.toPath(), new Gson().toJson(configuration).getBytes());
        } catch (Exception e) {
            log("Error during writing / parsing to config file");
            return false;
        }

        Path accountListFile = moduleFolderPath.resolve(configuration.getAccountListFileName());
        if (!accountListFile.toFile().exists()) {
            log("Account list file does not exist");
            try {
                if (!accountListFile.toFile().createNewFile()) {
                    log("Failed to create account list file");
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
        }

        for (AccountList map : accountMaps.values()) {
            if (!map.lock()) {
                log("Map " + map.getId() + " failed to lock, maybe already locked?");
            }
        }

        try {
            Files.write(accountListFile, new Gson().toJson(accountMaps).getBytes());
        } catch (Exception e) {
            log("Error during writing / parsing to account list file");
            return false;
        }

        return true;
    }

    @Override
    public boolean loadStandardConfiguration() {
        configuration = new AccountConfiguration();
        return true;
    }

    @Override
    public Optional<BasicConfig> getConfiguration() {
        return Optional.ofNullable(configuration);
    }

    @Override
    public String getRestPath() {
        return REST_PATH;
    }

    @Override
    public Class<?>[] getServletConfiguration() {
        return new Class[]{
                GsonJsonElementMessageBodyReader.class,
                GsonJsonElementMessageBodyWriter.class
        };
    }

    @Override
    public Class<? extends BasicServlet> getRestServlet() {
        return AccountServlet.class;
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
