package com.felipearpa.tyche

import android.app.Application
import android.content.ComponentCallbacks2
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.memory.MemoryCache
import coil3.network.ConnectivityChecker
import coil3.network.NetworkFetcher
import coil3.network.cachecontrol.CacheControlCacheStrategy
import coil3.network.okhttp.asNetworkClient
import com.felipearpa.tyche.account.AVATAR_MEMORY_BUDGET_BYTES
import com.felipearpa.tyche.account.AvatarRequestDeduplicationInterceptor
import com.felipearpa.tyche.account.SerialConcurrentRequestStrategy
import com.felipearpa.tyche.account.di.accountViewModelModule
import com.felipearpa.tyche.account.di.avatarImageStoreModule
import com.felipearpa.tyche.account.reportingAvatarExchanges
import com.felipearpa.tyche.bet.di.betViewModelModule
import com.felipearpa.tyche.core.di.coreModule
import com.felipearpa.tyche.data.bet.di.betDataModule
import com.felipearpa.tyche.data.pool.di.poolDataModule
import com.felipearpa.tyche.di.appModule
import com.felipearpa.tyche.network.di.networkModule
import com.felipearpa.tyche.pool.di.poolViewModelModule
import com.felipearpa.tyche.poolhome.di.poolHomeViewModelModule
import com.felipearpa.tyche.poolscore.di.poolScoreViewModelModule
import com.felipearpa.tyche.session.authentication.di.authenticationModule
import com.felipearpa.tyche.session.avatar.di.avatarModule
import com.felipearpa.tyche.session.di.sessionModule
import okhttp3.OkHttpClient
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class TycheApplication : Application(), SingletonImageLoader.Factory {

    // Coil ignores HTTP cache headers by default; avatars live at a fixed URL and rely on
    // `Cache-Control: must-revalidate`, so the loader gets the header-respecting strategy.
    // Decoded memory is bounded by the avatar budget and concurrent avatar requests sharing
    // a memory key coalesce into one load. Fetches of one URL at different sizes (toolbar and
    // drawer on cold start, every surface when a photo is replaced) run one after another:
    // raced, all but one lose the disk entry's editor and Coil answers their empty 304 with a
    // second, full download. The network client reports each origin answer to the avatar
    // store, which is how the store learns which remote photo its decoded variants show.
    // Coil's connectivity check is bypassed because it turns an offline fetch into a synthetic
    // 504, the response that makes Coil 3.4.0 leak its disk snapshot; offline now fails in
    // OkHttp, before Coil has anything to leak.
    @OptIn(ExperimentalCoilApi::class)
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(
                    NetworkFetcher.Factory(
                        networkClient = {
                            OkHttpClient().asNetworkClient()
                                .reportingAvatarExchanges(avatarImageStore = get())
                        },
                        cacheStrategy = { CacheControlCacheStrategy() },
                        connectivityChecker = { ConnectivityChecker.ONLINE },
                        concurrentRequestStrategy = { SerialConcurrentRequestStrategy() },
                    ),
                )
                add(AvatarRequestDeduplicationInterceptor())
            }
            .memoryCache {
                // The 16 MiB avatar budget is a deliberate app-wide cap. Bundled match flags
                // (`FlagImage`) share this loader, so avatars and flags together stay within
                // it — decoded avatars can never exceed the budget, only fall short of it.
                MemoryCache.Builder()
                    .maxSizeBytes(AVATAR_MEMORY_BUDGET_BYTES)
                    .build()
            }
            .build()

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Decoded images are recoverable from the disk/HTTP cache, so under memory pressure
        // the whole memory cache is released.
        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            SingletonImageLoader.get(this).memoryCache?.clear()
        }
    }

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@TycheApplication)
            modules(
                appModule,
                coreModule,
                networkModule,
                sessionModule,
                authenticationModule,
                avatarModule,
                avatarImageStoreModule,
                poolDataModule,
                betDataModule,
                poolViewModelModule,
                poolHomeViewModelModule,
                poolScoreViewModelModule,
                betViewModelModule,
                accountViewModelModule,
            )
        }
    }
}
