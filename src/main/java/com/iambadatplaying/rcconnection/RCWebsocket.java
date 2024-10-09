package com.iambadatplaying.rcconnection;

import com.iambadatplaying.Starter;
import com.iambadatplaying.logger.LogLevel;
import com.iambadatplaying.logger.Loggable;
import com.iambadatplaying.logger.SimpleLogger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

import java.util.Timer;
import java.util.TimerTask;

@WebSocket
public class RCWebsocket implements Loggable {
    private final Starter   starter;
    private       Session   currentSession;
    private final TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            try {
                log("Sending keep alive");
                currentSession.getRemote().sendString("");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
    private final Timer     timer     = new Timer();

    public RCWebsocket(Starter starter) {
        this.starter = starter;
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        this.currentSession = session;
        log("Connect: " + session.getRemoteAddress().getAddress());
        subscribeToEndpoint("OnJsonApiEvent");
        timer.schedule(timerTask, 0, 290000);
    }

    @OnWebSocketMessage
    public void onMessage(String message) {
        starter.getDataManger().handleData(message);
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        log("Closed: " + reason);
        timer.cancel();
        starter.getRCConnector().RCWebsocketClosed();
    }

    @OnWebSocketError
    public void onError(Throwable t) {
        if ((t.getMessage() != null) && !t.getMessage().equals("null")) {
            log(t.getMessage());
        }
    }

    private void subscribeToEndpoint(String endpoint, int tries) {
        if (tries > 5) {
            log(LogLevel.CRITICAL, "No updates receivable for endpoint " + endpoint);
            return;
        }
        try {
            log("Subscribing to: " + endpoint);
            currentSession.getRemote().sendString("[5, \"" + endpoint + "\"]");
        } catch (Exception e) {
            log(LogLevel.WARN, "Cannot subscribe to endpoint: " + endpoint);
            new Thread(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                subscribeToEndpoint(endpoint, tries + 1);
            }).start();
        }
    }

    public void subscribeToEndpoint(String endpoint) {
        subscribeToEndpoint(endpoint, 0);
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
