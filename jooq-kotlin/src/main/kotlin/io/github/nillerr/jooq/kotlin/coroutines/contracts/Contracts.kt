package io.github.nillerr.jooq.kotlin.coroutines.contracts

import org.jooq.Field

/**
 * Ensures that the given value is not null and returns it. If the value is null, an exception
 * is thrown with a detailed message, using the provided field information.
 *
 * @param value The value to be checked for nullability.
 * @param field The field for which the value is being checked, used in the exception message if null.
 * @return The non-null value provided as input.
 * @throws IllegalStateException if the value is null.
 */
fun <T : Any> checkFieldNotNull(value: T?, field: Field<T?>): T =
    checkNotNull(value) { "Unexpectedly found 'null' while getting the value of the field '$field'" }
