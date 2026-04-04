package com.felipearpa.tyche.config.di

import com.felipearpa.tyche.config.LocalIosBundleIdProvider
import com.felipearpa.tyche.config.LocalJoinPoolUrlTemplateProvider
import com.felipearpa.tyche.config.LocalSignInLinkUrlTemplateProvider
import com.felipearpa.tyche.core.IosBundleIdProvider
import com.felipearpa.tyche.core.JoinPoolUrlTemplateProvider
import com.felipearpa.tyche.session.SignInLinkUrlTemplateProvider
import org.koin.dsl.module

val configModule = module {
    factory<SignInLinkUrlTemplateProvider> { LocalSignInLinkUrlTemplateProvider() }
    factory<JoinPoolUrlTemplateProvider> { LocalJoinPoolUrlTemplateProvider() }
    factory<IosBundleIdProvider> { LocalIosBundleIdProvider() }
}
