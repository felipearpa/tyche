package com.pipel.tyche.poolLayout.di

import com.pipel.tyche.poolLayout.data.PoolLayoutRepository
import com.pipel.tyche.poolLayout.useCase.DefaultActiveFindActivePoolsLayoutsUseCase
import com.pipel.tyche.poolLayout.useCase.FindActivePoolsLayoutsUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface PoolLayoutUseCaseModule {

    @Binds
    @Singleton
    fun provideFindActivePoolsLayoutsUseCase(impl: DefaultActiveFindActivePoolsLayoutsUseCase): FindActivePoolsLayoutsUseCase

}

@Module
@InstallIn(SingletonComponent::class)
object PoolLayoutRepositoryModule {

    @Provides
    @Singleton
    fun providePoolLayoutRepository(retrofit: Retrofit): PoolLayoutRepository =
        retrofit.create(PoolLayoutRepository::class.java)

}