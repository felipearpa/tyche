package com.felipearpa.tyche.session

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class CurrentAccountSnapshotTest {

    @Test
    fun `given an envelope with a validation time when encoded and decoded then it round-trips`() {
        val snapshot = CurrentAccountSnapshot(
            account = AccountBundle(
                accountId = "account-1",
                externalAccountId = "external-1",
                email = "gambler@tyche.com",
            ).withUsername("ElGoleador"),
            validatedAtEpochMillis = 1_722_470_400_000L,
        )

        val decoded = decodeCurrentAccountSnapshot(Json.encodeToString(snapshot))

        decoded shouldBe snapshot
    }

    @Test
    fun `given a legacy raw account bundle when decoded then it migrates as a never-validated snapshot with the same fields`() {
        val legacy =
            """{"accountId":"a","externalAccountId":"e","email":"x@y.z","username":"name"}"""

        val decoded = decodeCurrentAccountSnapshot(legacy)

        decoded.shouldNotBeNull()
        decoded.validatedAtEpochMillis.shouldBeNull()
        decoded.account.accountId shouldBe "a"
        decoded.account.externalAccountId shouldBe "e"
        decoded.account.email shouldBe "x@y.z"
        decoded.account.username shouldBe "name"
    }

    @Test
    fun `given a legacy bundle without a username when decoded then the username falls back to the email`() {
        val legacy = """{"accountId":"a","externalAccountId":"e","email":"x@y.z"}"""

        val decoded = decodeCurrentAccountSnapshot(legacy)

        decoded.shouldNotBeNull()
        decoded.validatedAtEpochMillis.shouldBeNull()
        decoded.account.username shouldBe "x@y.z"
    }

    @Test
    fun `given an unrecognizable payload when decoded then it yields null instead of throwing`() {
        decodeCurrentAccountSnapshot("not json at all").shouldBeNull()
        decodeCurrentAccountSnapshot("""{"unrelated":true}""").shouldBeNull()
        decodeCurrentAccountSnapshot("").shouldBeNull()
    }
}
