package com.felipearpa.tyche.account

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.PlatformContext
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.compose.useExistingImageAsPlaceholder
import coil3.decode.DataSource
import coil3.network.HttpException
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.size.Size
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

data class AccountAvatarFallback(
    val identity: String,
    val colorKey: String = identity,
    val backgroundColor: Color? = null,
    val foregroundColor: Color? = null,
)

/**
 * The account's avatar photo with the letter avatar as fallback — the single composable
 * behind every surface that shows a gambler. Loading is routed through the shared
 * [AvatarImageStore]: the memory-cache key carries the account's process-local generation and
 * the rendered pixel bucket, so an in-session upload re-keys every surface at once and
 * near-identical layouts share one decoded variant. Fresh entries are served from memory,
 * stale ones send one load past the memory cache so Coil revalidates against the fixed URL,
 * and a fresh cached absence renders the fallback without repeating the not-found request.
 */
@Composable
fun AccountAvatar(accountId: String, email: String, modifier: Modifier = Modifier) {
    AccountAvatar(
        accountId = accountId,
        fallback = AccountAvatarFallback(
            identity = email.substringBefore('@'),
            colorKey = email,
        ),
        modifier = modifier,
    )
}

@Composable
fun AccountAvatar(
    accountId: String,
    fallback: AccountAvatarFallback,
    modifier: Modifier = Modifier,
) {
    if (LocalInspectionMode.current || accountId.isEmpty()) {
        InitialAvatar(
            identity = fallback.identity,
            colorKey = fallback.colorKey,
            backgroundColor = fallback.backgroundColor,
            foregroundColor = fallback.foregroundColor,
            modifier = modifier,
        )
        return
    }

    val avatarImageStore = koinInject<AvatarImageStore>()
    val generations by avatarImageStore.generations.collectAsStateWithLifecycle()
    val generation = generations[accountId] ?: 0

    // Bumped when a cached absence expires while this surface stays composed, so long-lived
    // screens re-evaluate the fallback instead of showing the letter avatar forever.
    var missingRetryTick by remember(accountId) { mutableIntStateOf(0) }
    val attemptLoad = remember(accountId, generation, missingRetryTick) {
        avatarImageStore.shouldAttemptLoad(accountId)
    }

    BoxWithConstraints(modifier = modifier) {
        val bucket = avatarPixelBucket(
            targetPx = if (constraints.hasBoundedWidth) {
                constraints.maxWidth
            } else {
                AVATAR_MAX_PIXEL_SIZE
            },
        )

        LaunchedEffect(accountId, bucket) {
            avatarImageStore.noteRequestedBucket(accountId = accountId, bucket = bucket)
        }

        if (!attemptLoad) {
            InitialAvatar(
                identity = fallback.identity,
                colorKey = fallback.colorKey,
                backgroundColor = fallback.backgroundColor,
                foregroundColor = fallback.foregroundColor,
                modifier = Modifier.fillMaxSize(),
            )

            LaunchedEffect(accountId, missingRetryTick) {
                val delayMillis = avatarImageStore.missingRetryDelayMillis(accountId)
                    ?: return@LaunchedEffect
                delay(delayMillis)
                missingRetryTick++
            }
        } else {
            val context = LocalPlatformContext.current
            // A new generation keeps what the surface already shows until it is ready, or the
            // previous generation's variant on a surface composed meanwhile, so a replaced photo
            // swaps in place instead of flashing the letter avatar.
            val request = remember(accountId, generation, bucket) {
                avatarPhotoRequestConfiguration(accountId = accountId, bucket = bucket)
                    .toImageRequestBuilder(context = context)
                    .memoryCacheKey(
                        avatarMemoryCacheKey(
                            accountId = accountId,
                            generation = generation,
                            bucket = bucket,
                        ),
                    )
                    .placeholderMemoryCacheKey(
                        avatarPlaceholderMemoryCacheKey(
                            accountId = accountId,
                            generation = generation,
                            bucket = bucket,
                        ),
                    )
                    .useExistingImageAsPlaceholder(true)
                    .build()
            }

            // Keyed so a reused slot never carries another gambler's photo as placeholder.
            key(accountId) {
                SubcomposeAsyncImage(
                    model = request,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    onState = { state ->
                        when (state) {
                            is AsyncImagePainter.State.Success -> {
                                avatarImageStore.clearMissing(accountId = accountId)
                                // A load that reached the origin reported its own answer; only one
                                // served from memory may be showing a photo nobody asked about lately.
                                if (state.result.dataSource == DataSource.MEMORY_CACHE) {
                                    avatarImageStore.revalidateIfStale(accountId = accountId)
                                }
                            }

                            is AsyncImagePainter.State.Error -> {
                                val throwable = state.result.throwable
                                if (throwable is HttpException &&
                                    throwable.response.code in MISSING_AVATAR_STATUS_CODES
                                ) {
                                    avatarImageStore.recordMissing(accountId = accountId)
                                }
                            }

                            else -> Unit
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    val state by painter.state.collectAsStateWithLifecycle()
                    val showsPhoto = state is AsyncImagePainter.State.Success ||
                        (state is AsyncImagePainter.State.Loading && state.painter != null)
                    if (showsPhoto) {
                        SubcomposeAsyncImageContent()
                    } else {
                        InitialAvatar(
                            identity = fallback.identity,
                            colorKey = fallback.colorKey,
                            backgroundColor = fallback.backgroundColor,
                            foregroundColor = fallback.foregroundColor,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

internal data class AvatarPhotoRequestConfiguration(
    val url: String,
    val cacheControl: String,
    val diskCacheKey: String,
    val sizePx: Int,
)

/** Everything a surface's load and a revalidation share; callers add their memory policy. */
internal fun AvatarPhotoRequestConfiguration.toImageRequestBuilder(
    context: PlatformContext,
): ImageRequest.Builder =
    ImageRequest.Builder(context)
        .data(url)
        .httpHeaders(NetworkHeaders.Builder().set("Cache-Control", cacheControl).build())
        .diskCacheKey(diskCacheKey)
        .size(Size(sizePx, sizePx))
        .precision(Precision.INEXACT)

internal fun avatarPhotoRequestConfiguration(
    accountId: String,
    bucket: Int,
): AvatarPhotoRequestConfiguration {
    val url = AvatarUrl.of(accountId)
    return AvatarPhotoRequestConfiguration(
        url = url,
        // Never `no-cache`: Coil's `CacheControlCacheStrategy` (like OkHttp's) answers it with a
        // plain GET before it reaches the `If-None-Match` branch, re-downloading the unchanged
        // JPEG on every memory miss. `max-age=0` keeps revalidation mandatory — also for an
        // object stored without the bucket's `max-age=0, must-revalidate` — and yields a 304.
        cacheControl = "max-age=0",
        diskCacheKey = url,
        sizePx = bucket,
    )
}

private val MISSING_AVATAR_STATUS_CODES = setOf(403, 404)
