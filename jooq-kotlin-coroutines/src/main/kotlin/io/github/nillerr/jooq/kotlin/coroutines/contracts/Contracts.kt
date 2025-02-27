package io.github.nillerr.jooq.kotlin.coroutines.contracts

import org.jooq.Field

fun <T : Any> checkNotNull(value: T?, field: Field<T?>): T =
    checkNotNull(value) { "Unexpectedly found 'null' while getting the value of the field '$field'" }
