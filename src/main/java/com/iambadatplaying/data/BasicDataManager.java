package com.iambadatplaying.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iambadatplaying.Managable;
import com.iambadatplaying.Starter;
import com.iambadatplaying.logger.LogLevel;
import com.iambadatplaying.logger.Loggable;
import com.iambadatplaying.logger.SimpleLogger;

import java.util.regex.Matcher;

public abstract class BasicDataManager implements Managable, Loggable {
    public static final String KEY_EVENT_TYPE = "eventType";
    public static final String KEY_EVENT_DATA = "data";

    protected static final String UPDATE_TYPE_CREATE = "Create";
    protected static final String UPDATE_TYPE_UPDATE = "Update";
    protected static final String UPDATE_TYPE_DELETE = "Delete";

    protected       boolean running = false;
    protected final Starter starter;

    protected BasicDataManager(Starter starter) {
        this.starter = starter;
    }

    protected String getEventDataString(JsonObject data) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(KEY_EVENT_TYPE, getEventName());
        jsonObject.add(KEY_EVENT_DATA, data);
        return jsonObject.toString();
    }

    protected String getInitialDataString(JsonObject data) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(KEY_EVENT_TYPE, "Initial"+getEventName());
        jsonObject.add(KEY_EVENT_DATA, data);
        return jsonObject.toString();
    }

    public void start() {
        if (running) return;
        running = true;
        doStart();
        log(LogLevel.INFO, "Started");
    }

    protected abstract void doStart();

    public void stop() {
        if (!running) {
            log("Not running");
            return;
        }
        log("Stopping");
        running = false;
        doStop();
    }

    public boolean isRunning() {
        return running;
    }

    protected abstract void doStop();

    public abstract void reset();

    public abstract void sendCurrentState();

    protected abstract Matcher getURIMatcher(String uri);

    protected abstract void doUpdateAndSend(Matcher uriMatcher, String type, JsonElement data);

    public void update(String type, String uri, JsonElement data) {
        if (!running) {
            return;
        }

        Matcher uriMatcher = getURIMatcher(uri);
        if (!uriMatcher.matches()) return;
        doUpdateAndSend(uriMatcher, type, data);
    }

    public abstract String getEventName();

    public void log(LogLevel level, Object o) {
        SimpleLogger.getInstance().log(level, this.getClass().getSimpleName() + ": " + o);
    }

    public void log(Object o) {
        log(LogLevel.DEBUG, o);
    }
}
