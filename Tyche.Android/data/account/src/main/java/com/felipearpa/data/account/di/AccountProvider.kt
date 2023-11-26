package com.felipearpa.data.account.di

import android.content.Context
import com.felipearpa.data.account.AccountStorage
import com.felipearpa.data.account.AccountStorageInKeyStore
import com.felipearpa.data.account.AuthInterceptor
import com.felipearpa.data.account.AuthStorage
import com.felipearpa.data.account.AuthStorageInKeyStore
import com.felipearpa.tyche.core.data.StorageInKeyStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val AUTH_FILE_NAME = "token"
private const val AUTH_KEY = "token"

private const val ACCOUNT_FILE_NAME = "account"
private const val ACCOUNT_KEY = "account"

@Module
@InstallIn(SingletonComponent::class)
object AuthProvider {
    @Provides
    @Singleton
    fun provideAuthStorage(@ApplicationContext context: Context): AuthStorage =
        AuthStorageInKeyStore(
            StorageInKeyStore(
                context = context,
                key = AUTH_KEY,
                filename = AUTH_FILE_NAME
            )
        )

    @Provides
    @Singleton
    fun provideAuthInterceptor(loginStorage: AuthStorage) =
        AuthInterceptor(loginStorage = loginStorage)

    @Provides
    @Singleton
    fun provideAccountStorage(@ApplicationContext context: Context): AccountStorage =
        AccountStorageInKeyStore(
            StorageInKeyStore(
                context = context,
                key = ACCOUNT_KEY,
                filename = ACCOUNT_FILE_NAME
            )
        )
}