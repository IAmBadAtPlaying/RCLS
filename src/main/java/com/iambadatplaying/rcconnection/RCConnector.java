package com.iambadatplaying.rcconnection;

import com.iambadatplaying.EXIT_CODE;
import com.iambadatplaying.Managable;
import com.iambadatplaying.Starter;
import com.iambadatplaying.Util;
import com.iambadatplaying.logger.LogLevel;
import com.iambadatplaying.logger.Loggable;
import com.iambadatplaying.logger.SimpleLogger;
import com.iambadatplaying.rcconnection.process.Game;
import com.iambadatplaying.rcconnection.process.ProcessHandler;
import com.iambadatplaying.rcconnection.process.WindowsProcessHandler;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.InetAddress;
import java.security.cert.CertificateException;
import java.util.Base64;
import java.util.Optional;

public class RCConnector implements Managable, Loggable {

    public static Optional<ProcessHandler> getProcessHandlerForOS(Starter starter) {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return Optional.of(new WindowsProcessHandler(starter));
        }
        return Optional.empty();
    }

    private final Starter starter;

    private boolean running = false;
    private final RCConnectionManager rcConnectionManager;
    private final ProcessHandler processHandler;
    private final RCClient rcClient;
    private RCConnectionState connectionState = RCConnectionState.DISCONNECTED;
    private SSLContext sslContextGlobal = null;

    private Integer port = null;
    private String secret = null;
    private String authToken = null;

    public RCConnector(Starter starter) {
        this.starter = starter;
        this.processHandler = getProcessHandlerForOS(starter).orElse(null);
        this.rcConnectionManager = new RCConnectionManager(starter);
        this.rcClient = new RCClient(starter);
        if (!allowUnsecureConnections()) starter.exit(EXIT_CODE.CERTIFICATE_SETUP_FAILED);
    }

    private boolean allowUnsecureConnections() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                            if (chain != null && chain.length > 0) {
                                String clientHost = chain[0].getSubjectX500Principal().getName();
                                if (isLoopbackAddress(clientHost) || "CN=rclient".equals(clientHost)) {
                                    return;
                                }
                            }
                            throw new CertificateException("Untrusted client certificate");
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                            if (chain != null && chain.length > 0) {
                                String serverHost = chain[0].getSubjectX500Principal().getName();
                                if (isLoopbackAddress(serverHost) || "CN=rclient".equals(serverHost)) {
                                    return;
                                }
                            }
                            throw new CertificateException("Untrusted server certificate");

                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            sslContextGlobal = SSLContext.getInstance("TLS");
            sslContextGlobal.init(null, trustAllCerts, new java.security.SecureRandom());

            HttpsURLConnection.setDefaultSSLSocketFactory(sslContextGlobal.getSocketFactory());
            return true;
        } catch (Exception e) {
            System.out.println(e);
            log(LogLevel.ERROR, "Unable to allow all Connections!");
        }
        return false;
    }

    private boolean isLoopbackAddress(String host) {
        try {
            InetAddress address = InetAddress.getByName(host);
            return address.isLoopbackAddress();
        } catch (Exception e) {
            return false;
        }
    }

    public SSLContext getSslContextGlobal() {
        return sslContextGlobal;
    }

    public ProcessHandler getProcessHandler() {
        return processHandler;
    }

    public void connectToRC() {
        if (connectionState != RCConnectionState.DISCONNECTED) {
            log(LogLevel.WARN, "Wont connect to RC, already connected");
            return;
        }
        transition(RCConnectionState.CONNECTING);
        shutdownRCComponents();
        String secret = Util.createSecret();
        int port = Util.findFreePort();
        processHandler.startRiotClientServicesProcess(port, secret);
        transition(RCConnectionState.AWAIT_CONNECTION);
        String tempAuthToken = "Basic " + Base64.getEncoder().encodeToString(("riot:" + secret).trim().getBytes());
        if (checkConnection(port, tempAuthToken)) {
            this.port = port;
            this.secret = secret;
            this.authToken = tempAuthToken;
            log("Connected to RC");
            log(LogLevel.INFO, "Port: " + port);
            log(LogLevel.INFO, "Secret: " + secret);
            log(LogLevel.INFO, "AuthToken: " + this.authToken);
            transition(RCConnectionState.CONNECTED);
        } else {
            transition(RCConnectionState.DISCONNECTED);
        }
    }

    private boolean checkConnection(int port, String authToken) {
        return rcConnectionManager.checkConnection(port, authToken);
    }

    private void transition(RCConnectionState newState) {
        log("Transitioning from " + connectionState + " to " + newState);
        connectionState = newState;
        starter.getLocalServer().sendToAllWebsockets(connectionState.toString());
        switch (newState) {
            case CONNECTED:
                rcClient.start();
                if (!rcClient.registerDebugSession()) {
                    log(LogLevel.WARN, "Failed to register debug session, this might cause the connection to disconnect upon closing an instance of " + Game.RIOT_CLIENT.getDisplayName());
                }
                break;
            case DISCONNECTED:
                this.port = null;
                this.secret = null;
                this.authToken = null;
                rcClient.stop();
                break;
        }
    }

    public void RCWebsocketClosed() {
        transition(RCConnectionState.DISCONNECTED);
    }

    private void shutdownRCComponents() {
        for (Game game : Game.values()) {
            processHandler.getGameProcessId(game).ifPresent(
                    processHandler::killProcessWithId
            );
        }

        log("Killed all game processes");

        processHandler.getRiotClientServicesProcessId().ifPresent(
                processHandler::killProcessWithId
        );
    }

    public RCConnectionState getConnectionState() {
        return connectionState;
    }

    public RCConnectionManager getRCConnectionManager() {
        return rcConnectionManager;
    }

    public RCClient getRCClient() {
        return rcClient;
    }

    public Integer getPort() {
        return port;
    }

    public String getSecret() {
        return secret;
    }

    public String getAuthToken() {
        return authToken;
    }

    @Override
    public void start() {
        running = true;
        if (processHandler == null) {
            log(LogLevel.ERROR, "No process handler available for this OS");
            starter.exit(EXIT_CODE.NO_PROCESS_HANDLER_AVAILABLE);
        }
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
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
