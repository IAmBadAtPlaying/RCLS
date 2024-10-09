package com.iambadatplaying.data.map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iambadatplaying.Starter;
import com.iambadatplaying.data.BasicDataManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public abstract class MapDataManager<T> extends BasicDataManager {

    public static <T> Map<T, JsonObject> getMapFromArray(JsonArray array, String identifier, Function<JsonElement, T> keyMapper) {
        Map<T, JsonObject> map = new HashMap<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) continue;
            JsonObject object = element.getAsJsonObject();
            if (!object.has(identifier)) continue;
            T key = keyMapper.apply(object.get(identifier));
            map.put(key, object);
        }
        return map;
    }

    protected final Map<T, JsonObject> map;

    protected MapDataManager(Starter starter) {
        super(starter);
        map = Collections.synchronizedMap(new HashMap<>());
    }

    public Optional<JsonObject> get(T key) {
        if (map.containsKey(key)) return Optional.ofNullable(map.get(key));
        Optional<JsonObject> value = load(key);
        value.ifPresent(jsonObject -> map.put(key, jsonObject));
        return value;
    }

    public abstract Optional<JsonObject> getExternal(String key);

    public abstract Optional<JsonObject> load(T key);

    public abstract String getSingleKeyUpdateEventName();

    public JsonObject getMapAsJson() {
        JsonObject mapAsJson = new JsonObject();
        for (Map.Entry<T, JsonObject> entry : map.entrySet()) {
            mapAsJson.add(entry.getKey().toString(), entry.getValue());
        }
        return mapAsJson;
    }

    @Override
    public void reset() {
        map.clear();
        sendCurrentState();
    }

    @Override
    public void stop() {
        if (!running) return;
        running = false;
        doStop();
        map.clear();
    }
}
