package io.github.nillerr.r2dbc;

import io.r2dbc.spi.*;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.reactivestreams.Publisher;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

enum ExtendedR2dbcType implements Type {
    BIT(String.class),
    LONGVARCHAR(String.class),
    LONGVARBINARY(String.class),
    ;

    private final Class<?> javaType;

    ExtendedR2dbcType(Class<?> javaType) {
        this.javaType = javaType;
    }

    @NotNull
    @Override
    public Class<?> getJavaType() {
        return javaType;
    }

    @NotNull
    @Override
    public String getName() {
        return name();
    }
}

class Tools {
    public static Type toR2DBCType(int type) {
        switch (type) {
            case Types.BIT: return R2dbcType.BOOLEAN;
            case Types.TINYINT: return R2dbcType.TINYINT;
            case Types.SMALLINT: return R2dbcType.SMALLINT;
            case Types.INTEGER: return R2dbcType.INTEGER;
            case Types.BIGINT: return R2dbcType.BIGINT;

            case Types.FLOAT: return R2dbcType.FLOAT;
            case Types.REAL: return R2dbcType.REAL;
            case Types.DOUBLE: return R2dbcType.DOUBLE;
            case Types.NUMERIC: return R2dbcType.NUMERIC;
            case Types.DECIMAL: return R2dbcType.DECIMAL;

            case Types.CHAR: return R2dbcType.CHAR;
            case Types.VARCHAR: return R2dbcType.VARCHAR;
            case Types.LONGVARCHAR: return ExtendedR2dbcType.LONGVARCHAR;

            case Types.DATE: return R2dbcType.DATE;
            case Types.TIME: return R2dbcType.TIME;
            case Types.TIMESTAMP: return R2dbcType.TIMESTAMP;

            case Types.BINARY: return R2dbcType.BINARY;
            case Types.VARBINARY: return R2dbcType.VARBINARY;
            case Types.LONGVARBINARY: return ExtendedR2dbcType.LONGVARBINARY;

            case Types.NULL: return R2dbcType.NULL;

            case Types.ARRAY: return R2dbcType.COLLECTION;
            case Types.BINARY: return R2dbcType.BINARY;
            case Types.BLOB: return R2dbcType.BLOB;
            case Types.BOOLEAN: return R2dbcType.BOOLEAN;
            case Types.CHAR: return R2dbcType.CHAR;
            case Types.CLOB: return R2dbcType.CLOB;
            case Types.DATALINK: return R2dbcType.DATALINK;
            case Types.DATE: return R2dbcType.DATE;
        }
    }

    @MagicConstant(intValues = {
            java.sql.Connection.TRANSACTION_READ_COMMITTED,
            java.sql.Connection.TRANSACTION_READ_UNCOMMITTED,
            java.sql.Connection.TRANSACTION_REPEATABLE_READ,
            java.sql.Connection.TRANSACTION_SERIALIZABLE,
            java.sql.Connection.TRANSACTION_NONE,
    })
    public static int toJDBCTransactionLevel(@Nullable IsolationLevel isolationLevel) {
        if (isolationLevel == IsolationLevel.READ_COMMITTED) {
            return java.sql.Connection.TRANSACTION_READ_COMMITTED;
        } else if (isolationLevel == IsolationLevel.READ_UNCOMMITTED) {
            return java.sql.Connection.TRANSACTION_READ_UNCOMMITTED;
        } else if (isolationLevel == IsolationLevel.REPEATABLE_READ) {
            return java.sql.Connection.TRANSACTION_REPEATABLE_READ;
        } else if (isolationLevel == IsolationLevel.SERIALIZABLE) {
            return java.sql.Connection.TRANSACTION_SERIALIZABLE;
        } else {
            return java.sql.Connection.TRANSACTION_NONE;
        }
    }
}

class JDBCColumnMetadata implements ColumnMetadata {
    private final ResultSetMetaData metaData;
    private final int index;

    @NotNull
    @Override
    public Type getType() {
        return metaData.getColumnType(index);
    }

    @NotNull
    @Override
    public String getName() {
        return "";
    }
}

class JDBCRowMetadata implements RowMetadata {
    @NotNull
    @Override
    public ColumnMetadata getColumnMetadata(int index) {
        return null;
    }

    @NotNull
    @Override
    public ColumnMetadata getColumnMetadata(@NotNull String name) {
        return null;
    }

    @NotNull
    @Override
    public List<? extends ColumnMetadata> getColumnMetadatas() {
        return List.of();
    }

    @NotNull
    @Override
    public Collection<String> getColumnNames() {
        return List.of();
    }
}

class JDBCRow implements Row {
    @NotNull
    @Override
    public RowMetadata getMetadata() {
        return null;
    }

    @Override
    public <T> T get(int index, @NotNull Class<T> type) {
        return null;
    }

    @Override
    public <T> T get(@NotNull String name, @NotNull Class<T> type) {
        return null;
    }
}

class JDBCResultSetResult implements Result {
    private final int updateCount;
    private final ResultSet rs;
    private final JDBCExecutorService executorService;

    @NotNull
    @Override
    public Publisher<Integer> getRowsUpdated() {
        return executorService.launch(() -> updateCount);
    }

    @NotNull
    @Override
    public <T> Publisher<T> map(@NotNull BiFunction<Row, RowMetadata, ? extends T> mappingFunction) {
        return subscriber -> {
            while (true) {
                var completed = false
                try {
                    while (rs.next()) {
                        rs.getMetaData()
                    }
                    completed = true
                } catch (SQLException e) {
                    subscriber.onError(e);
                }

                if (completed) {
                    subscriber.onComplete();
                }
            }
        };
    }

    @NotNull
    @Override
    public Result filter(@NotNull Predicate<Segment> filter) {
        return null;
    }

    @NotNull
    @Override
    public <T> Publisher<T> flatMap(@NotNull Function<Segment, ? extends Publisher<? extends T>> mappingFunction) {
        return null;
    }
}

class JDBCBatch implements Batch {
    private final ArrayList<String> sqls = new ArrayList<>();

    private final java.sql.Connection connection;
    private final JDBCExecutorService executorService;

    @NotNull
    @Override
    public Batch add(@NotNull String sql) {
        sqls.add(sql);
        return this;
    }

    @NotNull
    @Override
    public Publisher<? extends Result> execute() {
        return executorService.launch(() -> {
            var statement = connection.createStatement();
            for (var sql : sqls) {
                statement.addBatch(sql);
            }
            statement.executeQuery()
            return statement.executeBatch();
        });
    }
}

public class JDBCConnection implements Connection {
    @NotNull
    private final java.sql.Connection connection;

    @NotNull
    private final JDBCExecutorService executorService;

    public JDBCConnection(@NotNull java.sql.Connection connection, @NotNull JDBCExecutorService executorService) {
        this.connection = connection;
        this.executorService = executorService;
    }

    @NotNull
    @Override
    public Publisher<Void> beginTransaction() {
        return executorService.launch(() -> connection.setAutoCommit(false));
    }

    @NotNull
    @Override
    public Publisher<Void> beginTransaction(@NotNull TransactionDefinition definition) {
        return executorService.launch(() -> {
            connection.setAutoCommit(false);

            var r2dbcIsolationLevel = definition.getAttribute(TransactionDefinition.ISOLATION_LEVEL);
            var jdbcTransactionLevel = Tools.toJDBCTransactionLevel(r2dbcIsolationLevel);
            if (jdbcTransactionLevel != java.sql.Connection.TRANSACTION_NONE) {
                connection.setTransactionIsolation(jdbcTransactionLevel);
            }

            var readOnly = definition.getAttribute(TransactionDefinition.READ_ONLY);
            if (readOnly != null) {
                connection.setReadOnly(readOnly);
            }
        });
    }

    @NotNull
    @Override
    public Publisher<Void> close() {
        return executorService.launch(connection::close);
    }

    @NotNull
    @Override
    public Publisher<Void> commitTransaction() {
        return executorService.launch(connection::commit);
    }

    @NotNull
    @Override
    public Batch createBatch() {
        return
    }

    @NotNull
    @Override
    public Publisher<Void> createSavepoint(String name) {
        return null;
    }

    @NotNull
    @Override
    public Statement createStatement(String sql) {
        return null;
    }

    @Override
    public boolean isAutoCommit() {
        return false;
    }

    @NotNull
    @Override
    public ConnectionMetadata getMetadata() {
        return null;
    }

    @NotNull
    @Override
    public IsolationLevel getTransactionIsolationLevel() {
        return null;
    }

    @NotNull
    @Override
    public Publisher<Void> releaseSavepoint(@NotNull String name) {
        return null;
    }

    @NotNull
    @Override
    public Publisher<Void> rollbackTransaction() {
        return null;
    }

    @NotNull
    @Override
    public Publisher<Void> rollbackTransactionToSavepoint(String name) {
        return null;
    }

    @NotNull
    @Override
    public Publisher<Void> setAutoCommit(boolean autoCommit) {
        return null;
    }

    @NotNull
    @Override
    public Publisher<Void> setLockWaitTimeout(Duration timeout) {
        return null;
    }

    @NotNull
    @Override
    public Publisher<Void> setStatementTimeout(Duration timeout) {
        return null;
    }

    @NotNull
    @Override
    public Publisher<Void> setTransactionIsolationLevel(IsolationLevel isolationLevel) {
        return null;
    }

    @NotNull
    @Override
    public Publisher<Boolean> validate(ValidationDepth depth) {
        return null;
    }
}
