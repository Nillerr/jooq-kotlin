package io.github.nillerr.jooq.r2dbc

import io.github.nillerr.r2dbc.JDBCExecutorService
import io.r2dbc.spi.Batch
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryMetadata
import io.r2dbc.spi.ConnectionMetadata
import io.r2dbc.spi.IsolationLevel
import io.r2dbc.spi.Statement
import io.r2dbc.spi.TransactionDefinition
import io.r2dbc.spi.ValidationDepth
import org.jooq.ConnectionProvider
import org.reactivestreams.Publisher
import java.sql.Connection
import java.time.Duration

class JDBCConnectionFactory : ConnectionFactory {
    override fun create(): Publisher<io.r2dbc.spi.Connection> {
        TODO("Not yet implemented")
    }

    override fun getMetadata(): ConnectionFactoryMetadata {
        TODO("Not yet implemented")
    }
}
