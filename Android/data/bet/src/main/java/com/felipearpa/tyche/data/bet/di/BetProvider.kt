package com.felipearpa.tyche.data.bet.di

import com.felipearpa.tyche.core.network.HttpClientQualifier
import com.felipearpa.tyche.data.bet.application.BetUseCase
import com.felipearpa.tyche.data.bet.application.GetFinishedPoolGamblerBetsUseCase
import com.felipearpa.tyche.data.bet.application.GetPendingPoolGamblerBetsUseCase
import com.felipearpa.tyche.data.bet.domain.PoolGamblerBetDataSource
import com.felipearpa.tyche.data.bet.domain.PoolGamblerBetRepository
import com.felipearpa.tyche.data.bet.infrastructure.PoolGamblerBetRemoteKtorDataSource
import com.felipearpa.tyche.data.bet.infrastructure.PoolGamblerBetRemoteRepository
import io.ktor.client.HttpClient
import org.koin.core.qualifier.named
import org.koin.dsl.module

val betDataModule = module {
    factory { GetPendingPoolGamblerBetsUseCase(poolGamblerBetRepository = get()) }
    factory { GetFinishedPoolGamblerBetsUseCase(poolGamblerBetRepository = get()) }
    factory { BetUseCase(poolGamblerBetRepository = get()) }

    factory<PoolGamblerBetRepository> {
        PoolGamblerBetRemoteRepository(
            poolGamblerBetRemoteDataSource = get(),
            networkExceptionHandler = get(),
        )
    }

    factory<PoolGamblerBetDataSource> {
        PoolGamblerBetRemoteKtorDataSource(httpClient = get<HttpClient>(named(HttpClientQualifier.Auth)))
    }
}
