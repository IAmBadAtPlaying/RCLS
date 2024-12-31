package com.iambadatplaying.server.rest.servlets.login;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iambadatplaying.Starter;
import com.iambadatplaying.Util;
import com.iambadatplaying.rcconnection.RCConnectionManager;
import com.iambadatplaying.rcconnection.RCConnectionState;
import com.iambadatplaying.server.rest.RestContextHandler;
import com.iambadatplaying.server.rest.servlets.ServletUtils;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;
import java.util.OptionalInt;

@Path("/v1")
public class LoginServletV1 {
    private static final JsonObject RSO_AUTH_V2_GRANT_OBJECT                     = new JsonObject();
    private static final JsonObject RSO_AUTHENTICATOR_RIOT_IDENTITY_START_OBJECT = new JsonObject();

    static {
        buildJsonElements();
    }

    private static void buildJsonElements() {
        buildRsoAuthV2GrantObject();
        buildRsoAuthenticatorRiotIdentityStartObject();
    }

    private static void buildRsoAuthV2GrantObject() {
        RSO_AUTH_V2_GRANT_OBJECT.addProperty("clientId", "riot-client");
        JsonArray trustLevels = new JsonArray();
        trustLevels.add("always_trusted");
        RSO_AUTH_V2_GRANT_OBJECT.add("trustLevels", trustLevels);
    }

    private static void buildRsoAuthenticatorRiotIdentityStartObject() {
        RSO_AUTHENTICATOR_RIOT_IDENTITY_START_OBJECT.addProperty("language", "en_GB");
        RSO_AUTHENTICATOR_RIOT_IDENTITY_START_OBJECT.addProperty("product-id", "riot-client");
        RSO_AUTHENTICATOR_RIOT_IDENTITY_START_OBJECT.addProperty("state", "auth");
    }

    private static JsonObject createSessionLoginObject(String loginToken, boolean remember) {
        JsonObject sessionLoginObject = new JsonObject();
        sessionLoginObject.addProperty("authentication_type", "RiotAuth");
        sessionLoginObject.addProperty("login_token", loginToken);
        sessionLoginObject.addProperty("persist_login", remember);
        return sessionLoginObject;
    }

    private static JsonObject createExternalLoginObject(JsonObject loginDataObject) {
        JsonObject externalLoginObject = new JsonObject();
        Util.copyJsonAttributes(loginDataObject, externalLoginObject, "locale", "method");
        externalLoginObject.addProperty("product-id", "");
        externalLoginObject.addProperty("redirect_uri", "riotclient://auth/v1/{login_token}");
        externalLoginObject.addProperty("remember", false);
        externalLoginObject.addProperty("riot_theme", "riot");
        return externalLoginObject;
    }

    @Context
    private ServletContext context;

    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    public Response status() {

        Starter starter = (Starter) context.getAttribute(RestContextHandler.KEY_CONTEXT_STARTER);

        boolean available = starter.getRCConnector().getConnectionState() == RCConnectionState.CONNECTED;

        if (!available) {
            return Response
                    .status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(ServletUtils.createResponseJson("Service Unavailable", "Riot Client connection not established"))
                    .build();
        }

        return Response
                .status(Response.Status.OK)
                .entity(ServletUtils.createResponseJson("Service Available", "Riot Client connection established"))
                .build();
    }

    @POST
    @Path("/reset")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response resetLogin() {
        Starter starter = (Starter) context.getAttribute(RestContextHandler.KEY_CONTEXT_STARTER);

        boolean available = starter.getRCConnector().getConnectionState() == RCConnectionState.CONNECTED;

        if (!available) {
            return Response
                    .status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(ServletUtils.createResponseJson("Service Unavailable", "Riot Client connection not established"))
                    .build();
        }

        HttpsURLConnection grantsConnection = starter.getRCConnector().getRCConnectionManager().buildConnection(
                RCConnectionManager.Method.POST,
                "/rso-auth/v2/authorizations",
                RSO_AUTH_V2_GRANT_OBJECT.toString()
        );

        Optional<JsonObject> optGratsResponse = ServletUtils.readConnection(grantsConnection)
                .flatMap(Util::parseJson)
                .flatMap(Util::getAsJsonObject);

        grantsConnection.disconnect();

        if (!optGratsResponse.isPresent()) return ServletUtils.createStandardProcessingErrorResponse();
        JsonObject grantsResponse = optGratsResponse.get();
        if (!Util.jsonKeysPresent(grantsResponse, "authorization", "country", "type"))
            return ServletUtils.createStandardProcessingErrorResponse();

        Optional<String> optType = Util.getOptString(grantsResponse, "type");
        if (!optType.isPresent()) return ServletUtils.createStandardProcessingErrorResponse();
        if (!"needs_authentication".equals(optType.get())) return ServletUtils.createStandardProcessingErrorResponse();

        HttpsURLConnection deleteAuthenticationConnection = starter.getRCConnector().getRCConnectionManager().buildConnection(
                RCConnectionManager.Method.DELETE,
                "/rso-authenticator/v1/authentication",
                null
        );

        OptionalInt deleteAuthenticationResponseCode = ServletUtils.getResponseCode(deleteAuthenticationConnection);
        deleteAuthenticationConnection.disconnect();

        if (!deleteAuthenticationResponseCode.isPresent()) return ServletUtils.createStandardProcessingErrorResponse();
        if (deleteAuthenticationResponseCode.getAsInt() != 204)
            return ServletUtils.createStandardProcessingErrorResponse();


        return Response
                .status(Response.Status.OK)
                .entity(ServletUtils.createResponseJson("Reset successful", "Login reset successfully"))
                .build();
    }

    @GET
    @Path("/captcha")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCaptchaData() {
        Starter starter = (Starter) context.getAttribute(RestContextHandler.KEY_CONTEXT_STARTER);

        boolean available = starter.getRCConnector().getConnectionState() == RCConnectionState.CONNECTED;

        if (!available) {
            return Response
                    .status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(ServletUtils.createResponseJson("Service Unavailable", "Riot Client connection not established"))
                    .build();
        }

        HttpsURLConnection identStartConnection = starter.getRCConnector().getRCConnectionManager().buildConnection(
                RCConnectionManager.Method.POST,
                "/rso-authenticator/v1/authentication/riot-identity/start",
                RSO_AUTHENTICATOR_RIOT_IDENTITY_START_OBJECT.toString()
        );

        Optional<JsonObject> optIdentStartResponse = ServletUtils.readConnection(identStartConnection)
                .flatMap(Util::parseJson)
                .flatMap(Util::getAsJsonObject);

        identStartConnection.disconnect();
        if (!optIdentStartResponse.isPresent()) return ServletUtils.createStandardProcessingErrorResponse();
        JsonObject identStartResponse = optIdentStartResponse.get();
        if (Util.jsonKeysPresent(identStartResponse, "errorCode"))
            return ServletUtils.createStandardProcessingErrorResponse();
        if (!Util.jsonKeysPresent(identStartResponse, "captcha"))
            return ServletUtils.createStandardProcessingErrorResponse();

        JsonElement captchaElement = identStartResponse.get("captcha");
        if (!captchaElement.isJsonObject()) return ServletUtils.createStandardProcessingErrorResponse();
        JsonObject captchaObject = captchaElement.getAsJsonObject();

        return Response
                .status(Response.Status.OK)
                .entity(captchaObject)
                .build();
    }

    @POST
    @Path("/login/external")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response externalLogin(JsonElement loginData) {
        Starter starter = (Starter) context.getAttribute(RestContextHandler.KEY_CONTEXT_STARTER);

        boolean available = starter.getRCConnector().getConnectionState() == RCConnectionState.CONNECTED;

        if (!available) {
            return Response
                    .status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(ServletUtils.createResponseJson("Service Unavailable", "Riot Client connection not established"))
                    .build();
        }

        if (loginData == null || !loginData.isJsonObject()) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(ServletUtils.createResponseJson("Bad Request", "Invalid login data"))
                    .build();
        }

        JsonObject loginDataObject = loginData.getAsJsonObject();

        if (!Util.jsonKeysPresent(loginDataObject, "locale", "method")) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(ServletUtils.createResponseJson("Bad Request", "Some JSON keys are missing"))
                    .build();
        }

        JsonObject cleanLoginData = createExternalLoginObject(loginDataObject);

        HttpsURLConnection externalLoginConnection = starter.getRCConnector().getRCConnectionManager().buildConnection(
                RCConnectionManager.Method.POST,
                "/rso-authenticator/v1/web-authentication-url",
                cleanLoginData.toString()
        );

        Optional<String> optExternalLoginResponse = ServletUtils.readConnection(externalLoginConnection)
                .map(str -> str.replace("\"", ""))
                .map(String::trim);

        externalLoginConnection.disconnect();

        if (!optExternalLoginResponse.isPresent()) return ServletUtils.createStandardProcessingErrorResponse();
        String externalLoginUrl = optExternalLoginResponse.get();

        JsonObject response = new JsonObject();
        response.addProperty("url", externalLoginUrl);

        return Response
                .status(Response.Status.OK)
                .entity(ServletUtils.createResponseJson("Success", response))
                .build();
    }

    @POST
    @Path("/login")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(JsonElement loginData) {
        Starter starter = (Starter) context.getAttribute(RestContextHandler.KEY_CONTEXT_STARTER);

        boolean available = starter.getRCConnector().getConnectionState() == RCConnectionState.CONNECTED;

        if (!available) {
            return Response
                    .status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(ServletUtils.createResponseJson("Service Unavailable", "Riot Client connection not established"))
                    .build();
        }

        if (loginData == null || !loginData.isJsonObject()) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(ServletUtils.createResponseJson("Bad Request", "Invalid login data"))
                    .build();
        }

        JsonObject loginDataObject = loginData.getAsJsonObject();

        if (!Util.jsonKeysPresent(loginDataObject, "username", "password", "remember", "language", "captcha")) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(ServletUtils.createResponseJson("Bad Request", "Some JSON keys are missing"))
                    .build();
        }

        JsonObject cleanLoginData = new JsonObject();
        Util.copyJsonAttributes(loginDataObject, cleanLoginData, "username", "password", "remember", "language", "captcha");

        HttpsURLConnection loginConnection = starter.getRCConnector().getRCConnectionManager().buildConnection(
                RCConnectionManager.Method.POST,
                "/rso-authenticator/v1/authentication/riot-identity/complete",
                cleanLoginData.toString()
        );

        Optional<JsonObject> optLoginResponse = ServletUtils.readConnection(loginConnection)
                .flatMap(Util::parseJson)
                .flatMap(Util::getAsJsonObject);

        loginConnection.disconnect();

        if (!optLoginResponse.isPresent()) return ServletUtils.createStandardProcessingErrorResponse();
        JsonObject loginResponse = optLoginResponse.get();
        System.out.println(loginResponse);
        if (Util.jsonKeysPresent(loginResponse, "errorCode")) {
            return ServletUtils.createStandardProcessingErrorResponse();
        }


        Optional<String> optLoginToken = Util.getOptJSONObject(loginResponse, "success")
                .flatMap(success -> Util.getOptString(success, "login_token"));

        Optional<Boolean> optRemember = Util.getOptBool(loginDataObject, "remember");
        boolean remember = optRemember.orElse(false);

        if (!optLoginToken.isPresent() || optLoginToken.get().isEmpty()) {
            System.out.println(loginResponse);
            Optional<JsonObject> optMultifactor = Util.getOptJSONObject(loginResponse, "multifactor");
            if (!optMultifactor.isPresent()) return ServletUtils.createStandardProcessingErrorResponse();
            return handleMultifactor(starter, optMultifactor.get(), remember);
        }

        return handleNormalAuthflow(starter, loginResponse, remember);
    }

    @POST
    @Path("/logout")
    @Produces(MediaType.APPLICATION_JSON)
    public Response logout() {
        Starter starter = (Starter) context.getAttribute(RestContextHandler.KEY_CONTEXT_STARTER);

        boolean available = starter.getRCConnector().getConnectionState() == RCConnectionState.CONNECTED;

        if (!available) {
            return Response
                    .status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(ServletUtils.createResponseJson("Service Unavailable", "Riot Client connection not established"))
                    .build();
        }

        HttpsURLConnection logoutConnection = starter.getRCConnector().getRCConnectionManager().buildConnection(
                RCConnectionManager.Method.DELETE,
                "/rso-auth/v1/session",
                null
        );

        OptionalInt optLogoutResponseCode = ServletUtils.getResponseCode(logoutConnection);
        logoutConnection.disconnect();
        if (!optLogoutResponseCode.isPresent()) return ServletUtils.createStandardProcessingErrorResponse();
        int logoutResponseCode = optLogoutResponseCode.getAsInt();
        switch (logoutResponseCode) {
            case 204:
            case 200:
                return Response
                        .status(Response.Status.OK)
                        .entity(ServletUtils.createResponseJson("Logout successful", "Logout successful"))
                        .build();
            default:
                return Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity(ServletUtils.createResponseJson("Logout failed", "Check if other games are running"))
                        .build();
        }
    }

    @POST
    @Path("/multifactor")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response resolveMultifactor(JsonElement data) {
        Starter starter = (Starter) context.getAttribute(RestContextHandler.KEY_CONTEXT_STARTER);

        boolean available = starter.getRCConnector().getConnectionState() == RCConnectionState.CONNECTED;

        if (!available) {
            return Response
                    .status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(ServletUtils.createResponseJson("Service Unavailable", "Riot Client connection not established"))
                    .build();
        }

        if (data == null || !data.isJsonObject()) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(ServletUtils.createResponseJson("Bad Request", "Invalid data"))
                    .build();
        }

        JsonObject dataObject = data.getAsJsonObject();
        if (!Util.jsonKeysPresent(dataObject, "otp", "rememberDevice")) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(ServletUtils.createResponseJson("Bad Request", "Some JSON keys are missing"))
                    .build();
        }

        JsonObject cleanData = new JsonObject();
        Util.copyJsonAttributes(dataObject, cleanData, "otp", "rememberDevice");
        Optional<Boolean> optRemember = Util.getOptBool(dataObject, "rememberDevice");
        boolean remember = optRemember.orElse(false);

        JsonObject multifactorObject = new JsonObject();
        multifactorObject.add("multifactor", cleanData);

        HttpsURLConnection multifactorConnection = starter.getRCConnector().getRCConnectionManager().buildConnection(
                RCConnectionManager.Method.POST,
                "/rso-authenticator/v1/authentication/multifactor",
                multifactorObject.toString()
        );

        Optional<JsonObject> optMultifactorResponse = ServletUtils.readConnection(multifactorConnection)
                .flatMap(Util::parseJson)
                .flatMap(Util::getAsJsonObject);

        multifactorConnection.disconnect();

        if (!optMultifactorResponse.isPresent()) return ServletUtils.createStandardProcessingErrorResponse();
        JsonObject multifactorResponse = optMultifactorResponse.get();
        System.out.println(multifactorResponse);
        if (!Util.jsonKeysPresent(multifactorResponse, "type", "success"))
            return ServletUtils.createStandardProcessingErrorResponse();

        Optional<String> optType = Util.getOptString(multifactorResponse, "type");
        if (!optType.isPresent()) return ServletUtils.createStandardProcessingErrorResponse();
        if (!"success".equals(optType.get())) return ServletUtils.createStandardProcessingErrorResponse();

        return handleNormalAuthflow(starter, multifactorResponse, remember);

    }

    private Response handleNormalAuthflow(Starter starter, JsonObject successResponse, boolean remember) {
        Optional<String> optLoginToken = Util.getOptJSONObject(successResponse, "success").flatMap(success -> Util.getOptString(success, "login_token"));
        if (!optLoginToken.isPresent()) return ServletUtils.createStandardProcessingErrorResponse();
        String loginToken = optLoginToken.get();
        if (loginToken.isEmpty()) return ServletUtils.createStandardProcessingErrorResponse();
        JsonObject sessionLoginObject = createSessionLoginObject(loginToken, remember);
        System.out.println("Executing session login");
        HttpsURLConnection sessionLoginConnection = starter.getRCConnector().getRCConnectionManager().buildConnection(
                RCConnectionManager.Method.PUT,
                "/rso-auth/v1/session/login-token",
                sessionLoginObject.toString()
        );

        Optional<JsonObject> optSessionLoginResponse = ServletUtils.readConnection(sessionLoginConnection)
                .flatMap(Util::parseJson)
                .flatMap(Util::getAsJsonObject);

        if (!optSessionLoginResponse.isPresent()) return ServletUtils.createStandardProcessingErrorResponse();
        System.out.println(optSessionLoginResponse.get());

        Optional<String> sessionLoginResponse = Util.getOptString(optSessionLoginResponse.get(), "type");

        sessionLoginConnection.disconnect();

        if (!sessionLoginResponse.isPresent()) return ServletUtils.createStandardProcessingErrorResponse();
        if (!"authenticated".equals(sessionLoginResponse.get()))
            return ServletUtils.createStandardProcessingErrorResponse();


        HttpsURLConnection authGrantConnection = starter.getRCConnector().getRCConnectionManager().buildConnection(
                RCConnectionManager.Method.POST,
                "/rso-auth/v2/authorizations",
                RSO_AUTH_V2_GRANT_OBJECT.toString()
        );

        Optional<JsonObject> optAuthGrantResponse = ServletUtils.readConnection(authGrantConnection)
                .flatMap(Util::parseJson)
                .flatMap(Util::getAsJsonObject);

        authGrantConnection.disconnect();

        if (!optAuthGrantResponse.isPresent()) return ServletUtils.createStandardProcessingErrorResponse();
        JsonObject authGrantResponse = optAuthGrantResponse.get();
        if (!Util.jsonKeysPresent(authGrantResponse, "authorization", "country", "type"))
            return ServletUtils.createStandardProcessingErrorResponse();

        Optional<String> optType = Util.getOptString(authGrantResponse, "type");
        if (!optType.isPresent()) return ServletUtils.createStandardProcessingErrorResponse();

        if (!"authorized".equals(optType.get())) return ServletUtils.createStandardProcessingErrorResponse();

        return Response
                .status(Response.Status.OK)
                .entity(ServletUtils.createResponseJson("Login successful", "Login successful"))
                .build();

    }

    private Response handleMultifactor(Starter starter, JsonObject multifactorData, boolean remember) {
        if (!Util.jsonKeysPresent(multifactorData, "email", "method", "methods", "mode"))
            return ServletUtils.createStandardProcessingErrorResponse();

        Optional<String> optMode = Util.getOptString(multifactorData, "mode");
        if (!optMode.isPresent()) return ServletUtils.createStandardProcessingErrorResponse();
        if (!"auth".equals(optMode.get())) return ServletUtils.createStandardProcessingErrorResponse();

        JsonObject cleanMultifactorData = new JsonObject();
        Util.copyJsonAttributes(multifactorData, cleanMultifactorData, "email", "method", "methods", "mode");

        return Response
                .status(Response.Status.UNAUTHORIZED)
                .entity(ServletUtils.createResponseJson("Multifactor authentication required", cleanMultifactorData))
                .build();
    }
}
