package com.felipearpa.data.pool.di

import com.felipearpa.data.pool.application.GetPoolGamblerScoresByGamblerUseCase
import com.felipearpa.data.pool.application.GetPoolGamblerScoresByPoolUseCase
import com.felipearpa.data.pool.application.GetPoolUseCase
import com.felipearpa.data.pool.domain.PoolGamblerScoreRemoteDataSource
import com.felipearpa.data.pool.domain.PoolGamblerScoreRepository
import com.felipearpa.data.pool.domain.PoolRemoteDataSource
import com.felipearpa.data.pool.domain.PoolRepository
import com.felipearpa.data.pool.infrastructure.PoolGamblerScoreRemoteRepository
import com.felipearpa.data.pool.infrastructure.PoolRemoteRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit

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
    fun provideGetPoolUseCase(poolRepository: PoolRepository) =
        GetPoolUseCase(poolRepository = poolRepository)
}

@Module
@InstallIn(SingletonComponent::class)
internal interface PoolRepositoryProvider {
    @Binds
    fun providePoolGamblerScoreRepository(impl: PoolGamblerScoreRemoteRepository): PoolGamblerScoreRepository

    @Binds
    fun providePoolRepository(impl: PoolRemoteRepository): PoolRepository
}

@Module
@InstallIn(SingletonComponent::class)
internal object PoolDataSourceProvider {
    @Provides
    fun providePoolGamblerScoreRemoteDataSource(retrofit: Retrofit): PoolGamblerScoreRemoteDataSource =
        retrofit.create(PoolGamblerScoreRemoteDataSource::class.java)

    @Provides
    fun providePoolRemoteDataSource(retrofit: Retrofit): PoolRemoteDataSource =
        retrofit.create(PoolRemoteDataSource::class.java)
}
