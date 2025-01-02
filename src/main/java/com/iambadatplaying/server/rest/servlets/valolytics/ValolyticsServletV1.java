package com.iambadatplaying.server.rest.servlets.valolytics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iambadatplaying.Starter;
import com.iambadatplaying.Util;
import com.iambadatplaying.data.map.SessionManager;
import com.iambadatplaying.data.map.ValorantMatchDataManager;
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
import java.net.URL;
import java.util.Optional;
import java.util.Set;

@Path("/v1")
public class ValolyticsServletV1 {

    private static final String KEY_ARES_DEPLOYMENT = "-ares-deployment=";

    private static final String  MAGIC_PLATFORM_STRING  = "ew0KCSJwbGF0Zm9ybVR5cGUiOiAiUEMiLA0KCSJwbGF0Zm9ybU9TIjogIldpbmRvd3MiLA0KCSJwbGF0Zm9ybU9TVmVyc2lvbiI6ICIxMC4wLjE5MDQyLjEuMjU2LjY0Yml0IiwNCgkicGxhdGZvcm1DaGlwc2V0IjogIlVua25vd24iDQp9";

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

        SessionManager sessionManager = (SessionManager) starter.getDataManger().getMapDataManager(SessionManager.class);

        JsonArray sessions = sessionManager.getSessionsByGame(Game.VALORANT);
        if (sessions == null || sessions.isEmpty()) {
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ServletUtils.createResponseJson("Failed to get session", "No Valorant session found"))
                    .build();
        }

        JsonObject valorantSession = sessions.get(0).getAsJsonObject();
        final String valorantVersionId = valorantSession.get("version").getAsString();
        final JsonObject launchConfiguration = valorantSession.get("launchConfiguration").getAsJsonObject();
        final JsonArray arguments = launchConfiguration.get("arguments").getAsJsonArray();

        String deploymentRegion = null;
        for (JsonElement element : arguments) {
            String argument = element.getAsString();
            if (!argument.startsWith(KEY_ARES_DEPLOYMENT)) continue;
            deploymentRegion = argument.substring(KEY_ARES_DEPLOYMENT.length());
        }

        if (deploymentRegion == null) {
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ServletUtils.createResponseJson("Failed to get config endpoint", "Failed to get config endpoint from launch configuration"))
                    .build();
        }

        JsonObject accumulator = new JsonObject();
        for (String matchId : matchIds) {
            try {
                HttpsURLConnection matchInfoCon = (HttpsURLConnection) new URL("https://pd." + deploymentRegion + ".a.pvp.net/match-details/v1/matches/" + matchId).openConnection();
                matchInfoCon.setRequestMethod("GET");
                matchInfoCon.setRequestProperty("User-Agent", "PostmanRuntime/7.43.0");
                matchInfoCon.setRequestProperty("Accept", "*/*");
                matchInfoCon.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
                matchInfoCon.setRequestProperty("Connection", "keep-alive");
                matchInfoCon.setRequestProperty("Authorization", "Bearer " + accessToken);
                matchInfoCon.setRequestProperty("X-Riot-ClientPlatform", MAGIC_PLATFORM_STRING);
                matchInfoCon.setRequestProperty("X-Riot-ClientVersion", valorantVersionId);
                matchInfoCon.setRequestProperty("X-Riot-Entitlements-JWT", entitlementToken);

                Optional<JsonElement> optMatchInfo = Util.getGZIPInputStream(matchInfoCon).flatMap(Util::inputStreamToString).flatMap(Util::parseJson);
                con.disconnect();
                optMatchInfo.ifPresent(matchInfo -> accumulator.add(matchId, matchInfo));
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
