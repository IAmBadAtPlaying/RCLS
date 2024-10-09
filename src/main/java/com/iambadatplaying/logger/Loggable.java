package com.iambadatplaying.logger;

public interface Loggable {
    void log(Object o);
    void log(LogLevel level, Object o);
}
