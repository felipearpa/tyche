package com.felipearpa.tyche

import android.app.Application
import com.felipearpa.tyche.account.di.accountViewModelModule
import com.felipearpa.tyche.bet.di.betViewModelModule
import com.felipearpa.tyche.config.di.configModule
import com.felipearpa.tyche.core.di.coreModule
import com.felipearpa.tyche.data.bet.di.betDataModule
import com.felipearpa.tyche.data.pool.di.poolDataModule
import com.felipearpa.tyche.network.di.networkModule
import com.felipearpa.tyche.pool.di.poolViewModelModule
import com.felipearpa.tyche.poolhome.di.poolHomeViewModelModule
import com.felipearpa.tyche.poolscore.di.poolScoreViewModelModule
import com.felipearpa.tyche.session.authentication.di.authenticationModule
import com.felipearpa.tyche.session.di.sessionModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class TycheApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@TycheApplication)
            modules(
                configModule,
                coreModule,
                networkModule,
                sessionModule,
                authenticationModule,
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
