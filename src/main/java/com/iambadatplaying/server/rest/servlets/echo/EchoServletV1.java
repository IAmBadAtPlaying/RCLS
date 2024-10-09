package com.iambadatplaying.server.rest.servlets.echo;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iambadatplaying.Util;
import com.iambadatplaying.server.rest.servlets.ServletUtils;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

@Path("/v1")
/* This is just a reference echo servlet to test / find out how the general structure of the servlets works
 *
 *
 * */
public class EchoServletV1 {

    private static final String KEY_ECHO = "echo";

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
