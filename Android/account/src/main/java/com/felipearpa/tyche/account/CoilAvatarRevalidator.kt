package com.felipearpa.tyche.account

import android.content.Context
import coil3.SingletonImageLoader
import coil3.request.CachePolicy

/**
 * Revalidates through the singleton Coil loader with the memory cache switched off, so the
 * load reaches the network fetcher and Coil sends the conditional request its disk entry
 * allows. The decoded result is discarded; the request carries no memory key, so it is never
 * coalesced with a surface's own load.
 */
internal class CoilAvatarRevalidator(private val context: Context) : AvatarRevalidator {

    override suspend fun revalidate(accountId: String, bucket: Int) {
        val request = avatarPhotoRequestConfiguration(accountId = accountId, bucket = bucket)
            .toImageRequestBuilder(context = context)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .build()

        SingletonImageLoader.get(context).execute(request)
    }
}
