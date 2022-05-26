package com.pipel.tyche.pool.di

import com.pipel.tyche.pool.data.PoolRepository
import com.pipel.tyche.pool.useCase.DefaultFindPoolsUseCase
import com.pipel.tyche.pool.useCase.FindPoolsUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface PoolUseCaseModule {

    @Binds
    @Singleton
    fun provideFindPoolsUseCase(impl: DefaultFindPoolsUseCase): FindPoolsUseCase

}

@Module
@InstallIn(SingletonComponent::class)
object PoolRepositoryModule {

    @Provides
    @Singleton
    fun providePoolRepository(retrofit: Retrofit): PoolRepository =
        retrofit.create(PoolRepository::class.java)

}