package com.iambadatplaying;

import com.iambadatplaying.data.DataManger;
import com.iambadatplaying.logger.LogLevel;
import com.iambadatplaying.logger.Loggable;
import com.iambadatplaying.logger.SimpleLogger;
import com.iambadatplaying.rcconnection.RCConnector;
import com.iambadatplaying.server.LocalServer;

public class Starter implements Managable, Loggable {
    private final LocalServer localServer;
    private final RCConnector rcConnector;
    private final DataManger dataManger;

    public static final boolean isDev = true;

    public static void main(String[] args) {
        Starter starter = new Starter();
        starter.start();
        starter.getRCConnector().connectToRC();
        //Some startup Methods may need access as soon as we get connected to the RC
        starter.getDataManger().start();
        starter.getLocalServer().awaitTermination();
        starter.stop();
    }

    public Starter() {
        this.localServer = new LocalServer(this);
        this.rcConnector = new RCConnector(this);
        this.dataManger = new DataManger(this);
    }

    public void exit(EXIT_CODE code) {
        log(LogLevel.CRITICAL,"Exiting with code: " + code.getCode() + " - " + code.getMessage());
        System.exit(code.getCode());
    }

    public LocalServer getLocalServer() {
        return localServer;
    }

    public RCConnector getRCConnector() {
        return rcConnector;
    }

    public DataManger getDataManger() {
        return dataManger;
    }

    @Override
    public void start() {
        localServer.start();
        rcConnector.start();
    }

    @Override
    public void stop() {
        localServer.stop();
        dataManger.stop();
        rcConnector.stop();
    }

    @Override
    public boolean isRunning() {
        return false;
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