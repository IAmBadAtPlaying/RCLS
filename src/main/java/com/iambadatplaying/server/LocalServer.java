package com.iambadatplaying.server;

import com.iambadatplaying.EXIT_CODE;
import com.iambadatplaying.Managable;
import com.iambadatplaying.Starter;
import com.iambadatplaying.logger.LogLevel;
import com.iambadatplaying.logger.Loggable;
import com.iambadatplaying.logger.SimpleLogger;
import com.iambadatplaying.server.rest.ProxyHandler;
import com.iambadatplaying.server.rest.RestContextHandler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class LocalServer implements Loggable, Managable {

    public static final int APPLICATION_PORT = 35200;
    public static final int DEBUG_FRONTEND_PORT = 3000;
    public static final int DEBUG_FRONTEND_PORT_V2 = 3001;

    private static final Pattern localHostPattern = Starter.isDev ?
            Pattern.compile("^(http://)?(localhost|127\\.0\\.0\\.1):(" + APPLICATION_PORT + "|" + DEBUG_FRONTEND_PORT + "|" + DEBUG_FRONTEND_PORT_V2 + ")(/)?$") :
            Pattern.compile("^(http://)?(localhost|127\\.0\\.0\\.1):(" + APPLICATION_PORT + ")(/)?$");
    private static final ArrayList<Pattern> allowedOrigins = new ArrayList<>();

    private final Starter starter;
    private final List<LocalWebsocket> websocketConnections = Collections.synchronizedList(new ArrayList<>());
    private boolean running = false;

    private Server server = null;

    static {
        addAllowedOrigins();
    }

    public static void addAllowedOrigins() {
        //Allows certain origins to access the resource server.
        //For example, if an external website wants to access the resource server, it must be added here.
    }

    public static boolean filterWebSocketRequest(ServletUpgradeRequest req) {
        return filterOrigin(req.getOrigin());
    }

    /**
    * Determines if the origin of the request should be filtered / aborted.
    *
    * @param origin The origin of the request
    * @return True if the request should be filtered / aborted, false if it should be allowed
    * */
    public static boolean filterOrigin(String origin) {
        if (origin == null) return false;

        if (localHostPattern.matcher(origin).matches()) return false;

        if (!allowedOrigins.isEmpty()) {
            for (Pattern pattern : allowedOrigins) {
                if (pattern.matcher(origin).matches()) return false;
            }
        }


        return true;
    }

    private static boolean filterRequest(HttpServletRequest req, HttpServletResponse resp) {
        String origin = req.getHeader("Origin");

        //Either local host OR non-browser request
        if (!filterOrigin(origin)) {
            resp.setHeader("Access-Control-Allow-Origin", origin);
            resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            resp.setHeader("Access-Control-Allow-Headers", "Content-Type");

            return false;
        }

        return true;
    }

    public LocalServer(Starter starter) {
        this.starter = starter;
    }

    @Override
    public void start() {
        running = true;
        server =  new Server(APPLICATION_PORT);
        setupServerHandlers();
        try {
            server.start();
        } catch (IOException e) {
            starter.exit(EXIT_CODE.SERVER_BIND_FAILED);
        } catch (Exception e) {
            log(LogLevel.ERROR, e);
        }
    }

    public void awaitTermination() {
        try {
            server.join();
        } catch (Exception e) {

        }
    }

    public void sendToAllWebsockets(String message) {
        for (LocalWebsocket websocket : websocketConnections) {
            websocket.sendMessage(message);
        }
    }

    public void removeWebsocketConnection(LocalWebsocket websocket) {
        websocketConnections.remove(websocket);
    }

    public void addWebsocketConnection(LocalWebsocket websocket) {
        websocketConnections.add(websocket);
    }

    private void setupServerHandlers() {
        HandlerList handlers = new HandlerList();

        ServletContextHandler wsContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
        wsContext.setContextPath("/ws");
        handlers.addHandler(wsContext);

        WebSocketHandler wsHandler = new WebSocketHandler() {
            @Override
            public void configure(WebSocketServletFactory factory) {
                factory.setCreator((req, resp) -> {
                    if (filterWebSocketRequest(req)) {
                        try {
                            resp.sendForbidden("Origin not allowed");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        return null;
                    }

                    return new LocalWebsocket(starter);
                });
            }
        };
        wsContext.insertHandler(wsHandler);

        RestContextHandler restContext = new RestContextHandler(starter);
        restContext.setContextPath("/rest");
        handlers.addHandler(restContext);

        ContextHandler proxyContext = new ContextHandler();
        proxyContext.setContextPath("/proxy");
        proxyContext.setHandler(new ProxyHandler(starter));
        handlers.addHandler(proxyContext);

        server.setHandler(handlers);
    }

    @Override
    public void stop() {
        running = false;
        websocketConnections.forEach(LocalWebsocket::externalShutdown);
        websocketConnections.clear();
        server.destroy();
        log("Stopped Server");
    }


    @Override
    public void log(Object o) {
        log(LogLevel.DEBUG, o);
    }

    @Override
    public void log(LogLevel level, Object o) {
        SimpleLogger.getInstance().log(level,this.getClass().getSimpleName() + ": " + o);
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
