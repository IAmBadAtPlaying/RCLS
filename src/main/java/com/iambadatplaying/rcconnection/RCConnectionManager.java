package com.iambadatplaying.rcconnection;

import com.iambadatplaying.EXIT_CODE;
import com.iambadatplaying.Managable;
import com.iambadatplaying.Starter;
import com.iambadatplaying.logger.LogLevel;
import com.iambadatplaying.logger.Loggable;
import com.iambadatplaying.logger.SimpleLogger;

import javax.net.ssl.HttpsURLConnection;
import javax.ws.rs.core.MediaType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public class RCConnectionManager implements Loggable {
    private Starter starter;

    public RCConnectionManager(Starter starter) {
        this.starter = starter;
        if (!allowHttpPatchMethod()) starter.exit(EXIT_CODE.HTTP_PATCH_SETUP_FAILED);
    }

    private boolean allowHttpPatchMethod() {
        try {
            Field declaredFieldMethods = HttpURLConnection.class.getDeclaredField("methods");
            Field declaredFieldModifiers = Field.class.getDeclaredField("modifiers");
            declaredFieldModifiers.setAccessible(true);
            declaredFieldModifiers.setInt(declaredFieldMethods, declaredFieldMethods.getModifiers() & ~Modifier.FINAL);
            declaredFieldMethods.setAccessible(true);
            String[] previousMethods = (String[]) declaredFieldMethods.get(null);
            Set<String> currentMethods = new LinkedHashSet<>(Arrays.asList(previousMethods));
            currentMethods.add("PATCH");
            String[] patched = currentMethods.toArray(new String[0]);
            declaredFieldMethods.set(null, patched);
            return true;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            System.out.println("Failed to allow HTTP PATCH method");
        }
        return false;
    }

    public enum Method {
        GET("GET"),
        POST("POST"),
        PUT("PUT"),
        DELETE("DELETE"),
        PATCH("PATCH");

        final String method;

        Method(String method) {
            this.method = method;
        }

        public String getMethod() {
            return method;
        }

        public static Optional<Method> fromString(String method) {
            for (Method m : Method.values()) {
                if (m.method.equals(method)) {
                    return Optional.of(m);
                }
            }
            return Optional.empty();
        }
    }

    public HttpsURLConnection buildConnection(
            Method method,
            String path,
            String postBody
    ) {
        String authToken = starter.getRCConnector().getAuthToken();
        Integer port = starter.getRCConnector().getPort();
        if (authToken == null || port == null) return null;
        return doBuildConnection(method, path, postBody, authToken, port);
    }

    private HttpsURLConnection doBuildConnection(Method method, String path, String postBody, String authToken, int port) {
        if (method == null || path == null) return null;
        String body = (postBody == null) ? "" : postBody;

        try {
            URL rcUrl = new URL("https://127.0.0.1:" + port + path);
            HttpsURLConnection connection = (HttpsURLConnection) rcUrl.openConnection();
            if (connection == null) return null;
            connection.setRequestMethod(method.getMethod());
            connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);
            connection.setRequestProperty("Authorization", authToken);
            connection.setConnectTimeout(1000);
            switch (method) {
                case POST:
                case PUT:
                case PATCH:
                    //Yes this is not consistent with some standards, but the DELETE method is allowed to have a body
                case DELETE:
                    connection.setDoOutput(true);
                    connection.getOutputStream().write(body.getBytes());
                    break;
                default:
            }

            return connection;
        } catch (Exception e) {
        }
        return null;
    }

    public boolean checkConnection(int port, String authToken) {
        return doCheckConnection(authToken, port, 0);
    }

    private boolean doCheckConnection(String authToken, int port, int tries) {
        if (tries > 3) return false;
        try {
            Thread.sleep(1000);
            HttpsURLConnection connection = doBuildConnection(Method.GET, "/riotclient/app-name", null, authToken, port);
            if (connection == null) return false;
            connection.connect();
            return connection.getResponseCode() == 200;
        } catch (Exception e) {
        }
        return doCheckConnection(authToken, port, tries + 1);
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
