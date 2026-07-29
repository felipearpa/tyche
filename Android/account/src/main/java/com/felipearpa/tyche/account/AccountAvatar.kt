package com.felipearpa.tyche.account

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AccountAvatarFallback(
    val identity: String,
    val colorKey: String = identity,
    val backgroundColor: Color? = null,
    val foregroundColor: Color? = null,
)

/**
 * Bumped after the signed-in account's avatar changes, so navigation chrome
 * showing the photo refetches instead of waiting for a fresh composition.
 */
object AvatarVersion {
    private val _value = MutableStateFlow(0)
    val value: StateFlow<Int> = _value.asStateFlow()

    fun bump() {
        _value.value += 1
    }
}

/**
 * The account's avatar photo with the letter avatar as fallback — the single composable
 * behind every surface that shows the signed-in gambler. The request revalidates against
 * the fixed URL so a photo replaced elsewhere is picked up instead of a superseded cached
 * copy; the memory-cache key carries [AvatarVersion] so an in-session upload shows at once.
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

    val version by AvatarVersion.value.collectAsStateWithLifecycle()
    val context = LocalPlatformContext.current
    val request = remember(accountId, version) {
        val configuration = avatarPhotoRequestConfiguration(accountId, version)
        ImageRequest.Builder(context)
            .data(configuration.url)
            .httpHeaders(
                NetworkHeaders.Builder()
                    .set("Cache-Control", configuration.cacheControl)
                    .build(),
            )
            .memoryCacheKey(configuration.memoryCacheKey)
            .diskCacheKey(configuration.diskCacheKey)
            .build()
    }

    SubcomposeAsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    ) {
        val state by painter.state.collectAsStateWithLifecycle()
        if (state is AsyncImagePainter.State.Success) {
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

internal data class AvatarPhotoRequestConfiguration(
    val url: String,
    val cacheControl: String,
    val memoryCacheKey: String,
    val diskCacheKey: String,
)

internal fun avatarPhotoRequestConfiguration(
    accountId: String,
    version: Int,
): AvatarPhotoRequestConfiguration {
    val url = AvatarUrl.of(accountId)
    return AvatarPhotoRequestConfiguration(
        url = url,
        cacheControl = "no-cache",
        memoryCacheKey = "avatar#$accountId#$version",
        diskCacheKey = url,
    )
}
