package io.github.nillerr.jooq.kotlin.coroutines

import io.github.nillerr.jooq.kotlin.coroutines.internal.getPrimaryKeyConditions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.jooq.Configuration
import org.jooq.DSLContext
import org.jooq.TableRecord
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class ExplicitRollbackException : Exception()

fun DSLContext.runTest(block: suspend CoroutineScope.(Configuration) -> Unit) {
    runBlocking {
        try {
            suspend().transaction { trx ->
                block(trx)
                throw ExplicitRollbackException()
            }
        } catch (e: ExplicitRollbackException) {
            // Ignored
        }
    }
}

suspend fun <R : TableRecord<R>> DSLContext.assertExists(record: R, message: String? = null) {
    val conditions = record.getPrimaryKeyConditions()

    val existing = selectFrom(record.table)
        .where(conditions)
        .suspend()
        .firstOrNull()

    assertEquals(record, existing, message)
}

suspend fun <R : TableRecord<R>> DSLContext.assertNotExists(record: R, message: String? = null) {
    val conditions = record.getPrimaryKeyConditions()

    val existing = selectFrom(record.table)
        .where(conditions)
        .suspend()
        .firstOrNull()

    assertNull(existing, message)
}
