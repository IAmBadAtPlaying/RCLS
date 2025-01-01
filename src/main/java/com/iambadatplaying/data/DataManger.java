package com.iambadatplaying.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iambadatplaying.Managable;
import com.iambadatplaying.Starter;
import com.iambadatplaying.Util;
import com.iambadatplaying.data.array.ArrayDataManager;
import com.iambadatplaying.data.map.MapDataManager;
import com.iambadatplaying.data.map.SessionManager;
import com.iambadatplaying.data.map.ValorantMatchDataManager;
import com.iambadatplaying.data.object.ObjectDataManager;
import com.iambadatplaying.data.object.RSOAuthenticationManager;
import com.iambadatplaying.logger.LogLevel;
import com.iambadatplaying.logger.Loggable;
import com.iambadatplaying.logger.SimpleLogger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DataManger implements Loggable, Managable {

    private final Starter starter;
    private       boolean running = false;

    private final Map<String, MapDataManager>    mapDataManagers    = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, ObjectDataManager> objectDataManagers = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, ArrayDataManager>  arrayDataManagers  = Collections.synchronizedMap(new HashMap<>());

    public DataManger(Starter starter) {
        this.starter = starter;
        addManagers();
    }

    private void addManagers() {
        addManager(new RSOAuthenticationManager(starter));
        addManager(new SessionManager(starter));
        addManager(new ValorantMatchDataManager(starter));
    }

    private void addManager(MapDataManager manager) {
        if (manager == null) return;
        mapDataManagers.put(manager.getClass().getName(), manager);
    }

    private void addManager(ObjectDataManager manager) {
        if (manager == null) return;
        objectDataManagers.put(manager.getClass().getName(), manager);
    }

    private void addManager(ArrayDataManager manager) {
        if (manager == null) return;
        arrayDataManagers.put(manager.getClass().getName(), manager);
    }

    public <T> MapDataManager<?> getMapDataManager(Class<? extends MapDataManager<T>> clazz) {
        return mapDataManagers.get(clazz.getName());
    }

    public ObjectDataManager getObjectDataManager(Class<? extends ObjectDataManager> clazz) {
        return objectDataManagers.get(clazz.getName());
    }

    public ArrayDataManager getArrayDataManager(Class<? extends ArrayDataManager> clazz) {
        return arrayDataManagers.get(clazz.getName());
    }

    public void handleData(String s) {
        Optional<JsonArray> optDataArray = Util.parseJson(s).flatMap(Util::getAsJsonArray);
        if (!optDataArray.isPresent()) return;
        JsonArray dataArray = optDataArray.get();
        if (dataArray.size() != 3) return;
        JsonElement data = dataArray.get(2);
        if (!data.isJsonObject()) return;
        JsonObject preDataElement = data.getAsJsonObject();
        JsonElement typeElement = preDataElement.get("eventType");
        JsonElement uriElement = preDataElement.get("uri");
        JsonElement dataElement = preDataElement.get("data");
        if (typeElement == null || uriElement == null) return;
        if (!typeElement.isJsonPrimitive() || !uriElement.isJsonPrimitive()) return;
        String type = typeElement.getAsString().trim();
        String uri = uriElement.getAsString().trim();
        update(type, uri, dataElement);
    }

    private void update(String type, String uri, JsonElement data) {
        if (!running) return;
        log(type + " " + uri + ": " + data);
        new Thread(() -> {
            mapDataManagers.values().forEach(manager -> {
                manager.update(type, uri, data);
            });
        }).start();
        new Thread(() -> {
            objectDataManagers.values().forEach(manager -> {
                manager.update(type, uri, data);
            });
        }).start();
        new Thread(() -> {
            arrayDataManagers.values().forEach(manager -> {
                manager.update(type, uri, data);
            });
        }).start();
    }

    @Override
    public void start() {
        running = true;
        mapDataManagers.values().forEach(MapDataManager::start);
        objectDataManagers.values().forEach(ObjectDataManager::start);
        arrayDataManagers.values().forEach(ArrayDataManager::start);
    }

    @Override
    public void stop() {
        log("Stopping");
        running = false;
        mapDataManagers.values().forEach(MapDataManager::stop);
        objectDataManagers.values().forEach(ObjectDataManager::stop);
        arrayDataManagers.values().forEach(ArrayDataManager::stop);
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
