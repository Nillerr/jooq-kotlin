package io.github.nillerr.r2dbc;

@FunctionalInterface
public interface VoidCallable {
    void call() throws Exception;
}
