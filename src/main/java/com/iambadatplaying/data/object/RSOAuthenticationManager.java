package com.iambadatplaying.data.object;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iambadatplaying.Starter;
import com.iambadatplaying.Util;
import com.iambadatplaying.logger.LogLevel;
import com.iambadatplaying.rcconnection.RCConnectionManager;
import com.iambadatplaying.server.rest.servlets.ServletUtils;

import javax.net.ssl.HttpsURLConnection;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RSOAuthenticationManager extends ObjectDataManager {
    public static final Pattern URI_PATTERN = Pattern.compile("/rso-authenticator/v1/authentication$");

    public RSOAuthenticationManager(Starter starter) {
        super(starter);
    }

    @Override
    protected Optional<JsonObject> fetchCurrentState() {
        HttpsURLConnection con = starter.getRCConnector().getRCConnectionManager().buildConnection(RCConnectionManager.Method.GET, "/rso-authenticator/v1/authentication", null);
        Optional<JsonObject> optData = ServletUtils.readConnection(con).flatMap(Util::parseJson).flatMap(Util::getAsJsonObject);
        if (!optData.isPresent()) return Optional.empty();
        JsonObject data = optData.get();
        if (data.has("errorCode")) return Optional.empty();
        return Optional.of(data);
    }

    @Override
    protected void doStart() {

    }

    @Override
    protected void doStop() {

    }

    @Override
    public void sendCurrentState() {
        JsonObject currentState = getCurrentStateObject();
        if (currentState == null) return;
        starter.getLocalServer().sendToAllWebsockets(getEventDataString(currentState));
    }

    @Override
    protected Matcher getURIMatcher(String uri) {
        return URI_PATTERN.matcher(uri);
    }

    @Override
    protected void doUpdateAndSend(Matcher uriMatcher, String type, JsonElement data) {
        if (!data.isJsonObject()) return;
        JsonObject jsonObject = data.getAsJsonObject();
        switch (type) {
            case UPDATE_TYPE_UPDATE:
            case UPDATE_TYPE_CREATE:
                Util.getOptJSONObject(jsonObject, "captcha").ifPresent(captcha -> {
                    log(LogLevel.INFO, "Captcha: " + captcha);
                });
                Util.getOptJSONObject(jsonObject, "multifactor").ifPresent(
                        multifactor -> {
                            log(LogLevel.INFO, "Multifactor: " + multifactor);
                        }
                );
                setCurrentState(jsonObject);
                sendCurrentState();
                break;
            case UPDATE_TYPE_DELETE:
                reset();
                break;
            default:
                break;
        }
    }

    @Override
    public String getEventName() {
        return "RSOAuthentication";
    }
}
