package com.felipearpa.data.bet.di

import com.felipearpa.data.bet.application.GetPoolGamblerBetsUseCase
import com.felipearpa.data.bet.domain.PoolGamblerBetRemoteDataSource
import com.felipearpa.data.bet.domain.PoolGamblerBetRepository
import com.felipearpa.data.bet.infrastructure.PoolGamblerBetRemoteRepository
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
    fun provideGetPoolGamblerBetsUseCase(poolGamblerBetRepository: PoolGamblerBetRepository) =
        GetPoolGamblerBetsUseCase(poolGamblerBetRepository = poolGamblerBetRepository)
}

@Module
@InstallIn(SingletonComponent::class)
internal interface PoolRepositoryProvider {
    @Binds
    fun providePoolGamblerScoreRepository(impl: PoolGamblerBetRemoteRepository): PoolGamblerBetRepository
}

@Module
@InstallIn(SingletonComponent::class)
internal object PoolDataSourceProvider {
    @Provides
    fun providePoolGamblerScoreRemoteDataSource(retrofit: Retrofit): PoolGamblerBetRemoteDataSource =
        retrofit.create(PoolGamblerBetRemoteDataSource::class.java)
}
