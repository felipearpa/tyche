package com.felipearpa.tyche.session.authentication.di

import com.felipearpa.tyche.session.authentication.application.LogOutUseCase
import com.felipearpa.tyche.session.authentication.application.SendSignInLinkToEmailUseCase
import com.felipearpa.tyche.session.authentication.application.SignInWithEmailAndPasswordUseCase
import com.felipearpa.tyche.session.authentication.application.SignInWithEmailLinkUseCase
import com.felipearpa.tyche.session.authentication.domain.AuthenticationDataSource
import com.felipearpa.tyche.session.authentication.domain.AuthenticationExternalDataSource
import com.felipearpa.tyche.session.authentication.domain.AuthenticationRepository
import com.felipearpa.tyche.session.authentication.infrastructure.AuthenticationFirebaseDataSource
import com.felipearpa.tyche.session.authentication.infrastructure.AuthenticationRemoteKtorDataSource
import com.felipearpa.tyche.session.authentication.infrastructure.AuthenticationRemoteRepository
import com.felipearpa.tyche.core.network.HttpClientQualifier
import io.ktor.client.HttpClient
import org.koin.core.qualifier.named
import org.koin.dsl.module

val authenticationModule = module {
    factory { SendSignInLinkToEmailUseCase(authenticationRepository = get()) }
    factory { SignInWithEmailLinkUseCase(authenticationRepository = get(), accountStorage = get()) }
    factory { SignInWithEmailAndPasswordUseCase(authenticationRepository = get(), accountStorage = get()) }
    factory { LogOutUseCase(authenticationRepository = get(), accountStorage = get()) }

    factory<AuthenticationRepository> {
        AuthenticationRemoteRepository(
            authenticationExternalDataSource = get(),
            authenticationDataSource = get(),
            networkExceptionHandler = get(),
        )
    }

    factory<AuthenticationDataSource> {
        AuthenticationRemoteKtorDataSource(httpClient = get<HttpClient>(named(HttpClientQualifier.Auth)))
    }

    factory<AuthenticationExternalDataSource> {
        AuthenticationFirebaseDataSource(
            firebaseAuth = get(),
            signInLinkUrlTemplate = get(),
            iosBundleId = get(),
            context = get(),
        )
    }
}
