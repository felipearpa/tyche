package com.pipel.tyche.poolGambler.di

import com.pipel.tyche.poolGambler.data.PoolGamblerRepository
import com.pipel.tyche.poolGambler.useCase.DefaultFindPoolsGamblersUseCase
import com.pipel.tyche.poolGambler.useCase.FindPoolsGamblersUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface PoolGamblerUseCaseModule {

    @Binds
    @Singleton
    fun provideFindPoolsGamblersUseCase(impl: DefaultFindPoolsGamblersUseCase): FindPoolsGamblersUseCase

}

@Module
@InstallIn(SingletonComponent::class)
object PoolGamblerRepositoryModule {

    @Provides
    @Singleton
    fun providePoolGamblerRepository(retrofit: Retrofit): PoolGamblerRepository =
        retrofit.create(PoolGamblerRepository::class.java)

}