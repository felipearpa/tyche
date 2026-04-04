package com.felipearpa.tyche.data.pool.di

import com.felipearpa.tyche.data.pool.application.CreatePoolUseCase
import com.felipearpa.tyche.data.pool.application.GetOpenPoolLayoutsUseCase
import com.felipearpa.tyche.data.pool.application.GetPoolGamblerScoreUseCase
import com.felipearpa.tyche.data.pool.application.GetPoolGamblerScoresByGamblerUseCase
import com.felipearpa.tyche.data.pool.application.GetPoolGamblerScoresByPoolUseCase
import com.felipearpa.tyche.data.pool.application.GetPoolUseCase
import com.felipearpa.tyche.data.pool.application.JoinPoolUseCase
import com.felipearpa.tyche.data.pool.domain.PoolDataSource
import com.felipearpa.tyche.data.pool.domain.PoolGamblerScoreDataSource
import com.felipearpa.tyche.data.pool.domain.PoolGamblerScoreRepository
import com.felipearpa.tyche.data.pool.domain.PoolLayoutDataSource
import com.felipearpa.tyche.data.pool.domain.PoolLayoutRepository
import com.felipearpa.tyche.data.pool.domain.PoolRepository
import com.felipearpa.tyche.data.pool.infrastructure.PoolGamblerScoreRemoteKtorDataSource
import com.felipearpa.tyche.data.pool.infrastructure.PoolGamblerScoreRemoteRepository
import com.felipearpa.tyche.data.pool.infrastructure.PoolLayoutRemoteKtorDataSource
import com.felipearpa.tyche.data.pool.infrastructure.PoolLayoutRemoteRepository
import com.felipearpa.tyche.data.pool.infrastructure.PoolRemoteKtorDataSource
import com.felipearpa.tyche.data.pool.infrastructure.PoolRemoteRepository
import com.felipearpa.tyche.core.network.HttpClientQualifier
import io.ktor.client.HttpClient
import org.koin.core.qualifier.named
import org.koin.dsl.module

val poolDataModule = module {
    factory { GetPoolGamblerScoresByGamblerUseCase(poolGamblerScoreRepository = get()) }
    factory { GetPoolGamblerScoresByPoolUseCase(poolGamblerScoreRepository = get()) }
    factory { GetPoolGamblerScoreUseCase(poolGamblerScoreRepository = get()) }
    factory { GetOpenPoolLayoutsUseCase(poolLayoutRepository = get()) }
    factory { CreatePoolUseCase(poolRepository = get()) }
    factory { JoinPoolUseCase(poolRepository = get()) }
    factory { GetPoolUseCase(poolRepository = get()) }

    factory<PoolGamblerScoreRepository> {
        PoolGamblerScoreRemoteRepository(
            poolGamblerScoreDataSource = get(),
            networkExceptionHandler = get(),
        )
    }
    factory<PoolLayoutRepository> {
        PoolLayoutRemoteRepository(
            poolLayoutDataSource = get(),
            networkExceptionHandler = get(),
        )
    }
    factory<PoolRepository> {
        PoolRemoteRepository(
            poolDataSource = get(),
            networkExceptionHandler = get(),
        )
    }

    factory<PoolGamblerScoreDataSource> {
        PoolGamblerScoreRemoteKtorDataSource(httpClient = get<HttpClient>(named(HttpClientQualifier.Auth)))
    }
    factory<PoolLayoutDataSource> {
        PoolLayoutRemoteKtorDataSource(httpClient = get<HttpClient>(named(HttpClientQualifier.Auth)))
    }
    factory<PoolDataSource> {
        PoolRemoteKtorDataSource(httpClient = get<HttpClient>(named(HttpClientQualifier.Auth)))
    }
}
