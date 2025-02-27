package io.github.nillerr.jooq.kotlin.coroutines

import org.jooq.generated.tables.records.UserRecord
import java.time.LocalDateTime

object Users {
    val lisa: UserRecord
        get() = UserRecord(
            id = -1,
            username = "lisadoe",
            email = "lisadoe@example.com",
            createdAt = LocalDateTime.parse("2020-02-02T02:02:02"),
        )

    val john: UserRecord
        get() = UserRecord(
            id = 1,
            username = "johndoe",
            email = "johndoe@example.com",
            deactivated = false,
            createdAt = LocalDateTime.parse("2020-02-02T02:02:02"),
        )

    val jane: UserRecord
        get() = UserRecord(
            id = 2,
            username = "janedoe",
            email = "janedoe@example.com",
            createdAt = LocalDateTime.parse("2020-02-02T02:02:02"),
        )
}
