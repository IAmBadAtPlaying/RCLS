package com.iambadatplaying;

public enum EXIT_CODE {
    NO_PROCESS_HANDLER_AVAILABLE(400, "No Process Handler available for this OS"),
    INSUFFICIENT_PERMISSIONS(401, "Insufficient Permissions"),
    RIOT_CLIENT_SERVICES_EXECUTABLE_NOT_FOUND(404, "Riot Client Services Executable not found"),
    CERTIFICATE_SETUP_FAILED(495, "Certificate Setup Failed"),
    SERVER_BIND_FAILED(500, "Server Bind Failed"),
    HTTP_PATCH_SETUP_FAILED(505, "HTTP Patch Setup Failed"),
    FAILED_TO_LOAD_MODULES(507, "Failed to load modules"),
    MULTIPLE_CONNECTION_ATTEMPTS_FAILED(522, "Multiple Connection Attempts Failed");

    private       int    code;
    private final String message;

    EXIT_CODE(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
