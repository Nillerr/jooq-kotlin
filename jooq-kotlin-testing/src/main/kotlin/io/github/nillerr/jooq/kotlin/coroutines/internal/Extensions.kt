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
