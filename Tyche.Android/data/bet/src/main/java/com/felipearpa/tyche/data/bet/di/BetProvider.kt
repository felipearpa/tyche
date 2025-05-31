package com.felipearpa.tyche.data.bet.di

import com.felipearpa.tyche.data.bet.application.GetFinishedPoolGamblerBetsUseCase
import com.felipearpa.tyche.data.bet.application.GetPendingPoolGamblerBetsUseCase
import com.felipearpa.tyche.data.bet.domain.PoolGamblerBetRemoteDataSource
import com.felipearpa.tyche.data.bet.domain.PoolGamblerBetRepository
import com.felipearpa.tyche.data.bet.infrastructure.PoolGamblerBetRemoteRepository
import com.felipearpa.tyche.session.Auth
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
    fun provideGetPendingPoolGamblerBetsUseCase(poolGamblerBetRepository: PoolGamblerBetRepository) =
        GetPendingPoolGamblerBetsUseCase(poolGamblerBetRepository = poolGamblerBetRepository)

    @Provides
    fun provideGetFinishedPoolGamblerBetsUseCase(poolGamblerBetRepository: PoolGamblerBetRepository) =
        GetFinishedPoolGamblerBetsUseCase(poolGamblerBetRepository = poolGamblerBetRepository)
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
    fun providePoolGamblerScoreRemoteDataSource(@Auth retrofit: Retrofit): PoolGamblerBetRemoteDataSource =
        retrofit.create(PoolGamblerBetRemoteDataSource::class.java)
}
