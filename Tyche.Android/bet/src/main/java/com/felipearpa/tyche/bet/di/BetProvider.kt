package com.felipearpa.tyche.bet.di

import com.felipearpa.tyche.bet.application.GetPoolGamblerBetsUseCase
import com.felipearpa.tyche.bet.domain.PoolGamblerBetRemoteDataSource
import com.felipearpa.tyche.bet.domain.PoolGamblerBetRepository
import com.felipearpa.tyche.bet.infrastructure.PoolGamblerBetRemoteRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object PoolUseCaseProvider {

    @Provides
    fun provideGetPoolGamblerBetsUseCase(poolGamblerBetRepository: PoolGamblerBetRepository) =
        GetPoolGamblerBetsUseCase(poolGamblerBetRepository = poolGamblerBetRepository)
}

@Module
@InstallIn(SingletonComponent::class)
interface PoolRepositoryProvider {

    @Binds
    fun providePoolGamblerScoreRepository(impl: PoolGamblerBetRemoteRepository): PoolGamblerBetRepository
}

@Module
@InstallIn(SingletonComponent::class)
object PoolDataSourceProvider {

    @Provides
    fun providePoolGamblerScoreRemoteDataSource(retrofit: Retrofit): PoolGamblerBetRemoteDataSource =
        retrofit.create(PoolGamblerBetRemoteDataSource::class.java)
}
