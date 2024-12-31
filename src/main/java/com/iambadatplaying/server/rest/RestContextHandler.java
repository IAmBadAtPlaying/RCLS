package com.iambadatplaying.server.rest;

import com.iambadatplaying.Starter;
import com.iambadatplaying.server.rest.providers.GsonJsonElementMessageBodyReader;
import com.iambadatplaying.server.rest.providers.GsonJsonElementMessageBodyWriter;
import com.iambadatplaying.server.rest.servlets.debug.DebugServletV1;
import com.iambadatplaying.server.rest.servlets.games.GameControlServlet;
import com.iambadatplaying.server.rest.servlets.login.LoginServletV1;
import com.iambadatplaying.server.rest.servlets.valolytics.ValolyticsServletV1;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;

public class RestContextHandler extends ServletContextHandler {
    private final Starter starter;

    public static final String KEY_CONTEXT_STARTER = "starter";

    public RestContextHandler(Starter starter) {
        super(SESSIONS);
        this.starter = starter;
        addAllServlets();
    }

    private void addAllServlets() {
        getServletContext().setAttribute(KEY_CONTEXT_STARTER, starter);
        addLoginServlet();
        addEchoServlet();
        addLaunchGameServlet();
        addExperimentalServlet();
    }

    private void addExperimentalServlet() {
        StringBuilder sb = new StringBuilder();
        buildProviderList(
                sb,
                GsonJsonElementMessageBodyReader.class,
                GsonJsonElementMessageBodyWriter.class
        );

        buildServletList(
                sb,
                ValolyticsServletV1.class
        );

        sb.delete(sb.length() - 1, sb.length());
        ServletHolder echoServletHolder = addServlet(ServletContainer.class, "/experimental/*");
        echoServletHolder.setInitOrder(0);
        echoServletHolder.setInitParameter(
                "jersey.config.server.provider.classnames",
                sb.toString()
        );
    }

    private void addEchoServlet() {
        StringBuilder sb = new StringBuilder();
        buildProviderList(
                sb,
                GsonJsonElementMessageBodyReader.class,
                GsonJsonElementMessageBodyWriter.class
        );

        buildServletList(
                sb,
                DebugServletV1.class
        );

        sb.delete(sb.length() - 1, sb.length());
        ServletHolder echoServletHolder = addServlet(ServletContainer.class, "/debug/*");
        echoServletHolder.setInitOrder(0);
        echoServletHolder.setInitParameter(
                "jersey.config.server.provider.classnames",
                sb.toString()
        );
    }

    private void addLoginServlet() {
        StringBuilder sb = new StringBuilder();
        buildProviderList(
                sb,
                GsonJsonElementMessageBodyReader.class,
                GsonJsonElementMessageBodyWriter.class
        );

        buildServletList(
                sb,
                LoginServletV1.class
        );

        sb.delete(sb.length() - 1, sb.length());
        ServletHolder loginServletHolder = addServlet(ServletContainer.class, "/login/*");
        loginServletHolder.setInitOrder(0);
        loginServletHolder.setInitParameter(
                "jersey.config.server.provider.classnames",
                sb.toString()
        );
    }

    private void addLaunchGameServlet() {
        StringBuilder sb = new StringBuilder();
        buildProviderList(
                sb,
                GsonJsonElementMessageBodyReader.class,
                GsonJsonElementMessageBodyWriter.class
        );

        buildServletList(
                sb,
                GameControlServlet.class
        );

        sb.delete(sb.length() - 1, sb.length());
        ServletHolder loginServletHolder = addServlet(ServletContainer.class, "/games/*");
        loginServletHolder.setInitOrder(0);
        loginServletHolder.setInitParameter(
                "jersey.config.server.provider.classnames",
                sb.toString()
        );
    }


    private static void buildGenericList(StringBuilder sb, Class<?>... classes) {
        for (Class<?> c : classes) {
            if (c == null) continue;
            sb.append(c.getCanonicalName());
            sb.append(",");
        }
    }

    private static void buildServletList(StringBuilder sb, Class<?>... classes) {
        for (Class<?> c : classes) {
            if (c == null) continue;
            if (!c.isAnnotationPresent(javax.ws.rs.Path.class)) {
                throw new IllegalArgumentException("Class " + c.getCanonicalName() + " is not annotated with @Path");
            }
            sb.append(c.getCanonicalName());
            sb.append(",");
        }
    }

    private static void buildProviderList(StringBuilder sb, Class<?>... classes) {
        for (Class<?> c : classes) {
            if (c == null) continue;
            if (!c.isAnnotationPresent(javax.ws.rs.ext.Provider.class)) {
                throw new IllegalArgumentException("Class " + c.getCanonicalName() + " is not annotated with @Provider");
            }
            sb.append(c.getCanonicalName());
            sb.append(",");
        }
    }
}
