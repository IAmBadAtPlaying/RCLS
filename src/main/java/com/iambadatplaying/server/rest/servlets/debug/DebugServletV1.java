package com.iambadatplaying.server.rest.servlets.debug;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iambadatplaying.Starter;
import com.iambadatplaying.Util;
import com.iambadatplaying.rcconnection.RCConnectionState;
import com.iambadatplaying.server.rest.RestContextHandler;
import com.iambadatplaying.server.rest.servlets.ServletUtils;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;
import java.util.OptionalInt;

@Path("/v1")
/* This is just a reference echo servlet to test / find out how the general structure of the servlets works
 *
 *
 * */
public class DebugServletV1 {

    private static final String KEY_ECHO = "echo";

    @Context
    private ServletContext context;

    @POST
    @Path("/shutdown")
    @Produces(MediaType.APPLICATION_JSON)
    public Response shutdown() {
        Starter starter = (Starter) context.getAttribute(RestContextHandler.KEY_CONTEXT_STARTER);

        if (starter == null) {
            return Response
                    .status(Response.Status.SERVICE_UNAVAILABLE)
                    .build();
        }


        new Thread(
                () -> {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    starter.stop();
                }
        ).start();


        return Response
                .status(Response.Status.OK)
                .entity(ServletUtils.createResponseJson("Shutting down").toString())
                .build();
    }

    @GET
    @Path("/processInfo")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getprocessInfo() {
        Starter starter = (Starter) context.getAttribute(RestContextHandler.KEY_CONTEXT_STARTER);

        if (starter == null || starter.getRCConnector().getConnectionState() != RCConnectionState.CONNECTED) {
            return Response
                    .status(Response.Status.SERVICE_UNAVAILABLE)
                    .build();
        }

        OptionalInt optRCProcessId = starter.getRCConnector().getProcessHandler().getRiotClientServicesProcessId();
        if (!optRCProcessId.isPresent()) {
            return Response
                    .status(Response.Status.SERVICE_UNAVAILABLE)
                    .build();
        }

        int rcsPid = optRCProcessId.getAsInt();
        int rcsPort = starter.getRCConnector().getPort();
        String rcsAuth = starter.getRCConnector().getSecret();

        JsonObject body = new JsonObject();

        body.addProperty("processId", rcsPid);
        body.addProperty("port", rcsPort);
        body.addProperty("auth", rcsAuth);

        return Response
                .status(Response.Status.OK)
                .entity(body)
                .build();
    }

    @POST
    @Path("/echo")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response echo(JsonElement jsonElement) {
        Optional<JsonObject> optJsonData = Util.getAsJsonObject(jsonElement);
        if (!optJsonData.isPresent()) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(ServletUtils.createResponseJson("Invalid JSON data").toString())
                    .build();
        }

        JsonObject jsonData = optJsonData.get();
        if (!Util.jsonKeysPresent(jsonData, KEY_ECHO)) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(ServletUtils.createResponseJson("Missing key: " + KEY_ECHO).toString())
                    .build();
        }

        JsonElement echo = jsonData.get(KEY_ECHO);
        if (!echo.isJsonPrimitive()) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(ServletUtils.createResponseJson("Invalid value for key: " + KEY_ECHO).toString())
                    .build();
        }

        return Response
                .status(Response.Status.OK)
                .entity(ServletUtils.createResponseJson("Echo received", jsonData.get(KEY_ECHO).getAsString()))
                .build();
    }
}
