package com.felipearpa.tyche.data.pool.di

import com.felipearpa.tyche.data.pool.application.CreatePool
import com.felipearpa.tyche.data.pool.application.GetOpenPoolLayouts
import com.felipearpa.tyche.data.pool.application.GetPoolGamblerScore
import com.felipearpa.tyche.data.pool.application.GetPoolGamblerScoresByGambler
import com.felipearpa.tyche.data.pool.application.GetPoolGamblerScoresByPool
import com.felipearpa.tyche.data.pool.application.GetPool
import com.felipearpa.tyche.data.pool.application.JoinPool
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
    factory { GetPoolGamblerScoresByGambler(poolGamblerScoreRepository = get()) }
    factory { GetPoolGamblerScoresByPool(poolGamblerScoreRepository = get()) }
    factory { GetPoolGamblerScore(poolGamblerScoreRepository = get()) }
    factory { GetOpenPoolLayouts(poolLayoutRepository = get()) }
    factory { CreatePool(poolRepository = get()) }
    factory { JoinPool(poolRepository = get()) }
    factory { GetPool(poolRepository = get()) }

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
