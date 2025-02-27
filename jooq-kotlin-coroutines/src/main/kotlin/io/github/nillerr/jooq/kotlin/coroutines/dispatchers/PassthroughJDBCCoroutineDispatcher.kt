package io.github.nillerr.jooq.kotlin.coroutines.dispatchers

import java.util.logging.Logger
import kotlin.reflect.jvm.jvmName

object PassthroughJDBCCoroutineDispatcher : JDBCCoroutineDispatcher {
    private val logger = Logger.getLogger(PassthroughJDBCCoroutineDispatcher::class.jvmName)

    private val dispatch = JDBCDispatch()

    override suspend fun <T> launch(block: suspend (JDBCDispatch) -> T): T {
        logger.warning(
            "Using .suspend() is not intended to be used with JDBC without specifying a `jdbcCoroutineDispatcher`. " +
                    "Please refer to the documentation at https://github.com/Nillerr/jooq-kotlin/jooq-kotlin-coroutines " +
                    "for more information."
        )

        return block(dispatch)
    }

    override fun close() {
        // Nothing
    }
}
