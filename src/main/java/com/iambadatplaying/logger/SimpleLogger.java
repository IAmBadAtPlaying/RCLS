package com.iambadatplaying.logger;

import com.iambadatplaying.Starter;

public class SimpleLogger {

    private SimpleLogger() {
        StringBuilder sb = new StringBuilder();
        sb.append("Logging levels: ");
        for (LogLevel level : LogLevel.values()) {
            sb.append(getColoredLevelPrefix(level)).append(" ");
        }
        logWithoutPrefix(sb.toString());
    }

    private static SimpleLogger _instance;

    public static SimpleLogger getInstance() {
        if (_instance == null) {
            _instance = new SimpleLogger();
        }
        return _instance;
    }

    private void logWithoutPrefix(Object o) {
        System.out.println(o);
    }

    public void log(LogLevel level, Object o) {
        if (!Starter.isDev) {
            switch (level) {
                case DEBUG:
                case INFO:
                    return;
                default:
                    break;

            }
        }
        System.out.println(getColoredLevelPrefix(level)+ ": " + o);
    }

    public void log(Object o) {
        log(LogLevel.INFO, o);
    }

    private static String getColoredLevelPrefix(LogLevel level) {
        switch (level) {
            case DEBUG:
                return "\u001B[34m["+level.name()+"]\u001B[0m";
            case INFO:
                return "\u001B[32m["+level.name()+"]\u001B[0m";
            case WARN:
                return "\u001B[33m["+level.name()+"]\u001B[0m";
            case ERROR:
                return "\u001B[31m["+level.name()+"]\u001B[0m";
            case CRITICAL:
                return "\u001B[41m["+level.name()+"]\u001B[0m";
            default:
                return "["+level.name()+"]";
        }
    }
}
