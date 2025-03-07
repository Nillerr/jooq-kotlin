package io.github.nillerr.jooq.kotlin

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

/**
 * Creates platform threads with a customizable name prefix.
 *
 * This class is responsible for creating threads with a unique name for each thread,
 * which includes the specified prefix and an incrementing thread number.
 *
 * @constructor Creates a PlatformThreadFactory with a specified name prefix.
 * @param prefix The prefix to be used for all thread names created by this factory.
 */
class PlatformThreadFactory(prefix: String) : ThreadFactory {
    private val prefix = "$prefix-${poolNumber.andIncrement}"

    private val threadNumber = AtomicInteger(1)

    override fun newThread(r: Runnable): Thread? {
        val number = threadNumber.getAndIncrement()
        return Thread(r, "$prefix-$number")
    }

    companion object {
        @JvmStatic
        private val poolNumber = AtomicInteger(1)
    }
}
