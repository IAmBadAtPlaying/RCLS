package com.iambadatplaying.rcconnection.process;

import com.iambadatplaying.Managable;
import com.iambadatplaying.logger.Loggable;

import java.util.OptionalInt;

public interface ProcessHandler extends Loggable, Managable {

    void startRiotClientServicesProcess(int port, String authToken);

    OptionalInt getGameProcessId(Game game);

    OptionalInt getRiotClientServicesProcessId();

    boolean killProcessWithId(int processId);

    boolean launchRiotClient(int port, String authToken);
}