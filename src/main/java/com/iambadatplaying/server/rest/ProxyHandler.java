package com.iambadatplaying.server.rest;

import com.iambadatplaying.Starter;
import com.iambadatplaying.rcconnection.RCConnectionManager;
import com.iambadatplaying.rcconnection.RCConnectionState;
import com.iambadatplaying.server.LocalServer;
import com.iambadatplaying.server.rest.servlets.ServletUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ProxyHandler extends AbstractHandler {

    private final Starter starter;

    public ProxyHandler(Starter starter) {
        this.starter = starter;
    }

    @Override
    public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
        String originHeader = request.getHeader("Origin");
        if (LocalServer.filterOrigin(originHeader)) {
            httpServletResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            httpServletResponse.getWriter().write(ServletUtils.createResponseJson("Forbidden", "Origin not allowed").toString());
            request.setHandled(true);
            return;
        }

        if (starter.getRCConnector().getConnectionState() != RCConnectionState.CONNECTED) {
            httpServletResponse.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            httpServletResponse.getWriter().write(ServletUtils.createResponseJson("Service Unavailable", "Riot Client connection not established").toString());
            request.setHandled(true);
            return;
        }

        StringBuilder requestPath = new StringBuilder();
        requestPath.append(s);
        appendRequestParameters(requestPath, httpServletRequest.getParameterMap());

        StringBuilder requestBodyBuilder = new StringBuilder();
        httpServletRequest.getReader().lines().forEach(requestBodyBuilder::append);

        // CORS-Preflight
        if ("OPTIONS".equals(request.getMethod())) {
            httpServletResponse.setStatus(200);
            httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");
            httpServletResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH");
            request.setHandled(true);
            return;
        }

        Optional<RCConnectionManager.Method> method = RCConnectionManager.Method.fromString(request.getMethod());
        if (!method.isPresent()) {
            httpServletResponse.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            httpServletResponse.getWriter().write(ServletUtils.createResponseJson("Method Not Allowed", "Method not supported").toString());
            request.setHandled(true);
            return;
        }

        HttpsURLConnection connection = starter.getRCConnector().getRCConnectionManager().buildConnection(
                method.get(),
                requestPath.toString(),
                requestBodyBuilder.toString()
        );
        if (connection == null) {
            httpServletResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            httpServletResponse.getWriter().write(ServletUtils.createResponseJson("Internal Server Error", "Failed to establish connection").toString());
            request.setHandled(true);
            return;
        }

        byte[] responseBytes = readBytesFromStream(ServletUtils.handleStreamResponse(connection));
        Map<String, List<String>> headers = connection.getHeaderFields();

        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String key = entry.getKey();
            List<String> value = entry.getValue();
            if (key == null || value == null) continue;
            switch (key) {
                case "Cache-Control":
                case "Access-Control-Allow-Origin":
                case "Access-Control-Allow-Methods":
                case "access-control-expose-headers":
                    continue;
                default:
                    if (value.isEmpty()) continue;
                    httpServletResponse.setHeader(key, value.get(0));
                    break;
            }
        }

        httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");
        httpServletResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH");
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        httpServletResponse.getOutputStream().write(responseBytes);
        httpServletResponse.getOutputStream().flush();
        request.setHandled(true);
    }

    private byte[] readBytesFromStream(InputStream inputStream) throws IOException {
        if (inputStream == null) return new byte[0];
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int bytesRead;
        byte[] data = new byte[8192];
        while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, bytesRead);
        }
        return buffer.toByteArray();
    }


    private void appendRequestParameters(StringBuilder sb, Map<String, String[]> params) {
        if (params == null || params.isEmpty()) return;
        sb.append("?");
        for (Map.Entry<String, String[]> entry : params.entrySet()) {
            for (String value : entry.getValue()) {
                sb.append(entry.getKey()).append("=").append(value).append("&");
            }
        }
        sb.delete(sb.length() - 1, sb.length());
    }
}
