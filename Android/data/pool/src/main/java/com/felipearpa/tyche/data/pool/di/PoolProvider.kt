package com.felipearpa.tyche.data.pool.di

import com.felipearpa.tyche.data.pool.application.CreatePoolUseCase
import com.felipearpa.tyche.data.pool.application.GetOpenPoolLayoutsUseCase
import com.felipearpa.tyche.data.pool.application.GetPoolGamblerScoreUseCase
import com.felipearpa.tyche.data.pool.application.GetPoolGamblerScoresByGamblerUseCase
import com.felipearpa.tyche.data.pool.application.GetPoolGamblerScoresByPoolUseCase
import com.felipearpa.tyche.data.pool.application.GetPoolUseCase
import com.felipearpa.tyche.data.pool.application.JoinPoolUseCase
import com.felipearpa.tyche.data.pool.domain.PoolGamblerScoreDataSource
import com.felipearpa.tyche.data.pool.domain.PoolGamblerScoreRepository
import com.felipearpa.tyche.data.pool.domain.PoolLayoutDataSource
import com.felipearpa.tyche.data.pool.domain.PoolLayoutRepository
import com.felipearpa.tyche.data.pool.domain.PoolDataSource
import com.felipearpa.tyche.data.pool.domain.PoolRepository
import com.felipearpa.tyche.data.pool.infrastructure.PoolGamblerScoreRemoteKtorDataSource
import com.felipearpa.tyche.data.pool.infrastructure.PoolGamblerScoreRemoteRepository
import com.felipearpa.tyche.data.pool.infrastructure.PoolLayoutRemoteKtorDataSource
import com.felipearpa.tyche.data.pool.infrastructure.PoolLayoutRemoteRepository
import com.felipearpa.tyche.data.pool.infrastructure.PoolRemoteKtorDataSource
import com.felipearpa.tyche.data.pool.infrastructure.PoolRemoteRepository
import com.felipearpa.tyche.session.Auth
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient

@Module
@InstallIn(SingletonComponent::class)
internal object PoolUseCaseProvider {
    @Provides
    fun provideGetPoolGamblerScoresByGamblerUseCase(poolGamblerScoreRepository: PoolGamblerScoreRepository) =
        GetPoolGamblerScoresByGamblerUseCase(poolGamblerScoreRepository = poolGamblerScoreRepository)

    @Provides
    fun provideGetPoolGamblerScoresByPoolUseCase(poolGamblerScoreRepository: PoolGamblerScoreRepository) =
        GetPoolGamblerScoresByPoolUseCase(poolGamblerScoreRepository = poolGamblerScoreRepository)

    @Provides
    fun provideGetPoolGamblerScoreUseCase(poolGamblerScoreRepository: PoolGamblerScoreRepository) =
        GetPoolGamblerScoreUseCase(poolGamblerScoreRepository = poolGamblerScoreRepository)

    @Provides
    fun provideGetOpenPoolLayoutsUseCase(poolLayoutRepository: PoolLayoutRepository) =
        GetOpenPoolLayoutsUseCase(poolLayoutRepository = poolLayoutRepository)

    @Provides
    fun provideCreatePoolUseCase(poolRepository: PoolRepository) =
        CreatePoolUseCase(poolRepository = poolRepository)

    @Provides
    fun provideJoinPoolUseCase(poolRepository: PoolRepository) =
        JoinPoolUseCase(poolRepository = poolRepository)

    @Provides
    fun provideGetPoolUseCase(poolRepository: PoolRepository) =
        GetPoolUseCase(poolRepository = poolRepository)
}

@Module
@InstallIn(SingletonComponent::class)
internal interface PoolRepositoryProvider {
    @Binds
    fun providePoolGamblerScoreRepository(impl: PoolGamblerScoreRemoteRepository): PoolGamblerScoreRepository

    @Binds
    fun providePoolLayoutRepository(impl: PoolLayoutRemoteRepository): PoolLayoutRepository

    @Binds
    fun providePoolRepository(impl: PoolRemoteRepository): PoolRepository
}

@Module
@InstallIn(SingletonComponent::class)
internal object PoolDataSourceProvider {
    @Provides
    fun providePoolGamblerScoreDataSource(@Auth httpClient: HttpClient): PoolGamblerScoreDataSource =
        PoolGamblerScoreRemoteKtorDataSource(httpClient = httpClient)

    @Provides
    fun providePoolLayoutDataSource(@Auth httpClient: HttpClient): PoolLayoutDataSource =
        PoolLayoutRemoteKtorDataSource(httpClient = httpClient)

    @Provides
    fun providePoolDataSource(@Auth httpClient: HttpClient): PoolDataSource =
        PoolRemoteKtorDataSource(httpClient = httpClient)
}
