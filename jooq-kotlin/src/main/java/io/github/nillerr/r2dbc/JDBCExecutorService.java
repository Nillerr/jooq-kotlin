package io.github.nillerr.r2dbc;

import org.jooq.Publisher;

import java.util.concurrent.Callable;

public interface JDBCExecutorService {
    Publisher<Void> launch(VoidCallable callable);

    <T> Publisher<T> launch(Callable<T> callable);
}
