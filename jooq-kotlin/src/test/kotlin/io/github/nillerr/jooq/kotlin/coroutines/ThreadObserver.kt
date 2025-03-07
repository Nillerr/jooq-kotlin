package io.github.nillerr.jooq.kotlin.coroutines

import java.util.concurrent.ConcurrentHashMap

class ThreadObserver {
    class Observation {
        var count: Long = 0
            private set

        fun increment(): Observation {
            count += 1
            return this
        }
    }

    val observations = ConcurrentHashMap<Thread, Observation>()

    fun observe() {
        observations.compute(Thread.currentThread()) { thread, observation ->
            (observation ?: Observation()).increment()
        }
    }

    fun clear() {
        observations.clear()
    }

    fun print() {
        var index = 1
        observations.toList().sortedBy { (thread, _) -> thread.name.substringAfterLast('-').toInt() }.forEach { (thread, observation) ->
            println("[ThreadObserver] (${index.toString().padStart(2, '0')}) ${thread.name} <${observation.count}>")
            index++
        }
    }
}
