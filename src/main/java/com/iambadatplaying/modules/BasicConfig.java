package com.iambadatplaying.modules;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.util.Optional;

public interface BasicConfig {
    long getVersion();
    default JsonElement getJson() {
        return new Gson().toJsonTree(this);
    }
}
