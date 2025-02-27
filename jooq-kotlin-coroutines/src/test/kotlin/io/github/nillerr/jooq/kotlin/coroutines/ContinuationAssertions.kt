package io.github.nillerr.jooq.kotlin.coroutines

/**
 * Asserts that the continuation of the coroutine occurs on the specified thread.
 *
 * @param expected The thread on which the coroutine is expected to resume.
 */
@Suppress("RedundantSuspendModifier")
suspend fun assertContinuationOnThread(expected: Thread) {
    val actual = Thread.currentThread()
    assert(expected == actual) { "Coroutine resumed on a different thread than the one that started it." }
}

/**
 * Ensures that the coroutine is resumed on a different thread than the provided unexpected thread.
 *
 * @param unexpected The thread on which the coroutine is not expected to continue.
 * @param lazyMessage A lambda providing the message to include in the assertion error if the check fails.
 */
@Suppress("RedundantSuspendModifier")
suspend fun assertContinuationOnDifferentThread(unexpected: Thread, lazyMessage: () -> String) {
    val actual = Thread.currentThread()
    assert(actual != unexpected, lazyMessage)
}
