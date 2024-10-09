package com.iambadatplaying.data.array;

import com.google.gson.JsonArray;
import com.iambadatplaying.Starter;
import com.iambadatplaying.data.BasicDataManager;

import java.util.Optional;

public abstract class ArrayDataManager extends BasicDataManager {

    protected JsonArray currentArray = new JsonArray();

    protected ArrayDataManager(Starter starter) {
        super(starter);
    }

    public Optional<JsonArray> getCurrentState() {
        if (!running) return Optional.empty();
        if (currentArray != null) return Optional.ofNullable(currentArray);
        Optional<JsonArray> newState = fetchCurrentState();
        newState.ifPresent(jsonArray -> currentArray = jsonArray);
        return newState;
    }

    protected abstract Optional<JsonArray> fetchCurrentState();

    @Override
    public void stop() {
        if (!running) return;
        running = false;
        doStop();
        currentArray = null;
    }

    @Override
    public void reset() {
        if (!running) return;
        currentArray = null;
        sendCurrentState();
    }
}
