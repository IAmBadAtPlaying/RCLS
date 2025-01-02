package com.iambadatplaying.data.object;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.iambadatplaying.Starter;
import com.iambadatplaying.data.BasicDataManager;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public abstract class ObjectDataManager extends BasicDataManager {

    private AtomicReference<JsonObject> currentState = new AtomicReference<>(new JsonObject());

    protected void setCurrentState(JsonObject newState) {
        currentState.set(newState);
    }

    protected JsonObject getCurrentStateObject() {
        return currentState.get();
    }

    protected ObjectDataManager(Starter starter) {
        super(starter);
    }

    public Optional<JsonObject> getCurrentState() {
        if (!running) return Optional.empty();
        if (currentState != null) return Optional.of(currentState.get());
        Optional<JsonObject> newState = fetchCurrentState();
        newState.ifPresent(currentState::set);
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
