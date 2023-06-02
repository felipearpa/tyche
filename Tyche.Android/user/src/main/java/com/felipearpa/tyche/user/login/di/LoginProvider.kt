package com.felipearpa.tyche.user.login.di

import android.content.Context
import com.felipearpa.tyche.user.LoginStorage
import com.felipearpa.tyche.user.login.application.LoginUseCase
import com.felipearpa.tyche.user.login.domain.LoginRemoteDataSource
import com.felipearpa.tyche.user.login.domain.LoginRepository
import com.felipearpa.tyche.user.login.infrastructure.LoginRemoteRepository
import com.felipearpa.tyche.user.login.infrastructure.LoginStorageInKeyStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LoginCommandQueryProvider {

    @Provides
    @Singleton
    fun provideLoginCommandHandler(
        loginRepository: LoginRepository,
        loginStorage: LoginStorage
    ): LoginUseCase =
        LoginUseCase(loginRepository = loginRepository, loginStorage = loginStorage)
}

@Module
@InstallIn(SingletonComponent::class)
interface LoginRepositoryProvider {

    @Binds
    @Singleton
    fun provideLoginRepository(impl: LoginRemoteRepository): LoginRepository
}

@Module
@InstallIn(SingletonComponent::class)
object LoginDataSourceProvider {

    @Provides
    @Singleton
    fun provideLoginNetworkDataSource(retrofit: Retrofit): LoginRemoteDataSource =
        retrofit.create(LoginRemoteDataSource::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
object LoginStorageProvider {

    @Provides
    @Singleton
    fun provideLoginStorage(@ApplicationContext app: Context): LoginStorage =
        LoginStorageInKeyStore(context = app)
}