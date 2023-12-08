package com.felipearpa.tyche.session.authentication.di

import com.felipearpa.tyche.session.AccountStorage
import com.felipearpa.tyche.session.Auth
import com.felipearpa.tyche.session.authentication.application.LoginUseCase
import com.felipearpa.tyche.session.authentication.application.LogoutUseCase
import com.felipearpa.tyche.session.authentication.domain.AuthenticationRemoteDataSource
import com.felipearpa.tyche.session.authentication.domain.AuthenticationRepository
import com.felipearpa.tyche.session.authentication.infrastructure.AuthenticationFirebaseDataSource
import com.felipearpa.tyche.session.authentication.infrastructure.AuthenticationRemoteRepository
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
internal object AuthenticationUseCaseProvider {
    @Provides
    @Singleton
    fun provideLoginUseCase(
        authenticationRepository: AuthenticationRepository,
        accountStorage: AccountStorage
    ): LoginUseCase =
        LoginUseCase(
            authenticationRepository = authenticationRepository,
            accountStorage = accountStorage
        )

    @Provides
    @Singleton
    fun provideLogoutUseCase(
        authenticationRepository: AuthenticationRepository,
        accountStorage: AccountStorage
    ): LogoutUseCase =
        LogoutUseCase(
            authenticationRepository = authenticationRepository,
            accountStorage = accountStorage
        )
}

@Module
@InstallIn(SingletonComponent::class)
internal interface AuthenticationRepositoryProvider {
    @Binds
    @Singleton
    fun provideAuthenticationRepository(impl: AuthenticationRemoteRepository): AuthenticationRepository
}

@Module
@InstallIn(SingletonComponent::class)
internal object LoginDataSourceProvider {
    @Provides
    @Singleton
    fun provideAuthenticationNetworkDataSource(@Auth retrofit: Retrofit): AuthenticationRemoteDataSource =
        retrofit.create(AuthenticationRemoteDataSource::class.java)

    @Provides
    @Singleton
    fun provideAuthenticationFirebaseDataSource(firebaseAuth: FirebaseAuth) =
        AuthenticationFirebaseDataSource(firebaseAuth = firebaseAuth)
}