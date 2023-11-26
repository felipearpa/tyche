package com.felipearpa.tyche.core.di

import com.felipearpa.tyche.core.network.NetworkExceptionHandler
import com.felipearpa.tyche.core.network.RetrofitExceptionHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface CoreProvider {
    @Binds
    @Singleton
    fun provideNetworkExceptionHandler(impl: RetrofitExceptionHandler): NetworkExceptionHandler
}