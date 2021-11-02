package com.pipel.tyche.data.di

import com.pipel.tyche.data.PoolLayoutRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun providePoolLayoutRepository(retrofit: Retrofit): PoolLayoutRepository =
        retrofit.create(PoolLayoutRepository::class.java)

}