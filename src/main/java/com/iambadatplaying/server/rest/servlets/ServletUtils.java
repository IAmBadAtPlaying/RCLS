package com.iambadatplaying.server.rest.servlets;

import com.google.gson.JsonObject;
import com.iambadatplaying.Util;

import javax.net.ssl.HttpsURLConnection;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Optional;
import java.util.OptionalInt;

public class ServletUtils {
    private ServletUtils() {}

    public static JsonObject createResponseJson(String message, String details) {
        JsonObject response = new JsonObject();
        response.addProperty("message", message);
        if (details != null) {
            response.addProperty("details", details);
        }
        return response;
    }

    public static JsonObject createResponseJson(String message, JsonObject data) {
        JsonObject response = new JsonObject();
        response.addProperty("message", message);
        if (data != null) {
            response.add("data", data);
        }
        return response;
    }

    public static JsonObject createResponseJson(String message) {
        return createResponseJson(message,(String) null);
    }

    public static OptionalInt getResponseCode(HttpURLConnection con) {
        try {
            return OptionalInt.of(con.getResponseCode());
        } catch (IOException e) {
            return OptionalInt.empty();
        }
    }

    public static InputStream handleStreamResponse(HttpURLConnection con) {
        InputStream is = null;
        try {
            if (100 <= con.getResponseCode() && con.getResponseCode() <= 399) {
                is = con.getInputStream();
            } else {
                is = con.getErrorStream();
            }
        } catch (IOException e) {}
        return is;
    }

    public static Optional<String> readConnection(HttpsURLConnection con) {
        return Util.inputStreamToString(handleStreamResponse(con));
    }

    public static Response createStandardProcessingErrorResponse() {
        return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(createResponseJson("Internal Server Error"))
                .build();
    }
}
