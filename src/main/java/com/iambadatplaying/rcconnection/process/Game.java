package com.iambadatplaying.rcconnection.process;

import com.google.gson.JsonObject;

import java.util.Optional;

public enum Game {
    VALORANT("Valorant", "valorant"),
    LEAGUE_OF_LEGENDS("League of Legends", "league_of_legends"),
    LOR("Legends of Runeterra", "bacon"),
    RIOT_CLIENT("Riot Client", "riot_client");

    private final String displayName;
    private final String internalName;

    Game(
            String displayName,
            String internalName
    ) {
        this.displayName = displayName;
        this.internalName = internalName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getInternalName() {
        return internalName;
    }

    public static Optional<Game> getByInternalName(String internalName) {
        for (Game game : Game.values()) {
            if (game.internalName.equals(internalName)) {
                return Optional.of(game);
            }
        }
        return Optional.empty();
    }

    public static Optional<Game> getByName(String name) {
        if (name == null || name.isEmpty()) {
            return Optional.empty();
        }

        name = name.toUpperCase();

        for (Game game : Game.values()) {
            if (game.name().equals(name)) {
                return Optional.of(game);
            }
        }
        return Optional.empty();
    }

    public JsonObject getAsJsonObject() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("displayName", displayName);
        jsonObject.addProperty("internalName", internalName);

        return jsonObject;
    }
}
