package com.pipel.tyche.poolLayout.di

import com.pipel.tyche.poolLayout.data.PoolLayoutRepository
import com.pipel.tyche.poolLayout.useCase.DefaultFindPoolsLayoutsUseCase
import com.pipel.tyche.poolLayout.useCase.FindPoolsLayoutsUseCase
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
    fun provideFindPoolsLayoutsUseCase(impl: DefaultFindPoolsLayoutsUseCase): FindPoolsLayoutsUseCase

}

@Module
@InstallIn(SingletonComponent::class)
object PoolLayoutRepositoryModule {

    @Provides
    @Singleton
    fun providePoolLayoutRepository(retrofit: Retrofit): PoolLayoutRepository =
        retrofit.create(PoolLayoutRepository::class.java)

}