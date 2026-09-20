package com.felipearpa.tyche.account

import android.content.Context
import coil3.Image
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache

internal class CoilAvatarMemoryCacheGateway(private val context: Context) :
    AvatarMemoryCacheGateway {

    override fun write(key: String, image: Image) {
        SingletonImageLoader.get(context).memoryCache?.set(
            MemoryCache.Key(key),
            MemoryCache.Value(image),
        )
    }

    override fun remove(key: String) {
        SingletonImageLoader.get(context).memoryCache?.remove(MemoryCache.Key(key))
    }
}
