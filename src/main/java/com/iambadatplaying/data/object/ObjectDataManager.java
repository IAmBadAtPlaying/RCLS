package com.iambadatplaying.data.object;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.iambadatplaying.Starter;
import com.iambadatplaying.data.BasicDataManager;

import java.util.Optional;

public abstract class ObjectDataManager extends BasicDataManager {

    protected JsonObject currentState = new JsonObject();

    protected ObjectDataManager(Starter starter) {
        super(starter);
    }

    public Optional<JsonObject> getCurrentState() {
        if (!running) return Optional.empty();
        if (currentState != null) return Optional.of(currentState);
        Optional<JsonObject> newState = fetchCurrentState();
        newState.ifPresent(jsonArray -> currentState = jsonArray);
        return newState;
    }

    protected abstract Optional<JsonObject> fetchCurrentState();

    @Override
    public void stop() {
        super.stop();
        currentState = null;
    }

    @Override
    public void reset() {
        if (!running) return;
        currentState = null;
        sendCurrentState();
    }
}
