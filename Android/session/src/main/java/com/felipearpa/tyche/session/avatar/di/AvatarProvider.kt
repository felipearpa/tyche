package com.felipearpa.tyche.session.avatar.di

import com.felipearpa.tyche.core.network.HttpClientQualifier
import com.felipearpa.tyche.session.avatar.application.UploadAvatar
import com.felipearpa.tyche.session.avatar.domain.AvatarDataSource
import com.felipearpa.tyche.session.avatar.domain.AvatarRepository
import com.felipearpa.tyche.session.avatar.domain.AvatarUploadDataSource
import com.felipearpa.tyche.session.avatar.infrastructure.AvatarKtorUploadDataSource
import com.felipearpa.tyche.session.avatar.infrastructure.AvatarRemoteKtorDataSource
import com.felipearpa.tyche.session.avatar.infrastructure.AvatarRemoteRepository
import io.ktor.client.HttpClient
import org.koin.core.qualifier.named
import org.koin.dsl.module

val avatarModule = module {
    factory { UploadAvatar(avatarRepository = get(), accountStorage = get()) }

    factory<AvatarRepository> {
        AvatarRemoteRepository(
            avatarDataSource = get(),
            avatarUploadDataSource = get(),
            networkExceptionHandler = get(),
        )
    }

    factory<AvatarDataSource> {
        AvatarRemoteKtorDataSource(httpClient = get<HttpClient>(named(HttpClientQualifier.Auth)))
    }

    factory<AvatarUploadDataSource> {
        AvatarKtorUploadDataSource(httpClient = get())
    }
}
