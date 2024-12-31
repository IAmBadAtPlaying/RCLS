package com.iambadatplaying.modules;

import com.google.gson.JsonElement;

import javax.ws.rs.GET;
import javax.ws.rs.core.Response;

public interface BasicServlet {

    @GET
    Response getConfig();
}
