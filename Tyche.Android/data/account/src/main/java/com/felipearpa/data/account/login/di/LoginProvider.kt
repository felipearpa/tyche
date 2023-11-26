package com.felipearpa.data.account.login.di

import com.felipearpa.data.account.AccountStorage
import com.felipearpa.data.account.AuthStorage
import com.felipearpa.data.account.login.application.LoginUseCase
import com.felipearpa.data.account.login.domain.LoginRemoteDataSource
import com.felipearpa.data.account.login.domain.LoginRepository
import com.felipearpa.data.account.login.infrastructure.LoginRemoteRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object LoginUseCaseProvider {
    @Provides
    @Singleton
    fun provideLoginUseCase(
        loginRepository: LoginRepository,
        authStorage: AuthStorage,
        accountStorage: AccountStorage
    ): LoginUseCase =
        LoginUseCase(
            loginRepository = loginRepository,
            authStorage = authStorage,
            accountStorage = accountStorage
        )
}

@Module
@InstallIn(SingletonComponent::class)
internal interface LoginRepositoryProvider {
    @Binds
    @Singleton
    fun provideLoginRepository(impl: LoginRemoteRepository): LoginRepository
}

@Module
@InstallIn(SingletonComponent::class)
internal object LoginDataSourceProvider {
    @Provides
    @Singleton
    fun provideLoginNetworkDataSource(retrofit: Retrofit): LoginRemoteDataSource =
        retrofit.create(LoginRemoteDataSource::class.java)
}