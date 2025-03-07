package io.github.nillerr.jooq.kotlin.coroutines

import io.github.nillerr.jooq.kotlin.coroutines.configuration.isJDBC
import io.github.nillerr.jooq.kotlin.coroutines.configuration.jdbcCoroutineDispatcher
import kotlinx.coroutines.reactive.collect
import org.jooq.Attachable
import org.jooq.Configuration
import org.jooq.Publisher
import org.jooq.Record
import org.jooq.Record1
import org.jooq.Record2
import org.jooq.ResultQuery
import org.jooq.RowCountQuery

private fun checkConfiguration(attachable: Attachable): Configuration {
    return checkNotNull(attachable.configuration()) { "Must be attached to a configuration." }
}

/**
 * Wraps the underlying ResultQuery into a [SuspendingResultQuery], enabling the query to be executed
 * in a coroutine-based suspending context.
 *
 * This allows the ResultQuery to be processed asynchronously, integrating seamlessly with coroutine-based
 * systems and enabling non-blocking database operations.
 *
 * @return A [SuspendingResultQuery] instance wrapping the current ResultQuery.
 */
fun <R : Record> ResultQuery<R>.suspend(): SuspendingResultQuery<R> {
    return SuspendingResultQuery(checkConfiguration(this), this)
}

/**
 * Wraps the underlying ResultQuery into a [SuspendingResultQuery], enabling the query to be executed
 * in a coroutine-based suspending context.
 *
 * This allows the ResultQuery to be processed asynchronously, integrating seamlessly with coroutine-based
 * systems and enabling non-blocking database operations.
 *
 * @return A [SuspendingResultQuery] instance wrapping the current ResultQuery.
 */
fun RowCountQuery.suspend(): SuspendingResultQuery<Int> {
    return SuspendingResultQuery(checkConfiguration(this), this)
}

/**
 * A suspending result query wrapper for executing database queries in a coroutine-based context.
 *
 * This class works with a [ResultQuery] implementation and provides a suspending mechanism to process
 * the results of the query asynchronously. The results can be collected and processed using a provided collector
 * function in a suspending manner.
 *
 * @param R The type of [Record] returned by the query execution.
 * @property query The underlying [ResultQuery] to be executed asynchronously.
 */
class SuspendingResultQuery<R>(private val configuration: Configuration, private val query: Publisher<R>) {
    /**
     * Collects the results of the query execution, applying the provided collector function on each record.
     *
     * @param collector A function to process each record in the result set.
     *                  It is invoked for every record returned by the query execution.
     */
    suspend fun collect(collector: (R) -> Unit) {
        if (configuration.isJDBC) {
            val dispatcher = configuration.jdbcCoroutineDispatcher
            return dispatcher.launch {
                query.collect {
                    collector(it)
                }
            }
        } else {
            return query.collect {
                collector(it)
            }
        }
    }
}

/**
 * Suspends and executes the query, transforming the result set into a Map.
 * Each record in the result set is processed to generate a key-value pair for the map.
 *
 * @param keySelector A lambda function to extract a key from each record.
 * @param valueTransform A lambda function to extract or transform a value from each record.
 * @return A Map containing the transformed keys and values based on the records of the query.
 */
suspend fun <R, K, V> SuspendingResultQuery<R>.toMap(keySelector: (R) -> K, valueTransform: (R) -> V): Map<K, V> {
    return buildMap {
        collect { value ->
            set(keySelector(value), valueTransform(value))
        }
    }
}

/**
 * Transforms the results of a query into a map, using a key selector function.
 * The keys of the map are determined by the `keySelector` function and the values
 * are the corresponding records from the query result.
 *
 * @param keySelector A function that maps a record to a key, used to group the records into the resulting map.
 * @return A map where the keys are derived from applying the `keySelector` function on the records of the query result,
 *         and the values are the corresponding records.
 */
suspend fun <R, K> SuspendingResultQuery<R>.toMap(keySelector: (R) -> K): Map<K, R> {
    return buildMap {
        collect { value ->
            set(keySelector(value), value)
        }
    }
}

/**
 * Suspends the execution of the query and transforms the result set into a Map.
 * Each record is processed to extract keys and values, forming a key-value mapping.
 *
 * @return A Map where the keys and values are derived from the records in the query result set.
 */
suspend fun <R : Record2<K, V>, K, V> SuspendingResultQuery<R>.toMap(): Map<K, V> {
    return toMap({ (key, _) -> key }, { (_, value) -> value })
}

/**
 * Suspends the query execution and collects its results. Transforms each record using the provided
 * transformation function, filters out `null` values, and adds the resulting values to the given destination collection.
 *
 * @param destination The mutable collection where the non-null transformed values will be added.
 * @param valueTransform A mapping function to transform each record of the result set into a value or `null`.
 * @return The provided destination collection containing the transformed non-null values.
 */
suspend fun <R, T : Any, C : MutableCollection<T>> SuspendingResultQuery<R>.mapNotNullTo(
    destination: C,
    valueTransform: (R) -> T?,
): C {
    collect { value ->
        val transformed = valueTransform(value)
        if (transformed != null) {
            destination.add(transformed)
        }
    }

    return destination
}

/**
 * Suspends the execution of a result query, mapping each record to a value using the provided transformation function,
 * and adds the result to the specified destination collection.
 *
 * @param destination The collection where transformed elements will be added.
 * @param valueTransform A function applied to each record to produce the desired value.
 * @return The updated destination collection containing the transformed values.
 */
suspend fun <R, T, C : MutableCollection<T>> SuspendingResultQuery<R>.mapTo(
    destination: C,
    valueTransform: (R) -> T,
): C {
    collect { value ->
        destination.add(valueTransform(value))
    }

    return destination
}

/**
 * Suspends until the result query collects all records into the provided mutable collection.
 *
 * @param destination The mutable collection where the records from the query will be added.
 * @return The same mutable collection passed as the destination, now populated with the records from the query.
 */
suspend fun <R, C : MutableCollection<R>> SuspendingResultQuery<R>.toCollection(destination: C): C {
    collect(destination::add)
    return destination
}

/**
 * Suspends the execution of the current coroutine, collecting the result of the query into a list.
 * The query results are processed asynchronously and returned as a list of records.
 *
 * @return A list of records resulting from executing the query, collected asynchronously.
 */
suspend fun <R> SuspendingResultQuery<R>.toList(): List<R> {
    return buildList { this@toList.toCollection(this) }
}

/**
 * Suspends the execution of a result query and transforms each record into a value using the specified
 * transformation function, returning a list of the transformed values.
 *
 * @param valueTransform A function applied to each record to produce the desired value.
 * @return A list containing the transformed values.
 */
suspend fun <R, T> SuspendingResultQuery<R>.mapToList(valueTransform: (R) -> T): List<T> {
    return buildList { this@mapToList.mapTo(this, valueTransform) }
}

/**
 * Executes the query asynchronously, transforms each retrieved record using the provided transformation function,
 * filters out `null` values, and returns a list containing the non-null transformed values.
 *
 * @param valueTransform A function to transform each record of the result set into a value or `null`.
 * @return A list of non-null values resulting from the transformation of the query's records.
 */
suspend fun <R, T : Any> SuspendingResultQuery<R>.mapNotNullToList(valueTransform: (R) -> T?): List<T> {
    return buildList { this@mapNotNullToList.mapNotNullTo(this, valueTransform) }
}

/**
 * Suspends the execution of a `ResultQuery` and collects the values of the query result into a list.
 *
 * @return A list containing the values extracted from the query result.
 */
suspend fun <T> SuspendingResultQuery<Record1<T>>.valueToList(): List<T> {
    return buildList { this@valueToList.mapTo(this) { (value) -> value } }
}

/**
 * Suspends and collects all records from the query into a [Set].
 *
 * @return A [Set] containing all records from the query execution.
 */
suspend fun <R> SuspendingResultQuery<R>.toSet(): Set<R> {
    return buildSet { this@toSet.toCollection(this) }
}

/**
 * Suspends and transforms each record from the query using the provided transformation function,
 * collecting the results into a [Set].
 *
 * @param valueTransform A function that transforms each record into the desired type.
 * @return A [Set] containing the transformed values.
 */
suspend fun <R, T> SuspendingResultQuery<R>.mapToSet(valueTransform: (R) -> T): Set<T> {
    return buildSet { this@mapToSet.mapTo(this, valueTransform) }
}

/**
 * Suspends and transforms each record from the query using the provided transformation function,
 * filtering out null values and collecting the non-null results into a [Set].
 *
 * @param valueTransform A function that transforms each record into the desired type or null.
 * @return A [Set] containing the non-null transformed values.
 */
suspend fun <R, T : Any> SuspendingResultQuery<R>.mapNotNullToSet(valueTransform: (R) -> T?): Set<T> {
    return buildSet { this@mapNotNullToSet.mapNotNullTo(this, valueTransform) }
}

/**
 * Suspends and extracts the single value from each Record1 type record,
 * collecting all values into a [Set].
 *
 * @return A [Set] containing the values extracted from Record1 records.
 */
suspend fun <T> SuspendingResultQuery<Record1<T>>.valueToSet(): Set<T> {
    return buildSet { this@valueToSet.mapTo(this) { (value) -> value } }
}

/**
 * Suspends and retrieves the first record from the query result.
 *
 * @return The first record from the query result.
 * @throws NoSuchElementException if the query result is empty.
 */
suspend fun <R> SuspendingResultQuery<R>.first(): R {
    var result: R? = null
    collect { result = it }
    return result ?: throw NoSuchElementException("No elements were returned")
}

/**
 * Suspends and retrieves the first record from the query result, or null if the result is empty.
 *
 * @return The first record from the query result, or null if no records exist.
 */
suspend fun <R> SuspendingResultQuery<R>.firstOrNull(): R? {
    var result: R? = null
    collect { result = it }
    return result
}

/**
 * Suspends and retrieves exactly one record from the query result.
 *
 * @return The single record from the query result.
 * @throws IllegalArgumentException if more than one record is found.
 * @throws NoSuchElementException if no records are found.
 */
suspend fun <R> SuspendingResultQuery<R>.singleOrNull(): R? {
    var result: R? = null
    collect {
        if (result != null) {
            throw IllegalArgumentException("More than one record match the condition")
        }

        result = it
    }
    return result
}

/**
 * Suspends and retrieves exactly one record from the query result.
 *
 * @return The single record from the query result.
 * @throws IllegalArgumentException if more than one record is found.
 * @throws NoSuchElementException if no records are found.
 */
suspend fun <R> SuspendingResultQuery<R>.single(): R {
    return singleOrNull() ?: throw NoSuchElementException("No records match the condition")
}

/**
 * Executes the query and returns the number of affected rows.
 *
 * @return The number of rows affected by the executed query, or 0 if no rows were affected.
 */
suspend fun SuspendingResultQuery<Int>.execute(): Int {
    return firstOrNull() ?: 0
}
