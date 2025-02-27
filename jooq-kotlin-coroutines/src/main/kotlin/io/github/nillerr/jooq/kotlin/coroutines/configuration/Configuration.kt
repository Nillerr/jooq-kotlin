package io.github.nillerr.jooq.kotlin.coroutines.configuration

import org.jooq.Configuration
import org.jooq.DSLContext
import kotlin.reflect.jvm.jvmName

/**
 * Represents a key used to store and retrieve strongly-typed configuration data within a [Configuration].
 *
 * @param T The type of the data associated with the key. This ensures type safety when accessing configuration data.
 *
 * This interface is typically implemented as an object to serve as a unique identifier for a specific piece of configuration data.
 * Its primary use is in conjunction with extension functions such as `Configuration.get` or `Configuration.set`
 * to associate a strongly-typed value with a configuration instance.
 */
internal interface DataKey<T : Any>

/**
 * Retrieves a strongly-typed value associated with the specified configuration key.
 *
 * @param T The type of the value to be retrieved. Must be specified as a reified type parameter.
 * @param key The key used to look up the configuration value. Must implement [DataKey] specific to type [T].
 * @return The value associated with the specified key, or null if no value is found.
 */
internal inline fun <reified T : Any> Configuration.get(key: DataKey<T>): T? {
    return data(key) as T?
}

/**
 * Sets a value for the specified configuration data key and returns the previous value associated with the key, if any.
 *
 * @param key The configuration data key for which the value is to be set.
 * @param value The new value to be associated with the specified key.
 * @return The previous value associated with the specified key, or null if no previous value existed.
 */
internal inline fun <reified T : Any> Configuration.set(key: DataKey<T>, value: T): T? {
    return data(key, value) as T?
}

/**
 * Retrieves a strongly-typed value associated with the specified configuration key.
 *
 * @param T The type of the value to be retrieved. Must be specified as a reified type parameter.
 * @param key The key used to look up the configuration value. Must implement [DataKey] specific to type [T].
 * @return The value associated with the specified key, or null if no value is found.
 */
internal inline fun <reified T : Any> DSLContext.get(key: DataKey<T>): T? {
    return data(key) as T?
}

/**
 * Sets a value for the specified configuration data key and returns the previous value associated with the key, if any.
 *
 * @param key The configuration data key for which the value is to be set.
 * @param value The new value to be associated with the specified key.
 * @return The previous value associated with the specified key, or null if no previous value existed.
 */
internal inline fun <reified T : Any> DSLContext.set(key: DataKey<T>, value: T): T? {
    return data(key, value) as T?
}

/**
 * Extension property indicating whether the current [Configuration] is configured to use a JDBC connection.
 *
 * This property checks if the `connectionFactory` used in the [Configuration] is of type
 * `NoConnectionFactory`, which is typically associated with JDBC configurations.
 *
 * @receiver The [Configuration] to check.
 * @return True if the configuration uses a JDBC connection, false otherwise.
 */
internal val Configuration.isJDBC: Boolean
    get() {
        val connectionFactory = connectionFactory()
        return connectionFactory::class.jvmName == "org.jooq.impl.NoConnectionFactory"
    }
