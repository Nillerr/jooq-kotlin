package io.github.nillerr.jooq.kotlin.coroutines

import java.sql.Connection
import io.r2dbc.spi.IsolationLevel as R2DBCIsolationLevel

enum class IsolationLevel {
    READ_COMMITTED,
    READ_UNCOMMITTED,
    REPEATABLE_READ,
    SERIALIZABLE,
    ;

    fun toJDBCIsolationLevel(): Int {
        return when (this) {
            READ_COMMITTED -> Connection.TRANSACTION_READ_COMMITTED
            READ_UNCOMMITTED -> Connection.TRANSACTION_READ_UNCOMMITTED
            REPEATABLE_READ -> Connection.TRANSACTION_REPEATABLE_READ
            SERIALIZABLE -> Connection.TRANSACTION_SERIALIZABLE
        }
    }

    fun toR2DBCIsolationLevel(): R2DBCIsolationLevel {
        return when (this) {
            READ_COMMITTED -> R2DBCIsolationLevel.READ_COMMITTED
            READ_UNCOMMITTED -> R2DBCIsolationLevel.READ_UNCOMMITTED
            REPEATABLE_READ -> R2DBCIsolationLevel.REPEATABLE_READ
            SERIALIZABLE -> R2DBCIsolationLevel.SERIALIZABLE
        }
    }
}
