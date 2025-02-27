package io.github.nillerr.jooq.kotlin.coroutines

import io.github.nillerr.jooq.kotlin.coroutines.configuration.isJDBC
import io.github.nillerr.jooq.kotlin.coroutines.configuration.jdbcCoroutineDispatcher
import io.github.nillerr.jooq.kotlin.coroutines.contracts.checkNotNull
import io.github.nillerr.jooq.kotlin.coroutines.internal.getPrimaryKeyConditions
import io.github.nillerr.jooq.kotlin.coroutines.internal.set
import io.github.nillerr.jooq.kotlin.coroutines.internal.uncheckedCast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.publish
import kotlinx.coroutines.runBlocking
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.TableField
import org.jooq.TableLike
import org.jooq.TableRecord
import org.jooq.UpdatableRecord
import org.jooq.exception.DataAccessException
import org.jooq.exception.DataChangedException
import org.jooq.impl.DSL
import java.util.logging.Logger
import kotlin.reflect.jvm.jvmName

/**
 * Converts the current [DSLContext] instance into a [SuspendingDSLContext],
 * enabling coroutine support for database operations.
 *
 * @return an instance of [SuspendingDSLContext] wrapping the current DSLContext.
 */
fun DSLContext.suspend(): SuspendingDSLContext {
    return SuspendingDSLContext(this)
}

/**
 * A context for executing suspending, transactional database operations using a DSL-based API.
 *
 * This class acts as a wrapper around a [DSLContext] to enable coroutine-based interaction with the database.
 * It simplifies working with both JDBC and R2DBC transactions, while providing additional utilities to handle
 * database operations such as updates, inserts, and deletes for records.
 */
class SuspendingDSLContext(internal val dsl: DSLContext) {
    private val logger = Logger.getLogger(this::class.jvmName)

    private val unwrapTransactionMessages = setOf(
        "Rollback caused",
        "Exception when blocking on publisher",
    )

    private fun unwrapTransactionException(exception: DataAccessException): Throwable? {
        var cause: Throwable = exception
        while (cause::class == DataAccessException::class && cause.message in unwrapTransactionMessages) {
            cause = cause.cause ?: return cause
        }
        return cause.takeUnless { it::class == DataAccessException::class }
    }

    /**
     * Runs the specified coroutine, suspending until it completes, returning the result.
     */
    suspend fun <R> transaction(
        isolationLevel: IsolationLevel? = null,
        isReadOnly: Boolean? = null,
        transactional: suspend CoroutineScope.(DSLContext) -> R,
    ): R {
        val configuration = dsl.configuration()
        if (configuration.isJDBC) {
            return jdbcTransaction(isolationLevel, isReadOnly, transactional)
        } else {
            return r2dbcTransaction(isolationLevel, isReadOnly, transactional)
        }
    }

    /**
     * Executes a transactional block of suspending code using the JDBC transaction mechanism.
     * This method ensures the transactional block is executed within a JDBC transaction,
     * using the provided DSLContext in the context of a transactional CoroutineScope.
     *
     * @param transactional The suspending function to execute within the context of a JDBC transaction.
     *                      This function is passed the transactional DSLContext to perform database operations.
     * @return The result of the transactional block execution.
     * @throws DataAccessException If an error occurs during transaction execution or in case of a rollback.
     */
    private suspend fun <R> jdbcTransaction(
        isolationLevel: IsolationLevel?,
        isReadOnly: Boolean?,
        transactional: suspend CoroutineScope.(DSLContext) -> R,
    ): R {
        val configuration = dsl.configuration()
        val dispatcher = configuration.jdbcCoroutineDispatcher

        try {
            return dispatcher.launch { jdbcDispatch ->
                dsl.transactionResult { trx ->
                    runBlocking(jdbcDispatch) {
                        val dsl = trx.dsl()
                        dsl.connection { connection ->
                            if (isolationLevel != null) {
                                connection.transactionIsolation = isolationLevel.toJDBCIsolationLevel()
                            }

                            if (isReadOnly != null) {
                                connection.isReadOnly = isReadOnly
                            }
                        }

                        transactional(dsl)
                    }
                }
            }
        } catch (e: DataAccessException) {
            throw unwrapTransactionException(e) ?: throw DataAccessException("Rollback caused", e)
        }
    }

    /**
     * A Publisher cannot emit a `null` value, so we wrap every emitted value in a [Box] that can have a `null` value.
     */
    private class Box<T>(val value: T)

    /**
     * Executes a transactional block of suspending code using the R2DBC transaction mechanism.
     * The function ensures the transactional block is executed in the context of an R2DBC transaction,
     * managing the subscription and emission of results through a reactive pipeline.
     *
     * @param transactional The suspending function to execute within the context of an R2DBC transaction.
     *                      This function is passed the transactional DSLContext to perform database operations.
     * @return The result of the transactional block execution.
     * @throws DataAccessException If an error occurs during transaction execution or in case of a rollback.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun <R> r2dbcTransaction(
        isolationLevel: IsolationLevel?,
        isReadOnly: Boolean?,
        transactional: suspend CoroutineScope.(DSLContext) -> R,
    ): R {
        if (isReadOnly != null) {
            logger.warning("Setting `READ ONLY` for a transaction is not supported by R2DBC.")
        }

        try {
            val publisher = dsl.transactionPublisher { trx ->
                publish {
                    val connection = trx.connectionFactory().create().awaitFirst()
                    if (isolationLevel != null) {
                        connection.setTransactionIsolationLevel(isolationLevel.toR2DBCIsolationLevel()).awaitFirst()
                    }

                    val result = transactional(trx.dsl())
                    send(Box(result))
                }
            }

            val box = publisher.awaitFirst()
            return box.value
        } catch (e: DataAccessException) {
            throw unwrapTransactionException(e) ?: throw DataAccessException("Rollback caused", e)
        }
    }

    /**
     * Suspends and executes a query to count grouped records from a specified table based on a condition.
     *
     * @param of The table from which the records are being queried.
     * @param where The condition to filter the records.
     * @param groupBy The field used to group the records.
     * @return A map where the keys are the grouped field values and the values are the counts of records per group.
     */
    suspend fun <R : Record, T : Any> count(
        of: TableLike<R>,
        where: Condition,
        groupBy: TableField<R, T?>,
    ): Map<T, Int> {
        return dsl.select(groupBy, DSL.count())
            .from(of)
            .where(where)
            .groupBy(groupBy)
            .suspend()
            .toMap({ (key) -> checkNotNull(key, groupBy) }, { (_, count) -> count })
    }

    /**
     * Stores the [record] back to the database using an `UPDATE` statement.
     *
     * Only values of changed fields (as indicated by [Record.changed]) will be updates.
     *
     * @return 1 if the record was updated in the database. 0 if storing was not necessary.
     *
     * @throws DataAccessException if something went wrong executing the query
     * @throws DataChangedException If optimistic locking is enabled and the record has already been changed/deleted in the
     * database
     */
    suspend fun <R : UpdatableRecord<R>> update(record: UpdatableRecord<R>): Int {
        if (!record.changed()) {
            return 0
        }

        val conditions = record.getPrimaryKeyConditions()

        val updates = dsl.update(record.table)
            .set(record)
            .where(conditions)
            .awaitFirst()

        for (field in record.fields()) {
            record.changed(field, false)
        }

        return updates
    }

    /**
     * Stores the [record] back to the database using either an `INSERT` or `UPDATE` statement.
     *
     * Only values of changed fields (as indicated by [Record.changed]) will be updates.
     *
     * @return 1 if the record was updated in the database. 0 if storing was not necessary.
     *
     * @throws DataAccessException if something went wrong executing the query
     * @throws DataChangedException If optimistic locking is enabled and the record has already been changed/deleted in the
     * database
     */
    suspend fun <R : UpdatableRecord<R>> store(record: UpdatableRecord<R>): Int {
        val primaryKey = requireNotNull(record.table.primaryKey)
        val keys = primaryKey.fieldsArray
        if (keys.any { record.changed(it) || !it.dataType.nullable() && record.get(it) == null }) {
            // If any primary key has changed, we want to perform an insertion rather than update
            return insert(record)
        }

        return update(record)
    }

    /**
     * Deletes the [record] from teh database, based on the value of the primary key or main unique key.
     *
     * @return 1 if the record was deleted from the database. 0 if deletion was not necessary.
     */
    suspend fun <R : UpdatableRecord<R>> delete(record: UpdatableRecord<R>): Int {
        val conditions = record.getPrimaryKeyConditions()

        return dsl.deleteFrom(record.table)
            .where(conditions)
            .awaitFirst()
    }

    /**
     * Deletes the [records] from teh database, based on the value of the primary key or main unique key.
     *
     * @return The number of records that was deleted from the database. 0 if deletion was not necessary.
     */
    suspend fun <R : UpdatableRecord<R>> deleteAll(records: List<UpdatableRecord<R>>): Int {
        val record = records.firstOrNull()
        if (record == null) {
            return 0
        }

        val conditions = DSL.or(records.map { DSL.and(it.getPrimaryKeyConditions()) })

        return dsl.deleteFrom(record.table)
            .where(conditions)
            .suspend()
            .first()
    }

    /**
     * Store the [record] in the database using an `INSERT` statement.
     *
     * @return 1 if the record was stored to the database. 0 if storing was not necessary.
     */
    suspend fun <R : TableRecord<R>> insert(record: TableRecord<R>): Int {
        if (!record.changed()) {
            return 0
        }

        val insertion = dsl.insertInto(record.table)
            .set(record)
            .returning()
            .suspend()
            .first()

        insertion.copyTo(record)

        return 1
    }

    /**
     * Store all of the [records] in the database using an `INSERT` statement.
     *
     * @return the number of records that was stored to the database. 0 if storing was not necessary.
     */
    suspend fun <R : TableRecord<R>> insertAll(records: List<TableRecord<R>>): Int {
        val record = records.firstOrNull()
        if (record == null) {
            return 0
        }

        if (records.none { it.changed() }) {
            return 0
        }

        val insertions = dsl.insertInto(record.table)
            .set(records = records)
            .returning()
            .suspend()
            .toList()

        insertions.forEachIndexed { index, insertion ->
            insertion.copyTo(records[index])
        }

        return insertions.size
    }

    @Suppress("UNCHECKED_CAST")
    private fun <R : TableRecord<R>> TableRecord<R>.copyTo(destination: TableRecord<R>) {
        for (field in fields()) {
            destination.set(field, uncheckedCast(get(field)))
            destination.changed(field, false)
        }
    }

    /**
     * Store the [record] in the database using an `INSERT` statement.
     *
     * @return 1 if the record was stored to the database. 0 if storing was not necessary.
     */
    suspend fun <R : TableRecord<R>> insertOnConflictDoNothing(record: TableRecord<R>): Int {
        if (!record.changed()) {
            return 0
        }

        val insertion = dsl.insertInto(record.table)
            .set(record)
            .onConflictDoNothing()
            .returning()
            .suspend()
            .firstOrNull()

        if (insertion == null) {
            return 0
        }

        insertion.copyTo(record)

        return 1
    }
}
