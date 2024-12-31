package com.iambadatplaying.data.map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iambadatplaying.Starter;
import com.iambadatplaying.logger.LogLevel;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValorantMatchDataManager extends MapDataManager<String> {

    private static final Pattern RMS_MATCH_UPDATE_PATTERN = Pattern.compile("/riot-messaging-service/v1/messages/ares-core-game/core-game/v1/matches/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})");

    private final Set<String> matchIds = new HashSet<>();

    public ValorantMatchDataManager(Starter starter) {
        super(starter);
    }

    @Override
    public Optional<JsonObject> getExternal(String key) {
        return Optional.empty();
    }

    @Override
    public Optional<JsonObject> load(String key) {
        return Optional.empty();
    }

    @Override
    public String getSingleKeyUpdateEventName() {
        return "";
    }

    @Override
    protected void doStart() {
    }

    @Override
    protected void doStop() {
        this.matchIds.clear();
    }

    @Override
    public void sendCurrentState() {

    }

    @Override
    protected Matcher getURIMatcher(String uri) {
        return RMS_MATCH_UPDATE_PATTERN.matcher(uri);
    }

    public Set<String> getMatchIds() {
        return Collections.unmodifiableSet(matchIds);
    }

    @Override
    protected void doUpdateAndSend(Matcher uriMatcher, String type, JsonElement data) {
        if (!UPDATE_TYPE_CREATE.equals(type)) return;
        if (uriMatcher.groupCount() < 1) return;
        String matchId = uriMatcher.group(1);
        if (matchIds.contains(matchId)) return;
        matchIds.add(matchId);
        log(LogLevel.INFO, "New match: " + matchId);
    }

    @Override
    public String getEventName() {
        return "";
    }
}
