package com.iambadatplaying.rcconnection.process;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iambadatplaying.EXIT_CODE;
import com.iambadatplaying.Starter;
import com.iambadatplaying.Util;
import com.iambadatplaying.logger.LogLevel;
import com.iambadatplaying.logger.SimpleLogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Optional;
import java.util.OptionalInt;

public class WindowsProcessHandler implements ProcessHandler {
    private static final String WMIC_PROCESS_ID   = "ProcessId";
    private static final String WMIC_COMMAND_LINE = "CommandLine";

    private static final String RIOT_CLIENT_SERVICE_PROCESS_NAME = "RiotClientServices.exe";

    private static final String RIOT_GAMES_FOLDER_NAME = "Riot Games";
    private static final String RIOT_CLIENT_INSTALLS   = "RiotClientInstalls.json";

    private static final HashMap<Game, String> gameProcessNames = new HashMap<>();

    static {
        gameProcessNames.put(Game.VALORANT, "VALORANT-Win64-Shipping.exe");
        gameProcessNames.put(Game.LEAGUE_OF_LEGENDS, "LeagueClient.exe");
        gameProcessNames.put(Game.LOR, "LOR.exe");
        gameProcessNames.put(Game.RIOT_CLIENT, "Riot Client.exe");
    }

    private final Starter starter;
    private       Path    riotClientServicesExecutablePath = null;
    private       Thread  rcsProcessThread                 = null;

    public WindowsProcessHandler(Starter starter) {
        this.starter = starter;
        if (!getRiotClientServicesExecutablePath().isPresent()) {
            starter.exit(EXIT_CODE.RIOT_CLIENT_SERVICES_EXECUTABLE_NOT_FOUND);
        }
    }

    @Override
    public void startRiotClientServicesProcess(int port, String authToken) {
        if (rcsProcessThread != null && rcsProcessThread.isAlive()) {
            rcsProcessThread.interrupt();
        }
        Optional<Path> optRiotClientServicesExecutablePath = getRiotClientServicesExecutablePath();
        if (!optRiotClientServicesExecutablePath.isPresent()) {
            log(LogLevel.ERROR, "Unable to find the Riot Client Services executable path");
            return;
        }
        log("Starting Riot Client Services process");
        ProcessBuilder processBuilder = new ProcessBuilder(
                WindowsProcessUtils.buildStartRCSWithAuthTokenCommand(
                        optRiotClientServicesExecutablePath.get(),
                        port,
                        authToken,
                        true
                )
        );
        log("Command: " + processBuilder.command());
        rcsProcessThread = new Thread(() -> {
            try {
                Process proc = processBuilder.start();
                proc.waitFor();
            } catch (Exception e) {
            }
        });
        rcsProcessThread.start();
    }

    @Override
    public OptionalInt getGameProcessId(Game game) {
        switch (game) {
            case RIOT_CLIENT:
                return getRiotClientProcessId();
            default:
                return getDefaultGameProcessId(game);
        }
    }

    private OptionalInt getDefaultGameProcessId(Game game) {
        log("Using default process id retrieval for game: " + game.getDisplayName());
        String[] processIdWMICommand = WindowsProcessUtils.buildWMICommand(gameProcessNames.get(game), WMIC_PROCESS_ID);
        ProcessBuilder processBuilder = new ProcessBuilder(processIdWMICommand);
        try {
            Process process = processBuilder.start();
            Optional<String> optOutput = Util.inputStreamToString(process.getInputStream());
            Optional<String> optError = Util.inputStreamToString(process.getErrorStream());
            process.waitFor();
            process.destroy();

            if (defaultErrorOccurred(optOutput, optError)) return OptionalInt.empty();

            Optional<JsonObject> optProcessIdObj =
                    optOutput
                            .flatMap(Util::parseJson)
                            .flatMap(Util::getAsJsonObject);

            if (!optProcessIdObj.isPresent()) return OptionalInt.empty();

            JsonObject processIdObj = optProcessIdObj.get();

            return OptionalInt.of(processIdObj.get(WMIC_PROCESS_ID).getAsInt());
        } catch (Exception e) {
        }
        return OptionalInt.empty();
    }

    private OptionalInt getRiotClientProcessId() {
        log("Using custom process id retrieval for game: " + Game.RIOT_CLIENT.getDisplayName());
        ProcessBuilder processBuilder = new ProcessBuilder(
                WindowsProcessUtils.buildWMICommand(
                        gameProcessNames.get(Game.RIOT_CLIENT),
                        WMIC_PROCESS_ID,
                        WMIC_COMMAND_LINE
                )
        );
        try {
            Process process = processBuilder.start();
            Optional<String> optOutput = Util.inputStreamToString(process.getInputStream());
            Optional<String> optError = Util.inputStreamToString(process.getErrorStream());
            process.waitFor();
            process.destroy();

            if (defaultErrorOccurred(optOutput, optError)) return OptionalInt.empty();


            Optional<JsonArray> optArray = optOutput
                    .flatMap(Util::parseJson)
                    .flatMap(Util::getAsJsonArray);
            if (!optArray.isPresent()) return OptionalInt.empty();
            JsonArray array = optArray.get();
            //Find process where the command line contains "Riot Client.exe"
            Optional<JsonObject> optMainProcessObj = array.asList()
                    .stream()
                    .map(JsonElement::getAsJsonObject)
                    .filter(json -> json.get(WMIC_COMMAND_LINE).getAsString().contains("--remoting-auth-token"))
                    .findFirst();

            if (!optMainProcessObj.isPresent()) return OptionalInt.empty();
            JsonObject mainProcessObj = optMainProcessObj.get();
            return Util.getOptInt(mainProcessObj, WMIC_PROCESS_ID);
        } catch (Exception e) {
        }
        return OptionalInt.empty();
    }

    @Override
    public OptionalInt getRiotClientServicesProcessId() {
        log("Using custom process id retrieval for Riot Client Services");
        ProcessBuilder processBuilder = new ProcessBuilder(
                WindowsProcessUtils.buildWMICommand(
                        RIOT_CLIENT_SERVICE_PROCESS_NAME,
                        WMIC_PROCESS_ID
                )
        );
        try {
            Process process = processBuilder.start();
            Optional<String> optOutput = Util.inputStreamToString(process.getInputStream());
            Optional<String> optError = Util.inputStreamToString(process.getErrorStream());
            process.waitFor();
            process.destroy();

            if (defaultErrorOccurred(optOutput, optError)) return OptionalInt.empty();

            Optional<JsonObject> optProcessIdObj =
                    optOutput
                            .flatMap(Util::parseJson)
                            .flatMap(Util::getAsJsonObject);

            if (!optProcessIdObj.isPresent()) return OptionalInt.empty();
            JsonObject processIdObj = optProcessIdObj.get();
            return Util.getOptInt(processIdObj, WMIC_PROCESS_ID);
        } catch (Exception e) {
        }
        return OptionalInt.empty();
    }

    @Override
    public boolean killProcessWithId(int processId) {
        log("Killing process with id: " + processId);
        String[] command = WindowsProcessUtils.buildTaskkillCommand(processId);
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        try {
            Process process = processBuilder.start();
            Optional<String> optOutput = Util.inputStreamToString(process.getInputStream());
            Optional<String> optError = Util.inputStreamToString(process.getErrorStream());
            process.waitFor();
            process.destroy();

            if (defaultErrorOccurred(optOutput, optError)) {
                log("Error occurred while killing process with id: " + processId);
                return false;
            }

            log("Successfully killed process with id: " + processId);
            return true;
        } catch (IOException | InterruptedException e) {
        }
        return false;
    }

    @Override
    public boolean launchRiotClient(int port, String authToken) {
        Optional<Path> optRiotClientServicesExecutablePath = getRiotClientServicesExecutablePath();
        if (!optRiotClientServicesExecutablePath.isPresent()) {
            log(LogLevel.ERROR, "Unable to find the Riot Client Services executable path");
            return false;
        }

        Path rcsPath = optRiotClientServicesExecutablePath.get();
        Path riotClientElectronPath = rcsPath.getParent().resolve("RiotClientElectron").resolve("Riot Client.exe");
        File riotClientElectronFile = riotClientElectronPath.toFile();
        if (!riotClientElectronFile.exists() || !riotClientElectronFile.isFile()) {
            log(riotClientElectronFile.toPath().toString());
            log(LogLevel.ERROR, "The Riot Client Electron file does not exist");
            return false;
        }

        OptionalInt optProcessId = getRiotClientServicesProcessId();
        if (!optProcessId.isPresent()) {
            log(LogLevel.ERROR, "Unable to find the Riot Client Services process id");
            return false;
        }

        int processId = optProcessId.getAsInt();
        log("Launching Riot Client");
        ProcessBuilder processBuilder = new ProcessBuilder(
                WindowsProcessUtils.buildStartRiotClientCommand(
                        riotClientElectronPath,
                        port,
                        processId,
                        authToken
                )
        );

        try {
            Process process = processBuilder.start();
            process.waitFor();
            return true;
        } catch (IOException | InterruptedException e) {
        }
        return false;
    }

    private Optional<Path> getRiotClientServicesExecutablePath() {
        if (riotClientServicesExecutablePath != null) {
            return Optional.of(riotClientServicesExecutablePath);
        }

        String programFiles = System.getenv("ALLUSERSPROFILE");

        if (programFiles == null || programFiles.isEmpty()) {
            log(LogLevel.ERROR, "The program files environment variable is empty, unable to find the Riot Client Services executable");
            return Optional.empty();
        }

        Path programFilesPath = Paths.get(programFiles);
        Path riotGamesPath = programFilesPath.resolve(RIOT_GAMES_FOLDER_NAME);

        File riotGamesFolder = riotGamesPath.toFile();
        if (!riotGamesFolder.exists() || !riotGamesFolder.isDirectory()) {
            log(LogLevel.ERROR, "The Riot Games folder does not exist, unable to find the Riot Client Services executable");
            return Optional.empty();
        }

        Path riotClientInstallsPath = riotGamesPath.resolve(RIOT_CLIENT_INSTALLS);
        File riotClientInstallsFile = riotClientInstallsPath.toFile();

        if (!riotClientInstallsFile.exists() || !riotClientInstallsFile.isFile()) {
            log(LogLevel.ERROR, "The Riot Client Installs file does not exist, unable to find the Riot Client Services executable");
            return Optional.empty();
        }

        //Get stream from file
        try (InputStream is = Files.newInputStream(riotClientInstallsFile.toPath())) {
            Optional<String> optJsonString = Util.inputStreamToString(is);
            if (!optJsonString.isPresent()) {
                log(LogLevel.ERROR, "Unable to read the " + RIOT_CLIENT_INSTALLS + " file");
                return Optional.empty();
            }

            String jsonString = optJsonString.get();
            if (jsonString.trim().isEmpty()) {
                log(LogLevel.ERROR, RIOT_CLIENT_INSTALLS + " is empty!");
                return Optional.empty();
            }

            Optional<JsonElement> optJsonElement = Util.parseJson(jsonString);
            if (!optJsonElement.isPresent()) {
                log(LogLevel.ERROR, "The " + RIOT_CLIENT_INSTALLS + " cannot be parsed as Json");
                return Optional.empty();
            }

            JsonElement jsonElement = optJsonElement.get();
            if (!jsonElement.isJsonObject()) {
                log(LogLevel.ERROR, "The " + RIOT_CLIENT_INSTALLS + " does not contain a Json object");
                return Optional.empty();
            }

            JsonObject jsonObject = jsonElement.getAsJsonObject();
            String useKey = "rc_live";
            if (!Util.jsonKeysPresent(jsonObject, useKey)) {
                log("Key " + useKey + " is not present in the " + RIOT_CLIENT_INSTALLS + " file");
                useKey = "rc_default";
                if (!Util.jsonKeysPresent(jsonObject, useKey)) {
                    log(LogLevel.ERROR, "Key " + useKey + " is not present in the " + RIOT_CLIENT_INSTALLS + " file");
                    return Optional.empty();
                }
            }

            String stringRCSPath = jsonObject.get(useKey).getAsString();
            if (stringRCSPath == null || stringRCSPath.isEmpty()) {
                log(LogLevel.ERROR, "The Riot Client Services path is empty");
                return Optional.empty();
            }

            Path rcsPath = Paths.get(stringRCSPath);
            File rcsFile = rcsPath.toFile();
            if (!rcsFile.exists() || !rcsFile.isFile()) {
                log(LogLevel.ERROR, "The Riot Client Services file does not exist");
                return Optional.empty();
            }

            log("The Riot Client Services executable path is: " + rcsPath);

            riotClientServicesExecutablePath = rcsPath;
        } catch (Exception e) {
            return Optional.empty();
        }
        return Optional.of(riotClientServicesExecutablePath);
    }

    private boolean defaultErrorOccurred(Optional<String> output, Optional<String> error) {
        if (!output.isPresent()) return true;
        if (error.isPresent()) {
            String errorString = error.get().trim();
            if (!errorString.isEmpty()) {
                log("The error string is not empty: " + errorString);
                log("Assuming the process is not running");
                return true;
            }
        }
        return false;
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
