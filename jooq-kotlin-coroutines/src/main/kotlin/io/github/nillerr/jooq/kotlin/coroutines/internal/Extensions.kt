package io.github.nillerr.jooq.kotlin.coroutines.internal

import org.jooq.Condition
import org.jooq.InsertSetMoreStep
import org.jooq.InsertSetStep
import org.jooq.Record
import org.jooq.Records
import org.jooq.TableField
import org.jooq.TableRecord

@Suppress("UNCHECKED_CAST")
internal fun <T> uncheckedCast(value: Any?): T = value as T

internal fun <R : Record> InsertSetStep<R>.set(records: Iterable<Record>): InsertSetMoreStep<R> {
    // Calling `set(Map)` with an empty map is effectively a no-op, but returns the `InsertSetMoreStep` we're
    // interested in eventually returning.
    val initial = set(emptyMap<Nothing, Nothing>())
    return records.fold(initial) { step, record ->
        // Both `set(Record)` and `newRecord()` are mutable operations (like most jOOQ operations), so we don't
        // actually need to return the results of them, but can instead just keep on using the result of `set(Record)`,
        // which is of type `InsertSetMoreStep<R>`.
        step.set(record).also { it.newRecord() }
    }
}

/**
 * Retrieves a list of conditions representing the primary key constraints of this record.
 *
 * The method extracts the primary key fields from the associated table
 * and generates a list of conditions by mapping each primary key field
 * to its corresponding value in this record.
 *
 * @return a list of conditions where each condition represents the equality
 *         between a primary key field and its value in the record.
 */
internal fun <R : TableRecord<R>> TableRecord<R>.getPrimaryKeyConditions(): List<Condition> {
    val primaryKey = requireNotNull(table.primaryKey)
    val conditions = primaryKey.fields.map { field -> uncheckedCast<TableField<R, Any?>>(field).eq(get(field)) }
    return conditions
}
