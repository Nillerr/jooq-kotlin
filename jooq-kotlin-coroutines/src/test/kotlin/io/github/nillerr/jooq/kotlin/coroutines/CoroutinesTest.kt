package io.github.nillerr.jooq.kotlin.coroutines

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.nillerr.jooq.kotlin.coroutines.configuration.isJDBC
import io.github.nillerr.jooq.kotlin.coroutines.configuration.jdbcCoroutineDispatcher
import io.github.nillerr.jooq.kotlin.coroutines.contracts.checkFieldNotNull
import io.github.nillerr.jooq.kotlin.coroutines.dispatchers.StickyJDBCCoroutineDispatcher
import io.github.nillerr.jooq.kotlin.coroutines.dispatchers.StickyJDBCCoroutineDispatcherConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import liquibase.Liquibase
import liquibase.command.CommandScope
import liquibase.command.core.UpdateCommandStep
import liquibase.command.core.helpers.DatabaseChangelogCommandStep
import liquibase.command.core.helpers.DbUrlConnectionArgumentsCommandStep
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.conf.RenderKeywordCase
import org.jooq.conf.RenderNameCase
import org.jooq.conf.RenderQuotedNames
import org.jooq.conf.Settings
import org.jooq.exception.DataAccessException
import org.jooq.generated.tables.records.UserRecord
import org.jooq.generated.tables.references.USER
import org.jooq.impl.DSL
import org.jooq.impl.DataSourceConnectionProvider
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.concurrent.Executors
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@Testcontainers
class CoroutinesTest {
    @Test
    fun `transaction-less concurrency`(): Unit = runBlocking {
        // Given
        val configuration = dsl.configuration()
        assert(configuration.isJDBC) { "This test must be run with a JDBC connection." }

        val lisa = Users.lisa

        // When
        nettyWorkerSimulationDispatcher().use { dispatcher ->
            withContext(dispatcher) {
                repeat(500) { index ->
                    launch {
                        val record = newUserRecord(index)
                        dsl.suspend().insert(record)

                        simulateDispatchedAPICall()

                        dsl.assertExists(record)

                        simulateDispatchedAPICall()

                        dsl.suspend().delete(record)
                    }
                }
            }
        }

        // Then
        dsl.assertNotExists(lisa)
    }

    private suspend fun simulateDispatchedAPICall() {
        withContext(Dispatchers.IO) {
            delay(100)
        }
    }

    /**
     * Verifies the concurrent behavior of the `awaitTransaction` method in a JDBC setup.
     * Executes multiple transactions concurrently using coroutines, ensuring correct thread management
     * and transactional guarantees. Validates that the `awaitTransaction` works seamlessly
     * across different threads and maintains proper coroutine context transitions.
     *
     * Specifically, this test verifies that the block passed to `awaitTransaction` is executed on a
     * single thread, which remains the same thread throughout the lifetime of the transaction. The default
     * implementation of `awaitTransaction` creates a thread pool the size of the Hikari connection-pool,
     * which is strictly used for performing suspending database operations. Any database operation (including
     * suspending operations) performed within the `awaitTransaction` block are guaranteed to be executed on
     * the same thread as any other operation performed in the transaction not explicitly executed on a
     * different dispatcher.
     *
     * Likewise, any suspending database operation performed outside a call to `awaitTransaction` uses the same
     * Hikari connection-pool-limited thread pool.
     *
     * Notably, this test will fail if the dreaded connection timeout issue presents itself again.
     */
    @Test
    fun `awaitTransaction - JDBC - concurrent`(): Unit = runBlocking {
        // Given
        val configuration = dsl.configuration()
        assert(configuration.isJDBC) { "This test must be run with a JDBC connection." }

        val lisa = Users.lisa

        // When
        withTimeout(5_000) {
            nettyWorkerSimulationDispatcher().use { dispatcher ->
                withContext(dispatcher) {
                    repeat(100) { index ->
                        launch {
                            val serviceThread = Thread.currentThread()

                            val trxThread = dsl.suspend().transaction { trx ->
                                assertContinuationOnDifferentThread(serviceThread) { "Transaction coroutine started on the service thread." }

                                val trxThread = Thread.currentThread()

                                val record = newUserRecord(index)
                                trx.suspend().insert(record)

                                assertContinuationOnThread(trxThread)
                                simulateTransactionalDispatchedAPICall(trxThread)
                                assertContinuationOnThread(trxThread)

                                trx.assertExists(record)

                                assertContinuationOnThread(trxThread)
                                simulateTransactionalDispatchedAPICall(trxThread)
                                assertContinuationOnThread(trxThread)

                                trx.suspend().delete(record)

                                assertContinuationOnThread(trxThread)

                                trxThread
                            }

                            assertContinuationOnDifferentThread(trxThread) { "Service coroutine resumed on the transaction thread." }
                        }
                    }
                }
            }
        }

        // Then
        dsl.assertNotExists(lisa)
    }

    private fun newUserRecord(index: Int): UserRecord {
        val record = UserRecord(
            id = null,
            username = "user$index",
            email = "user$index@mail.com",
            deactivated = false,
            createdAt = null,
        )
        record.changed(USER.ID, false)
        record.changed(USER.CREATED_AT, false)
        return record
    }

    private suspend fun simulateTransactionalDispatchedAPICall(trxThread: Thread) {
        withContext(Dispatchers.IO) {
            assertContinuationOnDifferentThread(trxThread) { "Delay coroutine started on the transaction thread." }
            delay(100)
            assertContinuationOnDifferentThread(trxThread) { "Delay coroutine resumed on the transaction thread." }
        }
    }

    private fun nettyWorkerSimulationDispatcher(): ExecutorCoroutineDispatcher {
        val executor = Executors.newFixedThreadPool(4)
        val dispatcher = executor.asCoroutineDispatcher()
        return dispatcher
    }

    @Test
    fun awaitTransaction(): Unit = runBlocking {
        // Given
        val lisa = Users.lisa

        // When
        dsl.suspend().transaction { trx ->
            trx.suspend().insert(lisa)
            trx.assertExists(lisa)
            trx.suspend().delete(lisa)
        }

        // Then
        dsl.assertNotExists(lisa)
    }

    @Test
    fun `awaitTransaction unwraps DataAccessException`(): Unit = runBlocking {
        // Given
        val lisa1 = Users.lisa
        val lisa2 = Users.lisa

        // When
        val result = runCatching {
            dsl.suspend().transaction { trx ->
                trx.suspend().insert(lisa1)
                trx.suspend().insert(lisa2)
            }
        }

        // Then
        val exception = result.exceptionOrNull()
        assertTrue(exception is DataAccessException)
        assertEquals(
            """
                SQL [INSERT INTO "public"."user" ("id", "username", "email", "deactivated", "created_at") VALUES (?, ?, ?, ?, CAST(? AS timestamp(6))) RETURNING "public"."user"."id", "public"."user"."username", "public"."user"."email", "public"."user"."deactivated", "public"."user"."created_at"]; ERROR: duplicate key value violates unique constraint "user_pkey"
                  Detail: Key (id)=(-1) already exists.
            """.trimIndent(),
            exception.message,
        )
    }

    @Test
    fun `awaitTransaction unwraps other exception`(): Unit = runBlocking {
        // Given
        val expected = Exception("Expected Exception")

        // When
        val result = runCatching {
            dsl.suspend().transaction { trx ->
                throw expected
            }
        }

        // Then
        val actual = result.exceptionOrNull()
        assertSame(expected, actual)
    }

    @Test
    fun `awaitInsert without changes`() = dsl.runTest { trx ->
        // Given
        val user = Users.john
        user.changed(false)

        // When
        val result = trx.suspend().insert(user)

        // Then
        assertEquals(0, result)
        trx.assertNotExists(user)
    }

    @Test
    fun `awaitInsert with changes`() = dsl.runTest { trx ->
        // Given
        val user = Users.john

        // When
        val result = trx.suspend().insert(user)

        // Then
        assertEquals(1, result)
        trx.assertExists(user)
    }

    @Test
    fun `awaitInsertAll without values`() = dsl.runTest { trx ->
        // Given
        val users = emptyList<UserRecord>()

        // When
        val result = trx.suspend().insertAll(users)

        // Then
        assertEquals(0, result)
    }

    @Test
    fun `awaitInsertAll without changes`() = dsl.runTest { trx ->
        // Given
        val john = Users.john
        john.changed(false)

        val jane = Users.jane
        jane.changed(false)

        val users = listOf(john, jane)

        // When
        val result = trx.suspend().insertAll(users)

        // Then
        assertEquals(0, result)
        trx.assertNotExists(john)
        trx.assertNotExists(jane)
    }

    @Test
    fun `awaitInsertAll with changes`() = dsl.runTest { trx ->
        // Given
        val john = Users.john
        val jane = Users.jane

        val users = listOf(john, jane)

        // When
        val result = trx.suspend().insertAll(users)

        // Then
        assertEquals(2, result)
        trx.assertExists(john)
        trx.assertExists(jane)
    }

    @Test
    fun `awaitInsertOnConflictDoNothing without changes`() = dsl.runTest { trx ->
        // Given
        val user = Users.john
        user.changed(false)

        // When
        val result = trx.suspend().insertOnConflictDoNothing(user)

        // Then
        assertEquals(0, result)
        trx.assertNotExists(user)
    }

    @Test
    fun `awaitInsertOnConflictDoNothing when user does not exist`() = dsl.runTest { trx ->
        // Given
        val user = Users.john

        // When
        val result = trx.suspend().insertOnConflictDoNothing(user)

        // Then
        assertEquals(1, result)
        trx.assertExists(user)
    }

    @Test
    fun `awaitInsertOnConflictDoNothing when user exists`() = dsl.runTest { trx ->
        // Given
        val user = Users.john
        trx.suspend().insert(Users.john)

        // When
        val result = trx.suspend().insertOnConflictDoNothing(user)

        // Then
        assertEquals(0, result)
        trx.assertExists(user)
    }

    @Test
    fun `awaitInsert throws IntegrityConstraintViolationException when user exists`() = dsl.runTest { trx ->
        // Given
        trx.suspend().insert(Users.john)

        // When
        val result = runCatching { trx.suspend().insert(Users.john) }

        // Then
        val exception = result.exceptionOrNull()
        assertIs<DataAccessException>(exception)
    }

    @Test
    fun `awaitTransaction with awaitInsert throws IntegrityConstraintViolationException when user exists`() {
        // When
        val result = runCatching {
            dsl.runTest { trx ->
                trx.suspend().insert(Users.john)
                trx.suspend().insert(Users.john)
            }
        }

        // Then
        assertIs<DataAccessException>(result.exceptionOrNull())
    }

    @Test
    fun `awaitUpdate without changes`() = dsl.runTest { trx ->
        // Given
        val user = Users.john
        trx.suspend().insert(user)

        // When
        val result = trx.suspend().update(user)

        // Then
        assertEquals(0, result)
        trx.assertExists(user)
    }

    @Test
    fun `awaitUpdate with changes`() = dsl.runTest { trx ->
        // Given
        val user = Users.john
        trx.suspend().insert(user)

        user.username = "therealjohndoe"

        // When
        val result = trx.suspend().update(user)

        // Then
        assertEquals(1, result)
        trx.assertExists(user)
    }

    @Test
    fun `awaitDelete when user exists`() = dsl.runTest { trx ->
        // Given
        val user = Users.john
        trx.suspend().insert(user)

        // When
        val result = trx.suspend().delete(user)

        // Then
        assertEquals(1, result)
        trx.assertNotExists(user)
    }

    @Test
    fun `awaitDelete when user does not exist`() = dsl.runTest { trx ->
        // Given
        val user = Users.john

        // When
        val result = trx.suspend().delete(user)

        // Then
        assertEquals(0, result)
        trx.assertNotExists(user)
    }

    @Test
    fun `awaitDeleteAll when no users are provided`() = dsl.runTest { trx ->
        // Given
        val users = emptyList<UserRecord>()

        // When
        val result = trx.suspend().deleteAll(users)

        // Then
        assertEquals(0, result)
    }

    @Test
    fun `awaitDeleteAll when users exist`() = dsl.runTest { trx ->
        // Given
        val john = Users.john
        trx.suspend().insert(john)

        val jane = Users.jane
        trx.suspend().insert(jane)

        val users = listOf(john, jane)

        // When
        val result = trx.suspend().deleteAll(users)

        // Then
        assertEquals(2, result)
        trx.assertNotExists(john)
        trx.assertNotExists(jane)
    }

    @Test
    fun `awaitStore when user does not exist`() = dsl.runTest { trx ->
        // Given
        val user = Users.john

        // When
        val result = trx.suspend().store(user)

        // Then
        assertEquals(1, result)
        trx.assertExists(user)
    }

    @Test
    fun `awaitStore when user exists`() = dsl.runTest { trx ->
        // Given
        val user = Users.john
        trx.suspend().insert(user)

        user.username = "therealjohndoe"

        // When
        val result = trx.suspend().store(user)

        // Then
        assertEquals(1, result)
        trx.assertExists(user)
    }

    @Test
    fun `awaitToMap with key selector and value selector`() = dsl.runTest { trx ->
        // Given
        val john = Users.john
        trx.suspend().insert(john)

        val jane = Users.jane
        trx.suspend().insert(jane)

        val expected = buildMap {
            put(1, john.username)
            put(2, jane.username)
        }

        // When
        val result = trx.selectFrom(USER)
            .suspend().toMap({ checkFieldNotNull(it.id, USER.ID) }, { it.username })

        // Then
        assertEquals(expected, result)
    }

    @Test
    fun `awaitToMap with key selector`() = dsl.runTest { trx ->
        // Given
        val john = Users.john
        trx.suspend().insert(john)

        val jane = Users.jane
        trx.suspend().insert(jane)

        val expected = buildMap {
            put(1, john)
            put(2, jane)
        }

        // When
        val result = trx.selectFrom(USER)
            .suspend().toMap { checkFieldNotNull(it.id, USER.ID) }

        // Then
        assertEquals(expected, result)
    }

    @Test
    fun awaitToMap() = dsl.runTest { trx ->
        // Given
        val john = Users.john
        trx.suspend().insert(john)

        val jane = Users.jane
        trx.suspend().insert(jane)

        val expected = buildMap {
            put(john.username, 1)
            put(jane.username, 1)
        }

        // When
        val result = trx.select(USER.USERNAME, DSL.count())
            .from(USER)
            .groupBy(USER.USERNAME)
            .suspend().toMap()

        // Then
        assertEquals(expected, result)
    }

    @Test
    fun awaitToList() = dsl.runTest { trx ->
        // Given
        val john = Users.john
        trx.suspend().insert(john)

        val jane = Users.jane
        trx.suspend().insert(jane)

        val expected = listOf(john, jane)

        // When
        val result = trx.selectFrom(USER)
            .suspend().toList()

        // Then
        assertEquals(expected, result)
    }

    @Test
    fun awaitMapToList() = dsl.runTest { trx ->
        // Given
        val john = Users.john
        trx.suspend().insert(john)

        val jane = Users.jane
        trx.suspend().insert(jane)

        val expected = listOf(john.username, jane.username)

        // When
        val result = trx.selectFrom(USER)
            .orderBy(USER.ID.asc())
            .suspend().mapToList { it.username }

        // Then
        assertEquals(expected, result)
    }

    @Test
    fun awaitMapNotNullToList() = dsl.runTest { trx ->
        // Given
        val john = Users.john
        trx.suspend().insert(john)

        val jane = Users.jane
        trx.suspend().insert(jane)

        val expected = listOf(false)

        // When
        val result = trx.selectFrom(USER)
            .suspend().mapNotNullToList { it.deactivated }

        // Then
        assertEquals(expected, result)
    }

    @Test
    fun awaitValueToList() = dsl.runTest { trx ->
        // Given
        val john = Users.john
        trx.suspend().insert(john)

        val jane = Users.jane
        trx.suspend().insert(jane)

        val expected = listOf(john.id, jane.id)

        // When
        val result = trx.select(USER.ID)
            .from(USER)
            .suspend().valueToList()

        // Then
        assertEquals(expected, result)
    }

    @Test
    fun awaitToSet() = dsl.runTest { trx ->
        // Given
        val john = Users.john
        trx.suspend().insert(john)

        val jane = Users.jane
        trx.suspend().insert(jane)

        val expected = setOf(john, jane)

        // When
        val result = trx.selectFrom(USER)
            .suspend().toSet()

        // Then
        assertEquals(expected, result)
    }

    @Test
    fun awaitMapNotNullToSet() = dsl.runTest { trx ->
        // Given
        val john = Users.john
        trx.suspend().insert(john)

        val jane = Users.jane
        trx.suspend().insert(jane)

        val expected = setOf(false)

        // When
        val result = trx.selectFrom(USER)
            .suspend().mapNotNullToSet { it.deactivated }

        // Then
        assertEquals(expected, result)
    }

    @Test
    fun awaitValueToSet() = dsl.runTest { trx ->
        // Given
        val john = Users.john
        trx.suspend().insert(john)

        val jane = Users.jane
        trx.suspend().insert(jane)

        val expected = setOf(john.id, jane.id)

        // When
        val result = trx.select(USER.ID)
            .from(USER)
            .suspend().valueToSet()

        // Then
        assertEquals(expected, result)
    }

    @Test
    fun awaitMapToSet() = dsl.runTest { trx ->
        // Given
        val john = Users.john
        trx.suspend().insert(john)

        val jane = Users.jane
        trx.suspend().insert(jane)

        val expected = setOf(john.username, jane.username)

        // When
        val result = trx.selectFrom(USER)
            .suspend().mapToSet { it.username }

        // Then
        assertEquals(expected, result)
    }

    @Test
    fun awaitFirst() = dsl.runTest { trx ->
        // Given
        val john = Users.john
        trx.suspend().insert(john)

        // When
        val result = trx.selectFrom(USER)
            .where(USER.USERNAME.eq(john.username))
            .suspend().first()

        // Then
        assertEquals(john, result)
    }

    @Test
    fun `awaitFirstOrNull when user exists`() = dsl.runTest { trx ->
        // Given
        val john = Users.john
        trx.suspend().insert(john)

        // When
        val result = trx.selectFrom(USER)
            .where(USER.USERNAME.eq(john.username))
            .suspend().firstOrNull()

        // Then
        assertEquals(john, result)
    }

    @Test
    fun `awaitFirstOrNull when user not exist`() = dsl.runTest { trx ->
        // Given
        val john = Users.john

        // When
        val result = trx.selectFrom(USER)
            .where(USER.USERNAME.eq(john.username))
            .suspend().firstOrNull()

        // Then
        assertNull(result)
    }

    @Test
    fun `awaitSingle when user exists`() = dsl.runTest { trx ->
        // Given
        val john = Users.john
        trx.suspend().insert(john)

        // When
        val result = trx.selectFrom(USER)
            .where(USER.USERNAME.eq(john.username))
            .suspend().single()

        // Then
        assertEquals(john, result)
    }

    @Test
    fun `awaitSingle when multiple users match`() = dsl.runTest { trx ->
        // Given
        val john = Users.john
        trx.suspend().insert(john)

        val jane = Users.jane
        trx.suspend().insert(jane)

        // When
        val result = runCatching { trx.selectFrom(USER).suspend().single() }

        // Then
        val exception = assertIs<IllegalArgumentException>(result.exceptionOrNull())

        val expected = "More than one record match the condition"
        assertEquals(expected, exception.message)
    }

    @Test
    fun `awaitSingle when no users match`() = dsl.runTest { trx ->
        // When
        val result = runCatching { trx.selectFrom(USER).suspend().single() }

        // Then
        val exception = assertIs<NoSuchElementException>(result.exceptionOrNull())

        val expected = "No records match the condition"
        assertEquals(expected, exception.message)
    }

    @Test
    fun `awaitCount when user not exist`() = dsl.runTest { trx ->
        // Given
        val john = Users.john
        trx.suspend().insert(john)

        val jane = Users.jane
        trx.suspend().insert(jane)

        val expected = buildMap {
            put(checkFieldNotNull(john.id, USER.ID), 1)
            put(checkFieldNotNull(jane.id, USER.ID), 1)
        }

        // When
        val result = trx.suspend().count(USER, DSL.trueCondition(), USER.ID)

        // Then
        assertEquals(expected, result)
    }

    @Test
    fun checkFieldNotNull() {
        // Given
        val john = Users.john
        john.id = null

        // When
        val result = runCatching { checkFieldNotNull(john.id, USER.ID) }

        // Then
        val exception = assertIs<IllegalStateException>(result.exceptionOrNull())

        val expected = "Unexpectedly found 'null' while getting the value of the field '\"public\".\"user\".\"id\"'"
        assertEquals(expected, exception.message)
    }

    companion object {
        @Container
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:17.4")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test")

        lateinit var dsl: DSLContext
        lateinit var liquibase: Liquibase

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            // Liquibase
            val jdbcConnection = postgres.createConnection("")

            val resourceAccessor = ClassLoaderResourceAccessor()
            val liquibaseConnection = JdbcConnection(jdbcConnection)
            liquibase = Liquibase("changelog.xml", resourceAccessor, liquibaseConnection)

            // Update
            val update = CommandScope(*UpdateCommandStep.COMMAND_NAME).apply {
                addArgumentValue(DbUrlConnectionArgumentsCommandStep.DATABASE_ARG, liquibase.database)
                addArgumentValue(UpdateCommandStep.CHANGELOG_ARG, liquibase.databaseChangeLog)
                addArgumentValue(UpdateCommandStep.CHANGELOG_FILE_ARG, liquibase.changeLogFile)
                addArgumentValue(DatabaseChangelogCommandStep.CHANGELOG_PARAMETERS, liquibase.changeLogParameters)
            }

            update.execute()

            // JDBC
            val hikari = HikariConfig()
            hikari.jdbcUrl = postgres.jdbcUrl
            hikari.username = postgres.username
            hikari.password = postgres.password
            hikari.connectionTimeout = 30_000
            hikari.maximumPoolSize = 10

            val dataSource = HikariDataSource(hikari)

            // jOOQ
            val compatibleJOOQSettings = Settings()
                .withRenderNameCase(RenderNameCase.LOWER)
                .withRenderQuotedNames(RenderQuotedNames.ALWAYS)
                .withRenderKeywordCase(RenderKeywordCase.UPPER)
                .withExecuteLogging(true)

            val connectionProvider = DataSourceConnectionProvider(dataSource)
            val dialect = SQLDialect.POSTGRES
            dsl = DSL.using(connectionProvider, dialect, compatibleJOOQSettings)

            val configuration = StickyJDBCCoroutineDispatcherConfiguration(dsl)
            dsl.jdbcCoroutineDispatcher = StickyJDBCCoroutineDispatcher(configuration)
        }

        @AfterAll
        @JvmStatic
        fun afterAll() {
            liquibase.dropAll()
        }
    }
}
