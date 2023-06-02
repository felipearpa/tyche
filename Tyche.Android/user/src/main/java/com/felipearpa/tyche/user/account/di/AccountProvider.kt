package com.felipearpa.tyche.user.account.di

import com.felipearpa.tyche.user.LoginStorage
import com.felipearpa.tyche.user.account.application.CreateUserUseCase
import com.felipearpa.tyche.user.account.domain.AccountRemoteDataSource
import com.felipearpa.tyche.user.account.domain.AccountRepository
import com.felipearpa.tyche.user.account.infrastructure.AccountRemoteRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CommandHandlerProvider {

    @Provides
    @Singleton
    fun provideCreateUserCommandHandler(
        accountRepository: AccountRepository,
        loginStorage: LoginStorage
    ): CreateUserUseCase =
        CreateUserUseCase(accountRepository = accountRepository, loginStorage = loginStorage)
}

@Module
@InstallIn(SingletonComponent::class)
interface AccountRepositoryProvider {

    @Binds
    @Singleton
    fun provideUserRepository(impl: AccountRemoteRepository): AccountRepository

}

@Module
@InstallIn(SingletonComponent::class)
object AccountDataSourceProvider {

    @Provides
    @Singleton
    fun provideAccountRemoteDataSource(retrofit: Retrofit): AccountRemoteDataSource =
        retrofit.create(AccountRemoteDataSource::class.java)
}