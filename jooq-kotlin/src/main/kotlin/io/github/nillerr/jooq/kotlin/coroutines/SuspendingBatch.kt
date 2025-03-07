package io.github.nillerr.jooq.kotlin.coroutines

import io.github.nillerr.jooq.kotlin.coroutines.configuration.isJDBC
import io.github.nillerr.jooq.kotlin.coroutines.configuration.jdbcCoroutineDispatcher
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.collect
import org.jooq.Attachable
import org.jooq.Batch
import org.jooq.Configuration
import org.jooq.Publisher
import org.jooq.Record
import org.jooq.Record1
import org.jooq.Record2
import org.jooq.ResultQuery
import org.jooq.RowCountQuery
import java.util.concurrent.atomic.AtomicInteger

private fun checkConfiguration(batch: Batch): Configuration {
    val abstractBatch = Batch::class.java.classLoader.loadClass("org.jooq.impl.AbstractBatch")

    val configurationField = abstractBatch.getDeclaredField("configuration")
    configurationField.isAccessible = true

    return configurationField.get(batch) as Configuration
}

/**
 * Wraps the underlying Batch into a [SuspendingBatch], enabling the query to be executed
 * in a coroutine-based suspending context.
 *
 * This allows the Batch to be processed asynchronously, integrating seamlessly with coroutine-based
 * systems and enabling non-blocking database operations.
 *
 * @return A [SuspendingBatch] instance wrapping the current Batch.
 */
fun Batch.suspend(): SuspendingBatch {
    return SuspendingBatch(checkConfiguration(this), this)
}

/**
 * A suspending result query wrapper for executing database queries in a coroutine-based context.
 *
 * This class works with a [Batch] implementation and provides a suspending mechanism to process
 * the results of the query asynchronously. The results can be collected and processed using a provided collector
 * function in a suspending manner.
 *
 * @property batch The underlying [Batch] to be executed asynchronously.
 */
class SuspendingBatch(private val configuration: Configuration, private val batch: Batch) {
    val size: Int
        get() = batch.size()

    /**
     * Collects the results of the query execution, applying the provided collector function on each record.
     *
     * @param collector A function to process each record in the result set.
     *                  It is invoked for every record returned by the query execution.
     */
    suspend fun collect(collector: suspend (Int) -> Unit) {
        if (configuration.isJDBC) {
            val dispatcher = configuration.jdbcCoroutineDispatcher
            return dispatcher.launch {
                batch.execute().forEach {
                    collector(it)
                }
            }
        } else {
            return batch.collect {
                collector(it)
            }
        }
    }

    fun asFlow(): Flow<Int> {
        return flow { collect { emit(it) } }
    }
}

/**
 * Executes the query and returns the number of affected rows.
 *
 * @return The number of rows affected by the executed query, or 0 if no rows were affected.
 */
suspend fun SuspendingBatch.execute(): IntArray {
    var results = IntArray(size)
    val index = AtomicInteger(0)
    collect { results[index.getAndIncrement()] = it }
    return results
}
