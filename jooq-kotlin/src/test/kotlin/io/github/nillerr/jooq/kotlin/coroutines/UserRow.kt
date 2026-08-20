package io.github.nillerr.jooq.kotlin.coroutines

import org.jooq.Configuration
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.generated.tables.records.UserRecord
import org.jooq.generated.tables.references.USER
import java.time.LocalDateTime

class UserRow(private val record: UserRecord) {
    var id: Int?
        get() = record.id
        set(value) { record.id = value }

    var username: String
        get() = record.username
        set(value) { record.username = value }

    var email: String
        get() = record.email
        set(value) { record.email = value }

    var deactivated: Boolean
        get() = record.deactivated
        set(value) { record.deactivated = value }

    var createdAt: LocalDateTime?
        get() = record.createdAt
        set(value) { record.createdAt = value }

    fun asRecord(): UserRecord {
        return record
    }
}

class UserRepository {
    fun foo(db: Configuration) {
        db.dsl().selectFrom(USER)
            .toList()
            .map(::UserRow)
    }
}
