package com.felipearpa.session.login.di

import com.felipearpa.session.AccountStorage
import com.felipearpa.session.AuthStorage
import com.felipearpa.session.login.application.LoginUseCase
import com.felipearpa.session.login.domain.LoginRemoteDataSource
import com.felipearpa.session.login.domain.LoginRepository
import com.felipearpa.session.login.infrastructure.LoginRemoteRepository
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