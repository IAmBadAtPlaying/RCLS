package com.iambadatplaying.data.object;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iambadatplaying.Starter;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocaleManager extends ObjectDataManager {
    private static final Pattern LOCALE_PATTERN = Pattern.compile("/riotclient/region-locale$");

    public LocaleManager(Starter starter) {
        super(starter);
    }

    @Override
    protected Optional<JsonObject> fetchCurrentState() {
        return Optional.empty();
    }

    @Override
    protected void doStart() {

    }

    @Override
    protected void doStop() {

    }

    @Override
    public void sendCurrentState() {
        starter.getLocalServer().sendToAllWebsockets(getEventDataString(currentState));
    }

    @Override
    protected Matcher getURIMatcher(String uri) {
        return LOCALE_PATTERN.matcher(uri);
    }

    @Override
    protected void doUpdateAndSend(Matcher uriMatcher, String type, JsonElement data) {
        if (!data.isJsonObject()) return;
        JsonObject jsonObject = data.getAsJsonObject();
        switch (type) {
            case UPDATE_TYPE_UPDATE:
            case UPDATE_TYPE_CREATE:
                if (!jsonObject.has("locale")) return;
                currentState.addProperty("locale", jsonObject.get("locale").getAsString());
                sendCurrentState();
                break;
            case UPDATE_TYPE_DELETE:
                reset();
                break;
        }
    }

    @Override
    public String getEventName() {
        return "Locale";
    }
}
