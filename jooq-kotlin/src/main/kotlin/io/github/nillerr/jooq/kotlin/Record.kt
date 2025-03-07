package io.github.nillerr.jooq.kotlin

import org.jooq.Field
import org.jooq.Record
import kotlin.reflect.KClass
import kotlin.reflect.typeOf

/**
 * Retrieves the value of the specified field from the record and casts it to the provided type.
 *
 * @param T The reified type to which the value should be cast.
 * @param field The field whose value is to be retrieved.
 * @return The value of the specified field, cast to type T.
 * @throws IllegalArgumentException If the provided type cannot be represented as a KClass.
 */
inline fun <reified T> Record.getValueAs(field: Field<*>): T {
    val type = typeOf<T>()
    val classifier = type.classifier
    require(classifier is KClass<*>) { "The type must be representable as a KClass" }
    return get(field, classifier.javaObjectType) as T
}
