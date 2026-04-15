package com.felipearpa.tyche.session.authentication.di

import com.felipearpa.tyche.session.authentication.application.LogOut
import com.felipearpa.tyche.session.authentication.application.SendSignInLinkToEmail
import com.felipearpa.tyche.session.authentication.application.SignInWithEmailAndPassword
import com.felipearpa.tyche.session.authentication.application.SignInWithEmailLink
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
    factory { SendSignInLinkToEmail(authenticationRepository = get()) }
    factory { SignInWithEmailLink(authenticationRepository = get(), accountStorage = get()) }
    factory { SignInWithEmailAndPassword(authenticationRepository = get(), accountStorage = get()) }
    factory { LogOut(authenticationRepository = get(), accountStorage = get()) }

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
