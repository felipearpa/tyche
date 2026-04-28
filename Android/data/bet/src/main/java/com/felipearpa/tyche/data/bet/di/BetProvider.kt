package com.felipearpa.tyche.data.bet.di

import com.felipearpa.tyche.core.network.HttpClientQualifier
import com.felipearpa.tyche.data.bet.application.PlaceBet
import com.felipearpa.tyche.data.bet.application.GetFinishedPoolGamblerBets
import com.felipearpa.tyche.data.bet.application.GetLivePoolGamblerBets
import com.felipearpa.tyche.data.bet.application.GetPendingPoolGamblerBets
import com.felipearpa.tyche.data.bet.domain.PoolGamblerBetDataSource
import com.felipearpa.tyche.data.bet.domain.PoolGamblerBetRepository
import com.felipearpa.tyche.data.bet.infrastructure.PoolGamblerBetRemoteKtorDataSource
import com.felipearpa.tyche.data.bet.infrastructure.PoolGamblerBetRemoteRepository
import io.ktor.client.HttpClient
import org.koin.core.qualifier.named
import org.koin.dsl.module

val betDataModule = module {
    factory { GetPendingPoolGamblerBets(poolGamblerBetRepository = get()) }
    factory { GetFinishedPoolGamblerBets(poolGamblerBetRepository = get()) }
    factory { GetLivePoolGamblerBets(poolGamblerBetRepository = get()) }
    factory { PlaceBet(poolGamblerBetRepository = get()) }

    factory<PoolGamblerBetRepository> {
        PoolGamblerBetRemoteRepository(
            poolGamblerBetDataSource = get(),
            networkExceptionHandler = get(),
        )
    }

    factory<PoolGamblerBetDataSource> {
        PoolGamblerBetRemoteKtorDataSource(httpClient = get<HttpClient>(named(HttpClientQualifier.Auth)))
    }
}
