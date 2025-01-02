package com.iambadatplaying.data.map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iambadatplaying.Starter;
import com.iambadatplaying.Util;
import com.iambadatplaying.logger.LogLevel;
import com.iambadatplaying.rcconnection.RCClient;
import com.iambadatplaying.rcconnection.RCConnectionManager;
import com.iambadatplaying.rcconnection.process.Game;
import com.iambadatplaying.server.rest.servlets.ServletUtils;

import javax.net.ssl.HttpsURLConnection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SessionManager extends MapDataManager<String> {

    private static final Pattern URI_PATTERN = Pattern.compile("^/product-session/v1/sessions/(.*)");
    private static final String PRODUCT_ID_KEYSTONE_CLIENT = "KeystoneClient";

    private String keepAliveSessionId = null;
    private String keystoneSessionId = null;

    private Timer timer;

    public SessionManager(Starter starter) {
        super(starter);
    }

    private synchronized void resetTimer() {
        Optional.ofNullable(timer).ifPresent(Timer::cancel);
        timer = new Timer();
    }

    private void loadCurrentSessions() {
        HttpsURLConnection con = starter.getRCConnector().getRCConnectionManager().buildConnection(RCConnectionManager.Method.GET, "/product-session/v1/sessions", null);
        Optional<JsonObject> optResponse = ServletUtils.readConnection(con).flatMap(Util::parseJson).flatMap(Util::getAsJsonObject);
        con.disconnect();
        if (!optResponse.isPresent()) return;
        JsonObject response = optResponse.get();
        for (Map.Entry<String, JsonElement> entry : response.entrySet()) {
            String key = entry.getKey();
            JsonObject value = entry.getValue().getAsJsonObject();
            map.put(key, value);
        }
    }

    @Override
    public Optional<JsonObject> getExternal(String key) {
        return get(key);
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
        timer = new Timer();
        loadCurrentSessions();
    }

    @Override
    protected void doStop() {
        Optional.ofNullable(timer).ifPresent(Timer::cancel);
        Optional.ofNullable(timer).ifPresent(Timer::purge);
        timer = null;
        keepAliveSessionId = null;
        keystoneSessionId = null;
    }

    @Override
    public void sendCurrentState() {

    }

    @Override
    protected Matcher getURIMatcher(String uri) {
        return URI_PATTERN.matcher(uri);
    }

    @Override
    protected void doUpdateAndSend(Matcher uriMatcher, String type, JsonElement data) {
        if (uriMatcher.groupCount() != 1) return;
        String sessionId = uriMatcher.group(1);
        switch (type) {
            case UPDATE_TYPE_CREATE:
//                checkForKeystoneInstanceCreation(sessionId, data);
                Util.getAsJsonObject(data).ifPresent(obj -> map.put(sessionId, obj));
                break;
            case UPDATE_TYPE_UPDATE:
                Util.getAsJsonObject(data).ifPresent(obj -> map.put(sessionId, obj));
                break;
            case UPDATE_TYPE_DELETE:
//                checkForKeystoneInstanceDeletion(sessionId);
                map.remove(sessionId);
                break;
            default:
                break;
        }
    }

    private void checkForKeystoneInstanceDeletion(String sessionId) {
        Optional<String> optProductId = Util.getAsJsonObject(map.get(sessionId)).flatMap(obj -> Util.getOptString(obj, "productId"));
        if (!optProductId.isPresent()) return;
        String productId = optProductId.get();
        if (PRODUCT_ID_KEYSTONE_CLIENT.equals(productId) && sessionId.equals(keystoneSessionId)) {
            log(LogLevel.INFO, "Keystone client session was deleted, will try to unregister keep alive session");
            keystoneSessionId = null;
            timer.schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    unregisterKeepAliveSession();
                }
            }, 1000 *  5);
        }
    }

    private void unregisterKeepAliveSession() {
        if (keepAliveSessionId == null) return;
        HttpsURLConnection con = starter.getRCConnector().getRCConnectionManager().buildConnection(RCConnectionManager.Method.DELETE, "/product-session/v1/sessions/" + keepAliveSessionId, map.get(keepAliveSessionId).toString());
        Optional<Integer> optResponse = Util.getResponseCode(con);
        con.disconnect();
        if (!optResponse.isPresent()) return;
        int responseCode = optResponse.get();
        if (responseCode != 200 && responseCode != 204) return;
        keepAliveSessionId = null;
    }

    private void checkForKeystoneInstanceCreation(String sessionId, JsonElement data) {
        Optional<String> optProductId = Util.getAsJsonObject(data).flatMap(obj -> Util.getOptString(obj, "productId"));
        if (!optProductId.isPresent()) return;
        String productId = optProductId.get();
        if (PRODUCT_ID_KEYSTONE_CLIENT.equals(productId) && keystoneSessionId == null) {
            log(LogLevel.INFO, "Keystone client session was created, will try to register keep alive session");
            keystoneSessionId = sessionId;
            registerKeepAliveSession();
        }
    }

    private void registerKeepAliveSession() {
        if (keepAliveSessionId != null) return;
        JsonObject keepAliveObject = RCClient.createKeepAliveObject(false);
        String sessionId = Util.createSecret() + Util.createSecret();
        HttpsURLConnection con = starter.getRCConnector().getRCConnectionManager().buildConnection(RCConnectionManager.Method.POST, "/product-session/v1/sessions/" + sessionId, keepAliveObject.toString());
        Optional<Integer> optResponse = Util.getResponseCode(con);
        con.disconnect();
        if (!optResponse.isPresent()) return;
        int responseCode = optResponse.get();
        if (responseCode != 200 && responseCode != 204) return;
        keepAliveSessionId = sessionId;
    }

    public JsonArray getSessionsByGame(Game game) {
        if (this.map.isEmpty()) return new JsonArray();

        JsonArray sessions = new JsonArray();
        map.forEach((key, value) -> {
            JsonObject session = value.getAsJsonObject();
            if (!Util.jsonKeysPresent(session, "productId")) return;
            String productId = session.get("productId").getAsString();
            if (game.getInternalName().equals(productId)) {
                sessions.add(session);
            }
        });

        return sessions;
    }

    @Override
    public String getEventName() {
        return "";
    }
}
