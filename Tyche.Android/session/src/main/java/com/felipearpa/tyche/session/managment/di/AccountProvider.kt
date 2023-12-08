package com.felipearpa.tyche.session.managment.di

import com.felipearpa.tyche.session.AccountStorage
import com.felipearpa.tyche.session.Auth
import com.felipearpa.tyche.session.managment.application.CreateAccountUseCase
import com.felipearpa.tyche.session.managment.domain.AccountRemoteDataSource
import com.felipearpa.tyche.session.managment.domain.AccountRepository
import com.felipearpa.tyche.session.managment.infrastructure.AccountFirebaseDataSource
import com.felipearpa.tyche.session.managment.infrastructure.AccountRemoteRepository
import com.google.firebase.auth.FirebaseAuth
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
        accountStorage: AccountStorage
    ): CreateAccountUseCase =
        CreateAccountUseCase(
            accountRepository = accountRepository,
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
    fun provideAccountRemoteDataSource(@Auth retrofit: Retrofit): AccountRemoteDataSource =
        retrofit.create(AccountRemoteDataSource::class.java)

    @Provides
    @Singleton
    fun provideAccountFirebaseDataSource(firebaseAuth: FirebaseAuth) =
        AccountFirebaseDataSource(firebaseAuth = firebaseAuth)
}