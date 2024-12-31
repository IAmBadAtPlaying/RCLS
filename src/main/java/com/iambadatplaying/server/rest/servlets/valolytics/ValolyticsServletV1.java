package com.iambadatplaying.server.rest.servlets.valolytics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iambadatplaying.Starter;
import com.iambadatplaying.Util;
import com.iambadatplaying.data.map.ValorantMatchDataManager;
import com.iambadatplaying.logger.LogLevel;
import com.iambadatplaying.rcconnection.RCConnectionManager;
import com.iambadatplaying.rcconnection.RCConnectionState;
import com.iambadatplaying.rcconnection.process.Game;
import com.iambadatplaying.server.rest.RestContextHandler;
import com.iambadatplaying.server.rest.servlets.ServletUtils;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.net.URL;
import java.util.Optional;
import java.util.Set;
import java.util.zip.GZIPInputStream;

@Path("/v1")
public class ValolyticsServletV1 {

    private static final String MAGIC_PLATFORM_STRING = "ew0KCSJwbGF0Zm9ybVR5cGUiOiAiUEMiLA0KCSJwbGF0Zm9ybU9TIjogIldpbmRvd3MiLA0KCSJwbGF0Zm9ybU9TVmVyc2lvbiI6ICIxMC4wLjE5MDQyLjEuMjU2LjY0Yml0IiwNCgkicGxhdGZvcm1DaGlwc2V0IjogIlVua25vd24iDQp9";

    @Context
    private ServletContext context;

    @GET
    @Path("/matchIds")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMatchIds() {
        Starter starter = (Starter) context.getAttribute(RestContextHandler.KEY_CONTEXT_STARTER);

        ValorantMatchDataManager valorantMatchDataManager = (ValorantMatchDataManager) starter.getDataManger().getMapDataManager(ValorantMatchDataManager.class);
        if (valorantMatchDataManager == null) {
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ServletUtils.createResponseJson("Internal Server Error", "ValorantMatchDataManager not found"))
                    .build();
        }

        Set<String> matchIds = valorantMatchDataManager.getMatchIds();
        if (matchIds.isEmpty()) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(ServletUtils.createResponseJson("Not Found", "No match data found"))
                    .build();
        }

        JsonArray matchIdsJson = new JsonArray();
        matchIds.forEach(matchIdsJson::add);

        return Response
                .status(Response.Status.OK)
                .entity(matchIdsJson)
                .build();
    }

    @POST
    @Path("/accumulateMatchData")
    @Produces(MediaType.APPLICATION_JSON)
    public Response accumulateMatchData() {
        Starter starter = (Starter) context.getAttribute(RestContextHandler.KEY_CONTEXT_STARTER);

        boolean available = starter.getRCConnector().getConnectionState() == RCConnectionState.CONNECTED;

        if (!available) {
            return Response
                    .status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(ServletUtils.createResponseJson("Service Unavailable", "Riot Client connection not established"))
                    .build();
        }


        ValorantMatchDataManager valorantMatchDataManager = (ValorantMatchDataManager) starter.getDataManger().getMapDataManager(ValorantMatchDataManager.class);
        if (valorantMatchDataManager == null) {
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ServletUtils.createResponseJson("Internal Server Error", "ValorantMatchDataManager not found"))
                    .build();
        }

        Set<String> matchIds = valorantMatchDataManager.getMatchIds();
        if (matchIds.isEmpty()) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(ServletUtils.createResponseJson("Not Found", "No match data found"))
                    .build();
        }

        HttpsURLConnection entitlementCon = starter.getRCConnector().getRCConnectionManager().buildConnection(RCConnectionManager.Method.GET, "/entitlements/v1/token", null);
        if (entitlementCon == null) {
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ServletUtils.createResponseJson("Failed to build connection", "Failed to build connection to entitlements endpoint"))
                    .build();
        }

        Optional<JsonElement> optEntitlement = Util.getInputStream(entitlementCon).flatMap(Util::inputStreamToString).flatMap(Util::parseJson);
        if (!optEntitlement.isPresent()) {
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ServletUtils.createResponseJson("Failed to get entitlement", "Failed to get entitlement from response"))
                    .build();
        }

        JsonObject entitlement = optEntitlement.get().getAsJsonObject();
        if (!Util.jsonKeysPresent(entitlement, "accessToken", "token")) {
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ServletUtils.createResponseJson("Failed to get entitlement", "Entitlement response missing required keys"))
                    .build();
        }

        final String accessToken = entitlement.get("accessToken").getAsString();
        final String entitlementToken = entitlement.get("token").getAsString();

        HttpsURLConnection con = starter.getRCConnector().getRCConnectionManager().buildConnection(RCConnectionManager.Method.GET, "/product-session/v1/external-sessions", null);
        if (con == null) {
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ServletUtils.createResponseJson("Failed to build connection", "Failed to build connection to product-session endpoint"))
                    .build();
        }

        Optional<JsonElement> optProductSessions = Util.getInputStream(con).flatMap(Util::inputStreamToString).flatMap(Util::parseJson);
        if (!optProductSessions.isPresent()) {
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ServletUtils.createResponseJson("Failed to get product sessions", "Failed to get product sessions from response"))
                    .build();
        }

        JsonObject productSessions = optProductSessions.get().getAsJsonObject();
        Optional<JsonObject> optValorantSession = productSessions.entrySet().stream().map(entry -> entry.getValue().getAsJsonObject())
                .filter(obj -> {
                    if (!Util.jsonKeysPresent(obj, "productId", "version")) return false;
                    return Game.VALORANT.getInternalName().equals(obj.get("productId").getAsString());
                }).findFirst();

        if (!optValorantSession.isPresent()) {
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ServletUtils.createResponseJson("Failed to get valorant session", "Valorant session not found"))
                    .build();
        }

        JsonObject valorantSession = optValorantSession.get();
        final String valorantVersionId = valorantSession.get("version").getAsString();

        JsonObject accumulator = new JsonObject();
        for (String matchId : matchIds) {
            try {
                HttpsURLConnection matchInfoCon = (HttpsURLConnection) new URL("https://pd.eu.a.pvp.net/match-details/v1/matches/" + matchId).openConnection();
                matchInfoCon.setRequestMethod("GET");
                matchInfoCon.setRequestProperty("User-Agent", "PostmanRuntime/7.43.0");
                matchInfoCon.setRequestProperty("Accept", "*/*");
                matchInfoCon.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
                matchInfoCon.setRequestProperty("Connection", "keep-alive");
                matchInfoCon.setRequestProperty("Authorization", "Bearer " + accessToken);
                matchInfoCon.setRequestProperty("X-Riot-ClientPlatform", MAGIC_PLATFORM_STRING);
                matchInfoCon.setRequestProperty("X-Riot-ClientVersion", valorantVersionId);
                matchInfoCon.setRequestProperty("X-Riot-Entitlements-JWT", entitlementToken);

                Optional<String> optMatchInfo = Util.getGZIPInputStream(matchInfoCon).flatMap(Util::inputStreamToString);
                con.disconnect();
                starter.log(LogLevel.INFO, optMatchInfo.orElse("Failed to get match info"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return Response
                .status(Response.Status.OK)
                .entity(accumulator)
                .build();
    }
}