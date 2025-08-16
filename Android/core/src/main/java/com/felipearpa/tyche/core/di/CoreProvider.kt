package com.felipearpa.tyche.core.di

import com.felipearpa.network.NetworkExceptionHandler
import com.felipearpa.network.retrofit.RetrofitExceptionHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object CoreProvider {
    @Provides
    fun provideNetworkExceptionHandler(): NetworkExceptionHandler =
        RetrofitExceptionHandler()
}
