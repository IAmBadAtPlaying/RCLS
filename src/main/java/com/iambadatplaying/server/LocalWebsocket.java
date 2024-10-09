package com.iambadatplaying.server;

import com.iambadatplaying.Managable;
import com.iambadatplaying.Starter;
import com.iambadatplaying.logger.LogLevel;
import com.iambadatplaying.logger.Loggable;
import com.iambadatplaying.logger.SimpleLogger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

@WebSocket
public class LocalWebsocket implements Loggable {

    private static final int KEEP_ALIVE_INTERVAL_MS = 290000;

    private final Starter starter;

    private Session currentSession = null;
    private volatile boolean markedForShutdown = false;

    private final TimerTask keepAliveTask  = new TimerTask() {
        @Override
        public void run() {
            messageQueue.offer("");
        }
    };
    private final Timer     keepAliveTimer = new java.util.Timer();

    private final ConcurrentLinkedQueue<String> messageQueue        = new ConcurrentLinkedQueue<>();
    private final Thread                        messageSenderThread = new Thread(() -> {
        while (!Thread.currentThread().isInterrupted()) {
            if (messageQueue.isEmpty() || currentSession == null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    return;
                }
                continue;
            }
            String message = messageQueue.poll();
            if (message == null) continue;
            try {
                currentSession.getRemote().sendString(message);
            } catch (Exception e) {

            }
        }
    });

    public LocalWebsocket(Starter starter) {
        this.starter = starter;
    }

    public void externalShutdown() {
        if (!markedForShutdown) {
            starter.getLocalServer().removeWebsocketConnection(this);
            markedForShutdown = true;
        }
    }

    public void sendMessage(String message) {
        messageQueue.offer(message);
    }

    @OnWebSocketMessage
    public void onMessage(String message) {}

    @OnWebSocketConnect
    public void onConnect(Session session) {
        Optional.ofNullable(currentSession).ifPresent(Session::close);
        currentSession = session;
        starter.getLocalServer().addWebsocketConnection(this);
        log("Client connected! " + session.getRemoteAddress().getAddress());
        messageSenderThread.start();
        keepAliveTimer.schedule(keepAliveTask, 0, KEEP_ALIVE_INTERVAL_MS);
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        log("Client disconnected! " + reason);
        markedForShutdown = true;
        currentSession = null;
        messageSenderThread.interrupt();
        keepAliveTask.cancel();
    }

    @OnWebSocketError
    public void onError(Throwable t) {
        log(LogLevel.WARN, "Error: " + t.getMessage());
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
