package com.felipearpa.data.account.managment.di

import com.felipearpa.data.account.AccountStorage
import com.felipearpa.data.account.AuthStorage
import com.felipearpa.data.account.managment.application.CreateAccountUseCase
import com.felipearpa.data.account.managment.domain.AccountRemoteDataSource
import com.felipearpa.data.account.managment.domain.AccountRepository
import com.felipearpa.data.account.managment.infrastructure.AccountRemoteRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object UseCaseProvider {
    @Provides
    @Singleton
    fun provideCreateAccount(
        accountRepository: AccountRepository,
        authStorage: AuthStorage,
        accountStorage: AccountStorage
    ): CreateAccountUseCase =
        CreateAccountUseCase(
            accountRepository = accountRepository,
            loginStorage = authStorage,
            accountStorage = accountStorage
        )
}

@Module
@InstallIn(SingletonComponent::class)
internal interface AccountRepositoryProvider {
    @Binds
    @Singleton
    fun provideAccountRepository(impl: AccountRemoteRepository): AccountRepository
}

@Module
@InstallIn(SingletonComponent::class)
internal object AccountDataSourceProvider {
    @Provides
    @Singleton
    fun provideAccountRemoteDataSource(retrofit: Retrofit): AccountRemoteDataSource =
        retrofit.create(AccountRemoteDataSource::class.java)
}