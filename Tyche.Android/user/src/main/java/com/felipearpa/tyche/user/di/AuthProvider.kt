package com.felipearpa.tyche.user.di

import com.felipearpa.tyche.user.AuthInterceptor
import com.felipearpa.tyche.user.LoginStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthProvider {

    @Provides
    @Singleton
    fun provideAuthInterceptor(loginStorage: LoginStorage) =
        AuthInterceptor(loginStorage = loginStorage)
}