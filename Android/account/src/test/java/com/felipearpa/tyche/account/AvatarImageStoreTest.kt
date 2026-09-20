package com.felipearpa.tyche.account

import android.graphics.BitmapFactory
import coil3.Canvas
import coil3.Image
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.security.MessageDigest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class AvatarImageStoreTest {

    @Test
    fun `given a fresh validation when revalidation is requested then nothing is reloaded`() =
        runTest {
            val fixture = fixture()
            fixture.store.noteOriginExchange(downloaded("account-1", etag = ETAG_A))

            fixture.store.revalidateIfStale(accountId = "account-1")
            advanceUntilIdle()

            fixture.revalidator.requests.shouldBeEmpty()
        }

    @Test
    fun `given an account the origin never answered for then revalidation never runs`() =
        runTest {
            val fixture = fixture()

            fixture.store.revalidateIfStale(accountId = "account-1")
            advanceUntilIdle()

            fixture.revalidator.requests.shouldBeEmpty()
        }

    @Test
    fun `given a stale entry when concurrent revalidations are requested then exactly one runs at the smallest bucket in use`() =
        runTest {
            val fixture = fixture()
            fixture.store.noteRequestedBucket(accountId = "account-1", bucket = 256)
            fixture.store.noteRequestedBucket(accountId = "account-1", bucket = 64)
            fixture.store.noteOriginExchange(downloaded("account-1", etag = ETAG_A))
            fixture.now = 6.minutes.inWholeMilliseconds
            fixture.revalidator.gate = CompletableDeferred()

            fixture.store.revalidateIfStale(accountId = "account-1")
            fixture.store.revalidateIfStale(accountId = "account-1")

            fixture.revalidator.gate!!.complete(Unit)
            advanceUntilIdle()

            fixture.revalidator.requests shouldBe
                listOf(FakeAvatarRevalidator.Request(accountId = "account-1", bucket = 64))
        }

    @Test
    fun `given a fresh cached absence then a stale validation does not revalidate`() = runTest {
        val fixture = fixture()
        fixture.store.noteOriginExchange(downloaded("account-1", etag = ETAG_A))
        fixture.now = 6.minutes.inWholeMilliseconds
        fixture.store.recordMissing(accountId = "account-1")

        fixture.store.revalidateIfStale(accountId = "account-1")
        advanceUntilIdle()

        fixture.revalidator.requests.shouldBeEmpty()
    }

    @Test
    fun `given a stale entry when the origin answers with the same etag then validation freshens without a new generation`() =
        runTest {
            val fixture = fixture()
            fixture.store.noteOriginExchange(downloaded("account-1", etag = ETAG_A))
            fixture.now = 6.minutes.inWholeMilliseconds
            fixture.revalidator.onRevalidate = { accountId ->
                fixture.store.noteOriginExchange(notModified(accountId, etag = ETAG_A))
            }

            fixture.store.revalidateIfStale(accountId = "account-1")
            advanceUntilIdle()

            fixture.store.generation(accountId = "account-1") shouldBe 0
            fixture.memoryCache.removedKeys.shouldBeEmpty()

            // Now fresh: another request does not reload.
            fixture.store.revalidateIfStale(accountId = "account-1")
            advanceUntilIdle()
            fixture.revalidator.requests.size shouldBe 1
        }

    @Test
    fun `given decoded variants when the origin answers with another etag then the generation advances and the previous variants stay as placeholders`() =
        runTest {
            val fixture = fixture()
            fixture.store.noteRequestedBucket(accountId = "account-1", bucket = 64)
            fixture.store.noteOriginExchange(downloaded("account-1", etag = ETAG_A))

            // Inside the freshness window: a surface's own load at a new bucket met the change.
            fixture.store.noteOriginExchange(downloaded("account-1", etag = ETAG_B))

            fixture.store.generation(accountId = "account-1") shouldBe 1
            fixture.store.generations.value["account-1"] shouldBe 1
            fixture.memoryCache.removedKeys.shouldBeEmpty()

            // The answer also validated the entry, so nothing reloads inside the window.
            fixture.store.revalidateIfStale(accountId = "account-1")
            advanceUntilIdle()
            fixture.revalidator.requests.shouldBeEmpty()
        }

    @Test
    fun `given a replaced photo that Coil keeps downloading then the generation advances only once`() =
        runTest {
            val fixture = fixture()
            fixture.store.noteOriginExchange(downloaded("account-1", etag = ETAG_A))

            // Coil could not refresh its disk entry, so every re-keyed surface sends the old
            // validator again and gets the same replacement; and it retries an unusable 304
            // with a plain GET. Neither may re-key the surfaces again.
            fixture.store.noteOriginExchange(downloaded("account-1", etag = ETAG_B))
            fixture.store.noteOriginExchange(downloaded("account-1", etag = ETAG_B))
            fixture.store.noteOriginExchange(notModified("account-1", etag = ETAG_B))
            fixture.store.noteOriginExchange(downloaded("account-1", etag = ETAG_B))

            fixture.store.generation(accountId = "account-1") shouldBe 1
        }

    @Test
    fun `given a not modified answer for another photo than the one decoded then the generation advances`() =
        runTest {
            val fixture = fixture()
            fixture.store.noteOriginExchange(downloaded("account-1", etag = ETAG_A))
            fixture.store.noteOriginExchange(downloaded("account-1", etag = ETAG_B))

            // The disk entry never took B, and the remote went back to A: the 304 confirms the
            // disk, not what the surfaces show.
            fixture.store.noteOriginExchange(notModified("account-1", etag = ETAG_A))

            fixture.store.generation(accountId = "account-1") shouldBe 2
        }

    @Test
    fun `given no decoded variants yet when the first answer of the process arrives then the generation stays`() =
        runTest {
            val fixture = fixture()

            fixture.store.noteOriginExchange(downloaded("account-1", etag = ETAG_B))

            fixture.store.generation(accountId = "account-1") shouldBe 0
            fixture.store.generations.value["account-1"].shouldBeNull()
        }

    @Test
    fun `given an answer without an etag then the known photo is kept`() = runTest {
        val fixture = fixture()
        fixture.store.noteOriginExchange(downloaded("account-1", etag = ETAG_A))

        fixture.store.noteOriginExchange(notModified("account-1", etag = null))
        fixture.store.noteOriginExchange(notModified("account-1", etag = ETAG_A))

        fixture.store.generation(accountId = "account-1") shouldBe 0
    }

    @Test
    fun `given no known etag when one arrives then there is nothing to compare and the generation stays`() =
        runTest {
            val fixture = fixture()
            fixture.store.noteOriginExchange(downloaded("account-1", etag = null))

            fixture.store.noteOriginExchange(notModified("account-1", etag = ETAG_A))
            fixture.store.generation(accountId = "account-1") shouldBe 0

            // From here on the photo is known, so a different one is a replacement.
            fixture.store.noteOriginExchange(downloaded("account-1", etag = ETAG_B))
            fixture.store.generation(accountId = "account-1") shouldBe 1
        }

    @Test
    fun `given an answer to a request sent before a local upload then it cannot displace the upload`() =
        runTest {
            val fixture = fixture()
            val jpegData = byteArrayOf(1, 2, 3)
            fixture.store.noteOriginExchange(downloaded("account-1", etag = ETAG_A, requestedAt = 0L))
            fixture.now = 1_000L
            fixture.store.installLocalAvatar(
                image = FakeImage(),
                jpegData = jpegData,
                accountId = "account-1",
            )

            // A revalidation of the replaced photo was on the wire while the PUT succeeded.
            fixture.store.noteOriginExchange(notModified("account-1", etag = ETAG_A, requestedAt = 900L))
            fixture.store.generation(accountId = "account-1") shouldBe 1

            // The upload's own deferred download still matches the seeded identity.
            fixture.store.noteOriginExchange(
                downloaded("account-1", etag = quotedMd5(jpegData), requestedAt = 2_000L),
            )
            fixture.store.generation(accountId = "account-1") shouldBe 1
        }

    @Test
    fun `given an origin failure then the entry stays stale and keeps its generation`() = runTest {
        val fixture = fixture()
        fixture.store.noteOriginExchange(downloaded("account-1", etag = ETAG_A))
        fixture.now = 6.minutes.inWholeMilliseconds

        fixture.store.noteOriginExchange(
            AvatarOriginExchange(
                accountId = "account-1",
                status = 500,
                etag = ETAG_B,
                requestedAtEpochMillis = Long.MAX_VALUE,
            ),
        )

        fixture.store.generation(accountId = "account-1") shouldBe 0
        fixture.store.revalidateIfStale(accountId = "account-1")
        advanceUntilIdle()
        fixture.revalidator.requests.size shouldBe 1
    }

    @Test
    fun `given a revalidation that never reaches the origin then the entry stays stale and a later request retries`() =
        runTest {
            val fixture = fixture()
            fixture.store.noteOriginExchange(downloaded("account-1", etag = ETAG_A))
            fixture.now = 6.minutes.inWholeMilliseconds

            fixture.store.revalidateIfStale(accountId = "account-1")
            advanceUntilIdle()

            fixture.store.generation(accountId = "account-1") shouldBe 0

            fixture.store.revalidateIfStale(accountId = "account-1")
            advanceUntilIdle()

            fixture.revalidator.requests.size shouldBe 2
        }

    @Test
    fun `given a local upload when Coil later downloads that same photo then the generation stays`() =
        runTest {
            val fixture = fixture()
            val jpegData = byteArrayOf(1, 2, 3)
            fixture.store.installLocalAvatar(
                image = FakeImage(),
                jpegData = jpegData,
                accountId = "account-1",
            )

            // Freshly installed: nothing reloads inside the window.
            fixture.store.revalidateIfStale(accountId = "account-1")
            advanceUntilIdle()
            fixture.revalidator.requests.shouldBeEmpty()

            // Coil's disk entry still holds the replaced photo, so the origin sends the upload.
            fixture.now = 6.minutes.inWholeMilliseconds
            fixture.store.noteOriginExchange(downloaded("account-1", etag = quotedMd5(jpegData)))
            fixture.store.generation(accountId = "account-1") shouldBe 1

            // Another device replaces it afterwards.
            fixture.store.noteOriginExchange(downloaded("account-1", etag = ETAG_B))
            fixture.store.generation(accountId = "account-1") shouldBe 2
        }

    @Test
    fun `given more accounts than are tracked then the one composed least recently loses its buckets`() =
        runTest {
            val fixture = fixture()
            fixture.store.noteRequestedBucket(accountId = "composed-first", bucket = 96)
            fixture.store.noteRequestedBucket(accountId = "signed-in", bucket = 128)
            repeat(MAX_TRACKED_ACCOUNTS - 1) { index ->
                fixture.store.noteRequestedBucket(accountId = "gambler-$index", bucket = 160)
            }

            listOf("composed-first", "signed-in").forEach { accountId ->
                fixture.store.installLocalAvatar(
                    image = FakeImage(),
                    jpegData = byteArrayOf(1),
                    accountId = accountId,
                )
            }

            fixture.memoryCache.writes.map { write -> write.key }
                .shouldContainExactlyInAnyOrder(
                    "avatar#composed-first#1#512",
                    "avatar#signed-in#1#128",
                    "avatar#signed-in#1#512",
                )
        }

    @Test
    fun `given a recorded missing avatar then loads are suppressed only inside the freshness window`() =
        runTest {
            val fixture = fixture()

            fixture.store.recordMissing(accountId = "account-1")

            fixture.store.shouldAttemptLoad(accountId = "account-1").shouldBeFalse()

            fixture.now = 6.minutes.inWholeMilliseconds

            fixture.store.shouldAttemptLoad(accountId = "account-1").shouldBeTrue()
        }

    @Test
    fun `missing retry delay is null without a cached absence and counts down to a floor of 100ms`() =
        runTest {
            val fixture = fixture()

            fixture.store.missingRetryDelayMillis(accountId = "account-1").shouldBeNull()

            fixture.now = 1_000L
            fixture.store.recordMissing(accountId = "account-1")

            fixture.now = 61_000L
            fixture.store.missingRetryDelayMillis(accountId = "account-1") shouldBe
                5.minutes.inWholeMilliseconds - 60_000L

            fixture.now = 1_000L + 5.minutes.inWholeMilliseconds - 10L
            fixture.store.missingRetryDelayMillis(accountId = "account-1") shouldBe 100L
        }

    @Test
    fun `given the missing map is at capacity then the oldest entry is evicted`() = runTest {
        val fixture = fixture()
        repeat(512) { index ->
            fixture.now = index.toLong()
            fixture.store.recordMissing(accountId = "account-$index")
        }

        fixture.now = 512L
        fixture.store.recordMissing(accountId = "account-newest")

        fixture.store.shouldAttemptLoad(accountId = "account-0").shouldBeTrue()
        fixture.store.shouldAttemptLoad(accountId = "account-1").shouldBeFalse()
        fixture.store.shouldAttemptLoad(accountId = "account-newest").shouldBeFalse()
    }

    @Test
    fun `given a local upload then the image seeds tracked buckets plus 512 under a new generation and old keys are removed`() =
        runTest {
            val fixture = fixture()
            val image = FakeImage()
            fixture.store.noteRequestedBucket(accountId = "account-1", bucket = 64)
            fixture.store.recordMissing(accountId = "account-1")

            fixture.store.installLocalAvatar(
                image = image,
                jpegData = byteArrayOf(1, 2, 3),
                accountId = "account-1",
            )

            fixture.store.generation(accountId = "account-1") shouldBe 1
            fixture.store.generations.value["account-1"] shouldBe 1
            fixture.memoryCache.writes.map { write -> write.key }
                .shouldContainExactlyInAnyOrder(
                    "avatar#account-1#1#64",
                    "avatar#account-1#1#512",
                )
            fixture.memoryCache.writes.all { write -> write.image === image }.shouldBeTrue()
            fixture.memoryCache.removedKeys.shouldContainExactlyInAnyOrder(
                "avatar#account-1#0#64",
                "avatar#account-1#0#512",
            )
            fixture.store.shouldAttemptLoad(accountId = "account-1").shouldBeTrue()
        }

    @Test
    fun `given an empty account id then a local upload seeds nothing`() = runTest {
        val fixture = fixture()

        fixture.store.installLocalAvatar(
            image = FakeImage(),
            jpegData = byteArrayOf(1, 2, 3),
            accountId = "",
        )

        fixture.memoryCache.writes.shouldBeEmpty()
        fixture.store.generations.value[""].shouldBeNull()
    }

    @Test
    fun `given undecodable bytes when the uploaded avatar is installed then nothing is seeded`() =
        runTest {
            val fixture = fixture()
            mockkStatic(BitmapFactory::class)
            try {
                every { BitmapFactory.decodeByteArray(any(), any(), any()) } returns null

                fixture.store.installUploadedAvatar(
                    jpegData = byteArrayOf(1),
                    accountId = "account-1",
                )

                fixture.memoryCache.writes.shouldBeEmpty()
                fixture.store.generation(accountId = "account-1") shouldBe 0
            } finally {
                unmockkStatic(BitmapFactory::class)
            }
        }

    @Test
    fun `given decodable bytes when the uploaded avatar is installed then it seeds through the local install path`() =
        runTest {
            val fixture = fixture()
            fixture.store.noteRequestedBucket(accountId = "account-1", bucket = 64)
            mockkStatic(BitmapFactory::class)
            try {
                every {
                    BitmapFactory.decodeByteArray(any(), any(), any())
                } returns mockk(relaxed = true)

                fixture.store.installUploadedAvatar(
                    jpegData = byteArrayOf(1, 2, 3),
                    accountId = "account-1",
                )

                fixture.store.generation(accountId = "account-1") shouldBe 1
                fixture.memoryCache.writes.map { write -> write.key }
                    .shouldContainExactlyInAnyOrder(
                        "avatar#account-1#1#64",
                        "avatar#account-1#1#512",
                    )
            } finally {
                unmockkStatic(BitmapFactory::class)
            }
        }

    @Test
    fun `pixel buckets round up to a 32-pixel stride and cap at the 512-pixel source`() {
        avatarPixelBucket(targetPx = 1) shouldBe 32
        avatarPixelBucket(targetPx = 32) shouldBe 32
        avatarPixelBucket(targetPx = 33) shouldBe 64
        avatarPixelBucket(targetPx = 511) shouldBe 512
        avatarPixelBucket(targetPx = 2000) shouldBe 512
        avatarPixelBucket(targetPx = 0) shouldBe 32
    }

    private fun TestScope.fixture(): Fixture {
        val memoryCache = FakeAvatarMemoryCacheGateway()
        val revalidator = FakeAvatarRevalidator()
        val fixture = Fixture(memoryCache = memoryCache, revalidator = revalidator)
        fixture.store = AvatarImageStore(
            memoryCache = memoryCache,
            revalidator = revalidator,
            scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
            nowEpochMillis = { fixture.now },
        )
        return fixture
    }

    private class Fixture(
        val memoryCache: FakeAvatarMemoryCacheGateway,
        val revalidator: FakeAvatarRevalidator,
    ) {
        lateinit var store: AvatarImageStore
        var now: Long = 0L
    }
}

private const val ETAG_A = "\"a\""
private const val ETAG_B = "\"b\""

private fun downloaded(accountId: String, etag: String?, requestedAt: Long = Long.MAX_VALUE) =
    AvatarOriginExchange(
        accountId = accountId,
        status = 200,
        etag = etag,
        requestedAtEpochMillis = requestedAt,
    )

private fun notModified(accountId: String, etag: String?, requestedAt: Long = Long.MAX_VALUE) =
    AvatarOriginExchange(
        accountId = accountId,
        status = 304,
        etag = etag,
        requestedAtEpochMillis = requestedAt,
    )

private fun quotedMd5(data: ByteArray): String =
    MessageDigest.getInstance("MD5").digest(data)
        .joinToString(separator = "", prefix = "\"", postfix = "\"") { byte ->
            "%02x".format(byte)
        }

private class FakeAvatarMemoryCacheGateway : AvatarMemoryCacheGateway {
    data class Write(val key: String, val image: Image)

    val writes = mutableListOf<Write>()
    val removedKeys = mutableListOf<String>()

    override fun write(key: String, image: Image) {
        writes += Write(key = key, image = image)
    }

    override fun remove(key: String) {
        removedKeys += key
    }
}

private class FakeAvatarRevalidator : AvatarRevalidator {
    data class Request(val accountId: String, val bucket: Int)

    var gate: CompletableDeferred<Unit>? = null
    var onRevalidate: (accountId: String) -> Unit = {}
    val requests = mutableListOf<Request>()

    override suspend fun revalidate(accountId: String, bucket: Int) {
        requests += Request(accountId = accountId, bucket = bucket)
        gate?.await()
        onRevalidate(accountId)
    }
}

private class FakeImage : Image {
    override val size: Long = 4L
    override val width: Int = 1
    override val height: Int = 1
    override val shareable: Boolean = true

    override fun draw(canvas: Canvas) = Unit
}
