package com.iambadatplaying;

import com.iambadatplaying.data.DataManger;
import com.iambadatplaying.logger.LogLevel;
import com.iambadatplaying.logger.Loggable;
import com.iambadatplaying.logger.SimpleLogger;
import com.iambadatplaying.modules.BasicModuleLoader;
import com.iambadatplaying.rcconnection.RCConnector;
import com.iambadatplaying.server.LocalServer;

public class Starter implements Managable, Loggable {

    public static final String APPLICATION_NAME = "RCLS";

    private final LocalServer       localServer;
    private final RCConnector       rcConnector;
    private final DataManger        dataManger;
    private final BasicModuleLoader basicModuleLoader;

    public static final boolean isDev = true;

    private boolean running = false;

    public static void main(String[] args) {
        Starter starter = new Starter();
        starter.start();
        //Some startup Methods may need access as soon as we get connected to the RC
        starter.getLocalServer().awaitTermination();
    }

    public Starter() {
        logInit();
        this.basicModuleLoader = new BasicModuleLoader(this);
        this.localServer = new LocalServer(this);
        this.rcConnector = new RCConnector(this);
        this.dataManger = new DataManger(this);
    }

    public void exit(EXIT_CODE code) {
        log(LogLevel.CRITICAL, "Exiting with code: " + code.getCode() + " - " + code.getMessage());
        System.exit(code.getCode());
    }

    public LocalServer getLocalServer() {
        return localServer;
    }

    public RCConnector getRCConnector() {
        return rcConnector;
    }

    public BasicModuleLoader getBasicModuleLoader() {
        return basicModuleLoader;
    }

    public DataManger getDataManger() {
        return dataManger;
    }

    private void logInit() {
        if (Starter.isDev) {
            log(LogLevel.WARN, "----------------------------------------------------------------------------");
            log(LogLevel.WARN, "RUNNING UNSAFE DEVELOPMENT BUILD, SOME SECURITY FEATURES MAY BE DISABLED");
            log(LogLevel.WARN, "----------------------------------------------------------------------------");
        }
    }

    @Override
    public void start() {
        running = true;
        basicModuleLoader.start();
        localServer.start();
        rcConnector.start();
    }

    @Override
    public void stop() {
        running = false;
        rcConnector.stop();
        localServer.stop();
        basicModuleLoader.stop();
        dataManger.stop();
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }

    @Override
    public void log(Object o) {
        log(LogLevel.DEBUG, o);
    }

    @Override
    public void log(LogLevel level, Object o) {
        SimpleLogger.getInstance().log(level, this.getClass().getSimpleName() + ": " + o);
    }
}