package com.felipearpa.tyche.session.authentication.di

import com.felipearpa.tyche.session.AccountStorage
import com.felipearpa.tyche.session.Auth
import com.felipearpa.tyche.session.authentication.application.LogOutUseCase
import com.felipearpa.tyche.session.authentication.application.SendSignInLinkToEmailUseCase
import com.felipearpa.tyche.session.authentication.application.SignInWithEmailLinkUseCase
import com.felipearpa.tyche.session.authentication.domain.AuthenticationExternalDataSource
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
    fun provideSendSignInLinkToEmailUseCase(authenticationRepository: AuthenticationRepository) =
        SendSignInLinkToEmailUseCase(authenticationRepository = authenticationRepository)

    @Provides
    @Singleton
    fun provideSignInWithEmailLinkUseCase(
        authenticationRepository: AuthenticationRepository,
        accountStorage: AccountStorage
    ) =
        SignInWithEmailLinkUseCase(
            authenticationRepository = authenticationRepository,
            accountStorage = accountStorage
        )

    @Provides
    @Singleton
    fun provideLogoutUseCase(
        authenticationRepository: AuthenticationRepository,
        accountStorage: AccountStorage
    ) = LogOutUseCase(
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
    fun provideAuthenticationExternalDataSource(firebaseAuth: FirebaseAuth): AuthenticationExternalDataSource =
        AuthenticationFirebaseDataSource(firebaseAuth = firebaseAuth)
}