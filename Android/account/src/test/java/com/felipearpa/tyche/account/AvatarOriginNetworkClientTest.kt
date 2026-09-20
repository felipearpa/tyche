package com.felipearpa.tyche.account

import coil3.network.HttpException
import coil3.network.NetworkClient
import coil3.network.NetworkHeaders
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class AvatarOriginNetworkClientTest {

    @Test
    fun `given an avatar request then the answer and its etag are reported after Coil consumed the response`() =
        runTest {
            val fixture = fixture(originStatus = 304, etag = "\"etag\"")

            val consumed = fixture.client.executeRequest(
                NetworkRequest(url = AvatarUrl.of("account-1")),
            ) { response ->
                fixture.reported.shouldBeEmpty()
                response.code
            }

            consumed shouldBe 304
            fixture.reported shouldBe listOf(
                AvatarOriginExchange(
                    accountId = "account-1",
                    status = 304,
                    etag = "\"etag\"",
                    requestedAtEpochMillis = REQUESTED_AT,
                ),
            )
        }

    @Test
    fun `given a failure status then the request fails before Coil sees the response and nothing is reported`() =
        runTest {
            val fixture = fixture(originStatus = 504, etag = null)

            val exception = shouldThrow<HttpException> {
                fixture.client.executeRequest(NetworkRequest(url = AvatarUrl.of("account-1"))) {
                    error("Coil must not receive a response it would leak its disk snapshot on")
                }
            }

            exception.response.code shouldBe 504
            fixture.reported.shouldBeEmpty()
        }

    @Test
    fun `given Coil fails while consuming the response then nothing is reported`() = runTest {
        val fixture = fixture(originStatus = 200, etag = "\"etag\"")

        shouldThrow<IllegalStateException> {
            fixture.client.executeRequest(NetworkRequest(url = AvatarUrl.of("account-1"))) {
                error("disk full")
            }
        }

        fixture.reported.shouldBeEmpty()
    }

    @Test
    fun `given a request for anything but an avatar then it passes through untouched`() = runTest {
        val fixture = fixture(originStatus = 404, etag = null)

        val consumed = fixture.client.executeRequest(
            NetworkRequest(url = "https://example.com/flag.png"),
        ) { response -> response.code }

        consumed shouldBe 404
        fixture.reported.shouldBeEmpty()
    }

    @Test
    fun `avatar url maps back to its account and rejects every other url`() {
        AvatarUrl.accountIdOf(AvatarUrl.of("account-1")) shouldBe "account-1"
        AvatarUrl.accountIdOf(AvatarUrl.of("account-1") + "?X-Amz-Signature=abc") shouldBe "account-1"
        AvatarUrl.accountIdOf(AvatarUrl.of("")) shouldBe null
        AvatarUrl.accountIdOf(AvatarUrl.of("nested/account")) shouldBe null
        AvatarUrl.accountIdOf("https://example.com/avatars/account-1.jpg") shouldBe null
    }

    private fun fixture(originStatus: Int, etag: String?): Fixture {
        val reported = mutableListOf<AvatarOriginExchange>()
        return Fixture(
            client = AvatarOriginNetworkClient(
                delegate = FixedAnswerNetworkClient(status = originStatus, etag = etag),
                nowEpochMillis = { REQUESTED_AT },
                onReportExchange = { exchange -> reported += exchange },
            ),
            reported = reported,
        )
    }

    private class Fixture(val client: NetworkClient, val reported: List<AvatarOriginExchange>)

    private companion object {
        const val REQUESTED_AT = 42L
    }
}

private class FixedAnswerNetworkClient(
    private val status: Int,
    private val etag: String?,
) : NetworkClient {
    override suspend fun <T> executeRequest(
        request: NetworkRequest,
        block: suspend (response: NetworkResponse) -> T,
    ): T = block(
        NetworkResponse(
            code = status,
            headers = NetworkHeaders.Builder()
                .apply { etag?.let { value -> set("ETag", value) } }
                .build(),
        ),
    )
}
