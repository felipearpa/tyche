package com.felipearpa.tyche.account

import android.graphics.BitmapFactory
import coil3.Image
import coil3.asImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Accounting budget for decoded avatar pixels in the singleton Coil memory cache.
 */
const val AVATAR_MEMORY_BUDGET_BYTES: Long = 16L * 1024L * 1024L

/**
 * Single configuration point for how long a successful avatar validation — or a cached
 * absence — stays fresh.
 */
val avatarFreshness: Duration = 5.minutes

internal const val AVATAR_MEMORY_CACHE_KEY_PREFIX = "avatar#"
internal const val AVATAR_PIXEL_BUCKET_STRIDE = 32
internal const val AVATAR_MAX_PIXEL_SIZE = 512

/**
 * Rounds a measured pixel target up to the smallest stable 32-pixel bucket that covers it,
 * capped at the 512-pixel upload size, so near-identical layouts share one decoded variant.
 */
fun avatarPixelBucket(targetPx: Int): Int {
    val target = maxOf(targetPx, 1)
    val stride = AVATAR_PIXEL_BUCKET_STRIDE
    return (((target + stride - 1) / stride) * stride).coerceAtMost(AVATAR_MAX_PIXEL_SIZE)
}

internal fun avatarMemoryCacheKey(accountId: String, generation: Int, bucket: Int): String =
    "$AVATAR_MEMORY_CACHE_KEY_PREFIX$accountId#$generation#$bucket"

/** The previous generation's variant at the same bucket; `null` for the first generation. */
internal fun avatarPlaceholderMemoryCacheKey(accountId: String, generation: Int, bucket: Int): String? =
    (generation - 1).takeIf { previous -> previous >= 0 }?.let { previous ->
        avatarMemoryCacheKey(accountId = accountId, generation = previous, bucket = bucket)
    }

internal interface AvatarMemoryCacheGateway {
    fun write(key: String, image: Image)
    fun remove(key: String)
}

internal interface AvatarRevalidator {
    /**
     * Sends [accountId]'s photo through the image pipeline again, past the memory cache. The
     * outcome is not returned: the pipeline reports every origin answer, this one included, to
     * [AvatarImageStore.noteOriginExchange].
     */
    suspend fun revalidate(accountId: String, bucket: Int)
}

/** How the avatar origin answered one image request; [etag] identifies the remote photo. */
internal data class AvatarOriginExchange(
    val accountId: String,
    val status: Int,
    val etag: String?,
    val requestedAtEpochMillis: Long,
)

/** The answers Coil turns into an image: a body, or leave to use the one it has. */
internal fun isUsableOriginStatus(status: Int): Boolean = status in 200..299 || status == 304

/**
 * Application-scoped bookkeeping for shared avatar loading. Decoded pixels live only in the
 * singleton Coil memory cache, and Coil's disk cache revalidates with the stored `ETag` on
 * every load that misses memory. This store owns what Coil cannot express:
 *
 * - a process-local generation per account, carried in every memory key so replacing a photo
 *   re-keys all consumers at once;
 * - which remote photo the current generation shows. The image pipeline reports every origin
 *   answer, with its `ETag`, through [noteOriginExchange]: the same `ETag` only refreshes the
 *   validation time, a different one advances the generation. Because memory hits never reach
 *   the origin, a validation older than the freshness interval sends one coalesced load past
 *   the memory cache to ask again;
 * - bounded missing-avatar entries shared across surfaces for the freshness interval, so
 *   absent photos do not repeat not-found requests;
 * - the buckets each account was requested at, so an upload can seed every visible surface
 *   from local memory without a GET.
 */
class AvatarImageStore internal constructor(
    private val memoryCache: AvatarMemoryCacheGateway,
    private val revalidator: AvatarRevalidator,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val nowEpochMillis: () -> Long = { System.currentTimeMillis() },
    private val freshness: Duration = avatarFreshness,
) {
    private val lock = Any()
    // Never pruned: an account forgotten while its variants are still decoded would adopt the
    // next `ETag` as its first and keep showing a replaced photo.
    private var validationByAccount: Map<String, AvatarValidation> = emptyMap()
    private var missingSinceByAccount: Map<String, Long> = emptyMap()
    private var requestedBucketsByAccount: Map<String, Set<Int>> = emptyMap()
    private var revalidationsInFlight: Set<String> = emptySet()

    private val mutableGenerations = MutableStateFlow<Map<String, Int>>(emptyMap())

    /** Per-account avatar generations; published on replacement so composables re-key. */
    val generations: StateFlow<Map<String, Int>> = mutableGenerations.asStateFlow()

    fun generation(accountId: String): Int = generations.value[accountId] ?: 0

    /**
     * Records that a surface renders [accountId] at [bucket], so uploads can seed it. The account
     * whose surfaces entered composition least recently is dropped first; the bound is far above
     * any pool, because a toolbar that stays composed through a long leaderboard is not re-noted
     * and the signed-in gambler — the only one who uploads — must keep its buckets.
     */
    fun noteRequestedBucket(accountId: String, bucket: Int) {
        synchronized(lock) {
            val buckets = requestedBucketsByAccount[accountId].orEmpty() + bucket
            val others = requestedBucketsByAccount - accountId
            val bounded = if (others.size >= MAX_TRACKED_ACCOUNTS) {
                others - others.keys.first()
            } else {
                others
            }
            requestedBucketsByAccount = bounded + (accountId to buckets)
        }
    }

    /** `false` only while a fresh missing-avatar entry says the object is absent. */
    fun shouldAttemptLoad(accountId: String): Boolean =
        synchronized(lock) { !isMissingFresh(accountId) }

    /**
     * Records one origin answer. The `ETag` is the identity of the remote photo, so an answer
     * carrying another one than the current generation was decoded from means the photo was
     * replaced: the generation advances and every consumer reloads, keeping what it shows until
     * the replacement is ready. Comparing identities, not status codes, keeps that idempotent —
     * Coil repeats a `200` when it cannot refresh its disk entry, and retries an unusable `304`
     * with a plain GET. The first answer of a process has no decoded variant to supersede, and
     * an answer to a request sent before a local upload describes the photo that upload replaced.
     */
    internal fun noteOriginExchange(exchange: AvatarOriginExchange) {
        if (!isUsableOriginStatus(exchange.status)) return
        val accountId = exchange.accountId
        synchronized(lock) {
            val known = validationByAccount[accountId]
            val installedAt = known?.installedAtEpochMillis
            if (installedAt != null && exchange.requestedAtEpochMillis <= installedAt) return

            val etag = exchange.etag ?: known?.etag
            validationByAccount = validationByAccount + (
                accountId to AvatarValidation(
                    validatedAtEpochMillis = nowEpochMillis(),
                    etag = etag,
                    installedAtEpochMillis = installedAt,
                )
                )
            if (known?.etag != null && etag != known.etag) {
                mutableGenerations.value =
                    mutableGenerations.value + (accountId to generation(accountId) + 1)
            }
        }
    }

    fun recordMissing(accountId: String) {
        synchronized(lock) {
            val atCapacity = accountId !in missingSinceByAccount &&
                missingSinceByAccount.size >= MAX_MISSING_ENTRIES
            val bounded = if (atCapacity) {
                missingSinceByAccount.minByOrNull { entry -> entry.value }
                    ?.let { oldest -> missingSinceByAccount - oldest.key }
                    ?: missingSinceByAccount
            } else {
                missingSinceByAccount
            }
            missingSinceByAccount = bounded + (accountId to nowEpochMillis())
        }
    }

    fun clearMissing(accountId: String) {
        synchronized(lock) {
            missingSinceByAccount = missingSinceByAccount - accountId
        }
    }

    /**
     * Milliseconds until the cached absence for [accountId] expires, or `null` when no absence
     * is recorded; never below 100 so an on-screen retry cannot busy-loop.
     */
    fun missingRetryDelayMillis(accountId: String): Long? = synchronized(lock) {
        val missingSince = missingSinceByAccount[accountId] ?: return null
        (freshness.inWholeMilliseconds - (nowEpochMillis() - missingSince)).coerceAtLeast(100)
    }

    /**
     * No-op while the entry (or its cached absence) is fresh, a revalidation is already in
     * flight, or the account was never validated; otherwise sends one coalesced load past the
     * memory cache at the smallest bucket in use. A failed load reports nothing, so the entry
     * stays stale and a later access retries.
     */
    fun revalidateIfStale(accountId: String) {
        val bucket = synchronized(lock) {
            if (accountId in revalidationsInFlight) return
            if (accountId !in validationByAccount) return
            if (isValidationFresh(accountId) || isMissingFresh(accountId)) return
            revalidationsInFlight = revalidationsInFlight + accountId
            requestedBucketsByAccount[accountId]?.minOrNull() ?: AVATAR_PIXEL_BUCKET_STRIDE
        }

        scope.launch {
            try {
                revalidator.revalidate(accountId = accountId, bucket = bucket)
            } finally {
                synchronized(lock) {
                    revalidationsInFlight = revalidationsInFlight - accountId
                }
            }
        }
    }

    /**
     * Seeds the just-uploaded image under a new generation for every requested bucket plus the
     * full 512-pixel size, so all visible surfaces update from memory without a GET. The upload's
     * `ETag` is known locally, so when Coil's disk entry, which still holds the replaced photo,
     * is next revalidated, the download it triggers is recognized as this same photo.
     */
    fun installLocalAvatar(image: Image, jpegData: ByteArray, accountId: String) {
        if (accountId.isEmpty()) return
        synchronized(lock) {
            val previousGeneration = generation(accountId)
            val newGeneration = previousGeneration + 1
            val buckets =
                requestedBucketsByAccount[accountId].orEmpty() + AVATAR_MAX_PIXEL_SIZE

            buckets.forEach { bucket ->
                memoryCache.write(
                    key = avatarMemoryCacheKey(accountId, newGeneration, bucket),
                    image = image,
                )
                memoryCache.remove(key = avatarMemoryCacheKey(accountId, previousGeneration, bucket))
            }

            val installedAt = nowEpochMillis()
            validationByAccount = validationByAccount + (
                accountId to AvatarValidation(
                    validatedAtEpochMillis = installedAt,
                    etag = s3SimplePutEtag(jpegData),
                    installedAtEpochMillis = installedAt,
                )
                )
            missingSinceByAccount = missingSinceByAccount - accountId
            mutableGenerations.value = mutableGenerations.value + (accountId to newGeneration)
        }
    }

    /**
     * Decodes a just-uploaded JPEG and seeds it through [installLocalAvatar]; undecodable
     * bytes are ignored so a corrupt payload cannot disturb what is already cached.
     */
    suspend fun installUploadedAvatar(jpegData: ByteArray, accountId: String) {
        val bitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.size) ?: return
        installLocalAvatar(image = bitmap.asImage(), jpegData = jpegData, accountId = accountId)
    }

    private fun isValidationFresh(accountId: String): Boolean {
        val validatedAt = validationByAccount[accountId]?.validatedAtEpochMillis ?: return false
        return nowEpochMillis() - validatedAt < freshness.inWholeMilliseconds
    }

    private fun isMissingFresh(accountId: String): Boolean {
        val missingSince = missingSinceByAccount[accountId] ?: return false
        return nowEpochMillis() - missingSince < freshness.inWholeMilliseconds
    }
}

private data class AvatarValidation(
    val validatedAtEpochMillis: Long,
    val etag: String?,
    val installedAtEpochMillis: Long? = null,
)

/** S3 computes a simple (non-multipart) PUT's `ETag` as the quoted MD5 of the body. */
private fun s3SimplePutEtag(jpegData: ByteArray): String {
    val digest = MessageDigest.getInstance("MD5").digest(jpegData)
    return digest.joinToString(separator = "", prefix = "\"", postfix = "\"") { byte ->
        "%02x".format(byte)
    }
}

internal const val MAX_TRACKED_ACCOUNTS = 1024
private const val MAX_MISSING_ENTRIES = 512
