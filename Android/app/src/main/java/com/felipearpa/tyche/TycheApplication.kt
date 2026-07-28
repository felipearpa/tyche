package com.felipearpa.tyche

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.cachecontrol.CacheControlCacheStrategy
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.felipearpa.tyche.account.di.accountViewModelModule
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
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class TycheApplication : Application(), SingletonImageLoader.Factory {

    // Coil ignores HTTP cache headers by default; avatars live at a fixed URL and rely on
    // `Cache-Control: must-revalidate`, so the loader gets the header-respecting strategy.
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(cacheStrategy = { CacheControlCacheStrategy() }))
            }
            .build()

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
