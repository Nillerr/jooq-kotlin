package io.github.nillerr.r2dbc;

import org.jooq.Publisher;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PooledJDBCExecutorService implements JDBCExecutorService {
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    @Override
    public Publisher<Void> launch(VoidCallable callable) {
        return subscriber -> executor.submit(() -> {
            try {
                callable.call();
            } catch (Exception e) {
                subscriber.onError(e);
            }
            subscriber.onComplete();
        });
    }

    @Override
    public <T> Publisher<T> launch(Callable<T> callable) {
        return subscriber -> {
            executor.submit(() -> {
                try {
                    subscriber.onNext(callable.call());
                } catch (Exception e) {
                    subscriber.onError(e);
                }
                subscriber.onComplete();
            });
        };
    }
}
