package io.github.nillerr.jooq.kotlin.coroutines.dispatchers

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * A coroutine context element holding the dispatcher the root JDBC operation was started on.
 *
 * This is used to ensure JDBC operations within a transaction are executed on the same thread as the rest of the
 * content of the transaction, which guarantees thread-safety of a JDBC connection for the lifetime of a transaction.
 */
class JDBCDispatch : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<JDBCDispatch>
}
