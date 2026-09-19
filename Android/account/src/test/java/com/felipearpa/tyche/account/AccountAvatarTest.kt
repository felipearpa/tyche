package com.felipearpa.tyche.account

import coil3.annotation.ExperimentalCoilApi
import coil3.network.NetworkHeaders
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import coil3.network.cachecontrol.CacheControlCacheStrategy
import coil3.request.Options
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoilApi::class)
class AccountAvatarTest {
    @Test
    fun `email compatibility wrapper keeps using the local part for its initial`() {
        "felipe.arpa".avatarInitial() shouldBe 'F'
        "".avatarInitial() shouldBe null
    }

    @Test
    fun `username fallback uses the first available letter or number`() {
        "__élgoleador".avatarInitial() shouldBe 'É'
        "⚽ 88".avatarInitial() shouldBe '8'
        "___".avatarInitial() shouldBe null
    }

    @Test
    fun `generated avatar colors meet enhanced contrast in both themes`() {
        listOf(false, true).forEach { isDark ->
            val (background, foreground) = avatarColorsFor(
                key = "ElGoleador",
                isDark = isDark,
            )
            contrastRatio(background, foreground).shouldBeGreaterThanOrEqual(7.0)
        }
    }

    @Test
    fun `avatar request uses the deterministic public URL and the bucketed size`() {
        val request = avatarPhotoRequestConfiguration(accountId = "account-1", bucket = 64)

        request.url shouldBe
            "https://tyche-avatars.s3.us-east-2.amazonaws.com/avatars/account-1.jpg"
        request.diskCacheKey shouldBe request.url
        request.sizePx shouldBe 64
    }

    @Test
    fun `memory keys carry the generation and a new generation falls back to the previous one`() {
        avatarMemoryCacheKey(accountId = "account-1", generation = 3, bucket = 64) shouldBe
            "avatar#account-1#3#64"
        avatarPlaceholderMemoryCacheKey(accountId = "account-1", generation = 3, bucket = 64) shouldBe
            "avatar#account-1#2#64"
        avatarPlaceholderMemoryCacheKey(accountId = "account-1", generation = 0, bucket = 64) shouldBe
            null
    }

    @Test
    fun `stored avatar is revalidated with its ETag instead of being downloaded again`() = runTest {
        val readResult = readThroughCacheStrategy(
            storedHeaders = NetworkHeaders.Builder()
                .set("ETag", STORED_ETAG)
                .set("Cache-Control", "max-age=0, must-revalidate")
                .build(),
        )

        readResult.response shouldBe null
        readResult.request?.headers?.get("If-None-Match") shouldBe STORED_ETAG
    }

    @Test
    fun `stored avatar the origin declared fresh is still revalidated, never served blindly`() = runTest {
        // An object stored without the bucket's `must-revalidate` would be served from disk for
        // its whole lifetime; the request directive is what still forces the conditional GET.
        val readResult = readThroughCacheStrategy(
            storedHeaders = NetworkHeaders.Builder()
                .set("ETag", STORED_ETAG)
                .set("Cache-Control", "max-age=3600")
                .build(),
        )

        readResult.response shouldBe null
        readResult.request?.headers?.get("If-None-Match") shouldBe STORED_ETAG
    }

    // Runs the avatar request's headers through the cache strategy the app installs on Coil.
    private suspend fun readThroughCacheStrategy(storedHeaders: NetworkHeaders) =
        avatarPhotoRequestConfiguration(accountId = "account-1", bucket = 64)
            .let { configuration ->
                CacheControlCacheStrategy().read(
                    cacheResponse = System.currentTimeMillis().let { now ->
                        NetworkResponse(
                            requestMillis = now,
                            responseMillis = now,
                            headers = storedHeaders,
                        )
                    },
                    networkRequest = NetworkRequest(
                        url = configuration.url,
                        headers = NetworkHeaders.Builder()
                            .set("Cache-Control", configuration.cacheControl)
                            .build(),
                    ),
                    options = Options(context = mockk()),
                )
            }

    private companion object {
        const val STORED_ETAG = "\"5b660e06a59470bd760a4f4f701a001c\""
    }
}
