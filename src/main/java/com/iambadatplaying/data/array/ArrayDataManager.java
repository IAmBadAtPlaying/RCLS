package com.iambadatplaying.data.array;

import com.google.gson.JsonArray;
import com.iambadatplaying.Starter;
import com.iambadatplaying.data.BasicDataManager;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public abstract class ArrayDataManager extends BasicDataManager {

    private AtomicReference<JsonArray> currentArray = new AtomicReference<>(new JsonArray());

    protected void setCurrentArray(JsonArray array) {
        currentArray.set(array);
    }

    protected JsonArray getCurrentArray() {
        return currentArray.get();
    }

    protected ArrayDataManager(Starter starter) {
        super(starter);
    }

    public Optional<JsonArray> getCurrentState() {
        if (!running) return Optional.empty();
        JsonArray currentArray = getCurrentArray();
        if (currentArray != null) return Optional.of(currentArray);
        Optional<JsonArray> newState = fetchCurrentState();
        newState.ifPresent(this::setCurrentArray);
        return newState;
    }

    protected abstract Optional<JsonArray> fetchCurrentState();

    @Override
    public void stop() {
        super.stop();
        currentArray = null;
    }

    @Override
    public void reset() {
        if (!running) return;
        currentArray = null;
        sendCurrentState();
    }
}
