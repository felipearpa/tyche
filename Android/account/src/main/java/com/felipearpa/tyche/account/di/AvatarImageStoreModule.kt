package com.felipearpa.tyche.account.di

import com.felipearpa.tyche.account.AvatarImageStore
import com.felipearpa.tyche.account.AvatarImageStoreInstallUploadedAvatar
import com.felipearpa.tyche.account.CoilAvatarMemoryCacheGateway
import com.felipearpa.tyche.account.CoilAvatarRevalidator
import com.felipearpa.tyche.session.avatar.domain.InstallUploadedAvatar
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val avatarImageStoreModule = module {
    single {
        AvatarImageStore(
            memoryCache = CoilAvatarMemoryCacheGateway(context = androidContext()),
            revalidator = CoilAvatarRevalidator(context = androidContext()),
        )
    }

    single<InstallUploadedAvatar> {
        AvatarImageStoreInstallUploadedAvatar(avatarImageStore = get())
    }
}
