package com.iambadatplaying.rcconnection;


import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.iambadatplaying.Managable;
import com.iambadatplaying.Starter;
import com.iambadatplaying.Util;
import com.iambadatplaying.logger.LogLevel;
import com.iambadatplaying.logger.Loggable;
import com.iambadatplaying.logger.SimpleLogger;
import com.iambadatplaying.rcconnection.process.Game;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import javax.net.ssl.HttpsURLConnection;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Optional;

public class RCClient implements Managable, Loggable {

    private static final int     MAXIMUM_BUFFER_SIZE = Integer.MAX_VALUE;
    private final        Starter starter;

    private WebSocketClient client      = null;
    private boolean         running     = false;
    private RCWebsocket     rcWebsocket = null;

    public RCClient(Starter starter) {
        this.starter = starter;
    }

    public static JsonObject createKeepAliveObject(boolean isInternal) {
        JsonObject requestObject = new JsonObject();
        JsonObject launchConfig = new JsonObject();

        launchConfig.add("arguments", new JsonArray());
        launchConfig.addProperty("executable", "");
        launchConfig.add("locale", JsonNull.INSTANCE);
        launchConfig.add("voiceLocale", JsonNull.INSTANCE);
        launchConfig.addProperty("workingDirectory", "");

        requestObject.addProperty("exitCode",0);
        requestObject.add("exitReason", JsonNull.INSTANCE);
        requestObject.addProperty("isInternal",isInternal);
        requestObject.add("launchConfig", launchConfig);
        requestObject.addProperty("patchlineFullName", "Just dont quit on rc exit");
        requestObject.addProperty("patchlineId", "developer_product.defaultpatchline");
        requestObject.addProperty("phase", "None");
        requestObject.addProperty("productId", Game.RIOT_CLIENT.getInternalName());
        requestObject.addProperty("version", "0.0.0.0");
        return requestObject;
    }

    @Override
    public void start() {
        if (starter.getRCConnector().getConnectionState() != RCConnectionState.CONNECTED) {
            log(LogLevel.WARN, "Wont start RCClient, not connected to RC");
            return;
        }
        SslContextFactory sslContextFactory = new SslContextFactory.Client(false);

        HttpClient httpClient = new HttpClient(sslContextFactory);
        String sUri = "wss://127.0.0.1:" + starter.getRCConnector().getPort() + "/";
        this.client = new WebSocketClient(httpClient);
        client.setStopAtShutdown(true);
        client.getPolicy().setMaxTextMessageSize(MAXIMUM_BUFFER_SIZE);
        rcWebsocket = new RCWebsocket(starter);

        sslContextFactory.setSslContext(starter.getRCConnector().getSslContextGlobal());
        try {
            client.start();
            URI uri = new URI(sUri);
            ClientUpgradeRequest request = new ClientUpgradeRequest();
            request.setHeader("Authorization", starter.getRCConnector().getAuthToken());
            client.connect(rcWebsocket, uri, request);
        } catch (Exception e) {
            log(LogLevel.ERROR, e);
        }
        running = true;
    }

    public boolean registerDebugSession() {
        String debugSessionName = Util.createSecret() + Util.createSecret();
        JsonObject requestObject = RCClient.createKeepAliveObject(true);
        HttpsURLConnection con = starter.getRCConnector().getRCConnectionManager().buildConnection(RCConnectionManager.Method.POST, "/product-session/v1/sessions/"+debugSessionName, requestObject.toString());
        Optional<Integer> optResponse = Util.getResponseCode(con);
        if (!optResponse.isPresent()) {
            log(LogLevel.WARN, "Failed to get response code from debug session registration");
            return false;
        }
        int responseCode = optResponse.get();
        if (responseCode != Response.Status.OK.getStatusCode() && responseCode != Response.Status.NO_CONTENT.getStatusCode()) {
            log(LogLevel.WARN, "Failed to register debug session, response code: " + responseCode);
            return false;
        }

        log(LogLevel.DEBUG, "Registered debug session with name: " + debugSessionName);
        return true;
    }

    @Override
    public void stop() {
        running = false;
        if (client != null) client.destroy();
        client = null;
    }

    @Override
    public boolean isRunning() {
        return running;
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
