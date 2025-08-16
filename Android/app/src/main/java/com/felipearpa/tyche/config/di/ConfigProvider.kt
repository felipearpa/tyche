package com.felipearpa.tyche.config.di

import com.felipearpa.tyche.config.LocalSignInLinkUrlTemplateProvider
import com.felipearpa.tyche.session.SignInLinkUrlTemplateProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ConfigProvider {
    @Provides
    fun provideSignInLinkUrlTemplateProvider(): SignInLinkUrlTemplateProvider =
        LocalSignInLinkUrlTemplateProvider()
}
