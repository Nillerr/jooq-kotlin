package io.github.nillerr.jooq.kotlin.coroutines

import java.sql.Connection
import io.r2dbc.spi.IsolationLevel as R2DBCIsolationLevel

/**
 * Represents the isolation levels used in database transactions.
 *
 * Isolation levels define the degree to which the operations in one transaction are isolated
 * from those in other transactions. They affect the visibility of data changes made by a transaction
 * to other transactions and the level of concurrency control in the database.
 *
 * The available isolation levels are:
 *
 * - `READ_COMMITTED`: Ensures that a transaction only reads data committed before the transaction starts
 *   or during the transaction execution. Prevents dirty reads.
 *
 * - `READ_UNCOMMITTED`: Allows a transaction to read data that is not yet committed by other transactions.
 *   May result in dirty reads.
 *
 * - `REPEATABLE_READ`: Ensures that data read during a transaction does not change if accessed again within
 *   the same transaction. Prevents non-repeatable reads but does not prevent phantom reads.
 *
 * - `SERIALIZABLE`: Provides the highest level of isolation. Ensures that transactions are executed in a way
 *   that is equivalent to them running serially, one after another. Prevents dirty reads, non-repeatable reads,
 *   and phantom reads.
 */
enum class IsolationLevel {
    /**
     * Represents the `READ_COMMITTED` transaction isolation level.
     *
     * In the `READ_COMMITTED` isolation level, a transaction only reads data that has been committed.
     * This level prevents dirty reads, ensuring that transactions do not access uncommitted changes made
     * by other transactions. However, non-repeatable reads and phantom reads can still occur in this isolation level.
     *
     * Commonly used in scenarios where dirty reads need to be avoided but achieving higher levels
     * of isolation is not necessary or would lead to decreased performance.
     */
    READ_COMMITTED,

    /**
     * Represents the lowest isolation level, `READ_UNCOMMITTED`, in database transactions.
     *
     * At this isolation level, a transaction can read data that has been modified by other transactions but not yet committed,
     * allowing for dirty reads. This provides the highest level of concurrency but the least data integrity.
     *
     * Typically used in scenarios where performance is more critical than accuracy and consistency, and where the risk
     * of reading uncommitted changes is acceptable.
     *
     * This isolation level can be converted to corresponding JDBC and R2DBC isolation levels through utility methods.
     */
    READ_UNCOMMITTED,

    /**
     * Represents the `REPEATABLE READ` isolation level in a database transaction.
     *
     * This level ensures that if a transaction reads the same row multiple times,
     * the data remains consistent, even if other transactions modify the same row.
     * However, phantom reads may still occur, where rows that match a search condition
     * can appear or disappear due to other transactions.
     *
     * Typically used to maintain a high level of consistency during database operations
     * while allowing better performance compared to the `SERIALIZABLE` isolation level.
     */
    REPEATABLE_READ,

    /**
     * Represents the isolation levels that can be used for database transactions.
     *
     * This particular value, `SERIALIZABLE`, indicates the highest level of transaction isolation,
     * ensuring that transactions are completely isolated from one another. With this level,
     * the results of one transaction cannot be affected by another transaction.
     *
     * The `SERIALIZABLE` isolation level ensures that:
     * - No phantom reads occur.
     * - Dirty reads are prevented.
     * - Non-repeatable reads are not allowed.
     *
     * It is the most restrictive isolation level and can result in more locking and decreased concurrency.
     */
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
