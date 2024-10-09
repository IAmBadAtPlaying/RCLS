package com.iambadatplaying.server.rest.servlets.games;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iambadatplaying.Starter;
import com.iambadatplaying.Util;
import com.iambadatplaying.rcconnection.RCConnectionManager;
import com.iambadatplaying.rcconnection.RCConnectionState;
import com.iambadatplaying.rcconnection.process.Game;
import com.iambadatplaying.server.rest.RestContextHandler;
import com.iambadatplaying.server.rest.servlets.ServletUtils;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Optional;

@Path("/v1")
public class GameControlServlet {

    @Context
    private ServletContext context;

    @Produces(MediaType.APPLICATION_JSON)
    @GET
    @Path("/games")
    public Response getAvailableGames() {
        JsonArray games = new JsonArray();
        for (Game game : Game.values()) {
            games.add(game.getAsJsonObject());
        }
        return Response
                .status(Response.Status.OK)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(games)
                .build();
    }

    @POST
    @Path("/launch/{games}")
    public Response launchGame(@PathParam("games") String game) {
        Optional<Game> optGame = Game.getByInternalName(game);
        if (!optGame.isPresent()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ServletUtils.createResponseJson("Unknown Game Type", "Check if your requested game is currently supported"))
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build();
        }

        Starter starter = (Starter) context.getAttribute(RestContextHandler.KEY_CONTEXT_STARTER);

        boolean available = starter.getRCConnector().getConnectionState() == RCConnectionState.CONNECTED;

        if (!available) {
            return Response
                    .status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(ServletUtils.createResponseJson("Service Unavailable", "Riot Client connection not established"))
                    .build();
        }

        Game launchGame = optGame.get();
        switch (launchGame) {
            case RIOT_CLIENT:
                return startRiotClient(starter);
            default:
                return startDefaultGame(starter, launchGame);
        }
    }

    private Response startDefaultGame(Starter starter, Game launchGame) {
        HttpsURLConnection con = starter.getRCConnector().getRCConnectionManager().buildConnection(RCConnectionManager.Method.POST, "/product-launcher/v1/products/"+launchGame.getInternalName()+"/patchlines/live", null);
        if (con == null) {
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ServletUtils.createResponseJson("Failed to launch game", "Failed to establish connection to Riot Client"))
                    .build();
        }

        Optional<JsonElement> getElement = Util.getInputStream(con).flatMap(Util::inputStreamToString).flatMap(Util::parseJson);
        if (!getElement.isPresent()) {
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ServletUtils.createResponseJson("Failed to launch game", "Failed to parse response from Riot Client"))
                    .build();
        }

        JsonElement obj = getElement.get();
        if (!obj.isJsonPrimitive() || !obj.getAsJsonPrimitive().isString()) {
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ServletUtils.createResponseJson("Failed to launch game", "Unexpected response from Riot Client"))
                    .build();
        }

        return Response
                .status(Response.Status.OK)
                .entity(ServletUtils.createResponseJson("Game launched", obj.getAsString()))
                .build();
    }

    private Response startRiotClient(Starter starter) {
        if (!starter.getRCConnector().getProcessHandler().launchRiotClient(starter.getRCConnector().getPort(), starter.getRCConnector().getSecret())) {
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ServletUtils.createResponseJson("Failed to launch Riot Client", "Failed to establish connection to Riot Client"))
                    .build();
        }

        return Response
                .status(Response.Status.OK)
                .entity(ServletUtils.createResponseJson("Riot Client launched"))
                .build();
    }
}
