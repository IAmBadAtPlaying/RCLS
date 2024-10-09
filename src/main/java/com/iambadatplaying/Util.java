package com.iambadatplaying;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.UUID;

public final class Util {
    private Util() {}

    public static Optional<String> inputStreamToString(InputStream is) {
        if (is == null) return Optional.empty();
        StringBuilder result = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                result.append(line).append("\n");
            }
            is.close();
        } catch (IOException e) {
            return Optional.empty();
        }
        return Optional.of(result.toString());
    }

    public static Optional<JsonElement> parseJson(String json) {
        try {
            return Optional.of(JsonParser.parseString(json));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static Optional<JsonObject> getAsJsonObject(JsonElement jsonElement) {
        if (jsonElement == null || !jsonElement.isJsonObject()) return Optional.empty();
        return Optional.of(jsonElement.getAsJsonObject());
    }

    public static Optional<JsonArray> getAsJsonArray(JsonElement jsonElement) {
        if (jsonElement == null || !jsonElement.isJsonArray()) return Optional.empty();
        return Optional.of(jsonElement.getAsJsonArray());
    }

    public static boolean equalJsonElements(JsonElement a, JsonElement b) {
        if (a == null || b == null) return false;
        if (a.isJsonObject() && b.isJsonObject()) {
            return equalJsonObjects(a.getAsJsonObject(), b.getAsJsonObject());
        } else if (a.isJsonArray() && b.isJsonArray()) {
            return equalJsonArrays(a.getAsJsonArray(), b.getAsJsonArray());
        } else {
            return a.equals(b);
        }
    }

    private static boolean equalJsonObjects(JsonObject a, JsonObject b) {
        if (a == null || b == null) return false;
        if (a.entrySet().size() != b.entrySet().size()) return false;
        for (String key : a.keySet()) {
            if (!b.has(key)) return false;
            JsonElement aElement = a.get(key);
            JsonElement bElement = b.get(key);
            if (aElement.isJsonObject() && bElement.isJsonObject()) {
                if (!equalJsonObjects(aElement.getAsJsonObject(), bElement.getAsJsonObject())) return false;
            } else if (aElement.isJsonArray() && bElement.isJsonArray()) {
                if (!equalJsonArrays(aElement.getAsJsonArray(), bElement.getAsJsonArray())) return false;
            } else if (!aElement.equals(bElement)) return false;
        }
        return true;
    }

    private static boolean equalJsonArrays(JsonArray a, JsonArray b) {
        if (a == null || b == null) return false;
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            JsonElement aElement = a.get(i);
            JsonElement bElement = b.get(i);
            if (aElement.isJsonObject() && bElement.isJsonObject()) {
                if (!equalJsonObjects(aElement.getAsJsonObject(), bElement.getAsJsonObject())) return false;
            } else if (aElement.isJsonArray() && bElement.isJsonArray()) {
                if (!equalJsonArrays(aElement.getAsJsonArray(), bElement.getAsJsonArray())) return false;
            } else if (!aElement.equals(bElement)) return false;
        }
        return true;
    }

    public static boolean jsonKeysPresent(JsonObject jsonObject, String... attributes) {
        if (jsonObject == null || attributes == null) return false;
        for (String attribute : attributes) {
            if (!jsonObject.has(attribute)) return false;
        }
        return true;
    }

    public static void copyJsonAttrib(String key, JsonObject src, JsonObject dst) {
        if (src == null || dst == null) return;
        doCopyAttrib(key, src, dst);
    }

    private static void doCopyAttrib(String key, JsonObject src, JsonObject dst) {
        if (src.has(key)) {
            JsonElement object = src.get(key);
            if (object != null) {
                dst.add(key, object);
            }
        }
    }

    public static void copyJsonAttributes(JsonObject src, JsonObject dst, String... attributes) {
        if (src == null || dst == null || attributes == null) return;
        for (String attribute : attributes) {
            doCopyAttrib(attribute, src, dst);
        }
    }

    public static OptionalInt getOptInt(JsonObject jsonObject, String key) {
        if (jsonObject == null || !jsonObject.has(key)) return OptionalInt.empty();
        return OptionalInt.of(jsonObject.get(key).getAsInt());
    }

    public static Integer getInt(JsonObject jsonObject, String key, Integer defaultValue) {
        if (jsonObject == null || !jsonObject.has(key)) return defaultValue;
        return jsonObject.get(key).getAsInt();
    }

    public static OptionalLong getOptLong(JsonObject jsonObject, String key) {
        if (jsonObject == null || !jsonObject.has(key)) return OptionalLong.empty();
        return OptionalLong.of(jsonObject.get(key).getAsLong());
    }

    public static Optional<String> getOptString(JsonObject jsonObject, String key) {
        if (jsonObject == null || !jsonObject.has(key)) return Optional.empty();
        return Optional.of(jsonObject.get(key).getAsString());
    }

    public static String getString(JsonObject jsonObject, String key, String defaultValue) {
        if (jsonObject == null || !jsonObject.has(key)) return defaultValue;
        return jsonObject.get(key).getAsString();
    }

    public static Optional<Boolean> getOptBool(JsonObject jsonObject, String key) {
        if (jsonObject == null || !jsonObject.has(key)) return Optional.empty();
        return Optional.of(jsonObject.get(key).getAsBoolean());
    }

    public static Boolean getBoolean(JsonObject jsonObject, String key, Boolean defaultValue) {
        if (jsonObject == null || !jsonObject.has(key)) return defaultValue;
        return jsonObject.get(key).getAsBoolean();
    }

    public static Optional<JsonObject> getOptJSONObject(JsonObject jsonObject, String key) {
        if (jsonObject == null || !jsonObject.has(key)) return Optional.empty();
        return Optional.of(jsonObject.get(key).getAsJsonObject());
    }

    public static Optional<JsonArray> getOptJSONArray(JsonObject jsonObject, String key) {
        if (jsonObject == null || !jsonObject.has(key)) return Optional.empty();
        return Optional.of(jsonObject.get(key).getAsJsonArray());
    }

    public static int findFreePort() {
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(0);
            return socket.getLocalPort();
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                    //Wait for the OS to release the port
                    Thread.sleep(500);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String createSecret() {
        UUID uuid = UUID.randomUUID();
        String uuidString = uuid.toString();
        return uuidString.replace("-", "");
    }

    public static Optional<InputStream> getInputStream(HttpURLConnection connection) {
        try {
            return Optional.of(connection.getInputStream());
        } catch (IOException e) {
            try {
                return Optional.of(connection.getErrorStream());
            } catch (Exception ex) {}
        }

        return Optional.empty();
    }

    public static Optional<Integer> getResponseCode(HttpURLConnection connection) {
        try {
            return Optional.of(connection.getResponseCode());
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
