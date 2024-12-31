package com.iambadatplaying.server.rest;

import com.iambadatplaying.Starter;
import com.iambadatplaying.logger.LogLevel;
import com.iambadatplaying.logger.Loggable;
import com.iambadatplaying.logger.SimpleLogger;
import com.iambadatplaying.modules.BasicModule;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;

public class BasicModuleContextHandler extends ServletContextHandler implements Loggable {
    private final Starter starter;

    public static final String KEY_CONTEXT_STARTER = "starter";

    public BasicModuleContextHandler(Starter starter) {
        super(SESSIONS);
        this.starter = starter;
    }

    public void addAllServlets() {
        getServletContext().setAttribute(KEY_CONTEXT_STARTER, starter);
        addModules();
    }

    private void addModules() {
        log("Adding Modules");
        for (BasicModule module : starter.getBasicModuleLoader().getModules()) {
            log("Adding Module: " + module.getClass().getSimpleName());
            ServletHolder servletHolder = new ServletHolder(ServletContainer.class);
            servletHolder.setInitOrder(0);
            servletHolder.setInitParameter(
                    "jersey.config.server.provider.classnames",
                    buildModuleProviderList(module)
            );
            addServlet(servletHolder, module.getRestPath() + "/*");
            log("Module " + module.getClass().getSimpleName() + " added at " + getContextPath() + module.getRestPath());
        }
    }

    private String buildModuleProviderList(BasicModule configModule) {
        StringBuilder sb = new StringBuilder();
        for (Class<?> c : configModule.getServletConfiguration()) {
            if (c == null) continue;
            sb.append(c.getCanonicalName());
            sb.append(",");
        }
        sb.append(configModule.getRestServlet().getCanonicalName());
        return sb.toString();
    }

    @Override
    public void log(Object o) {
        log(LogLevel.DEBUG, o);
    }

    @Override
    public void log(LogLevel level, Object o) {
        SimpleLogger.getInstance().log(level,this.getClass().getSimpleName() + ": " + o);
    }
}
