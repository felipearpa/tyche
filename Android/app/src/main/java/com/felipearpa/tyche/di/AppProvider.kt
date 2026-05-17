package com.felipearpa.tyche.di

import com.felipearpa.tyche.UsernameEditorViewModel
import com.felipearpa.tyche.account.bygoogle.WebClientIdProvider
import com.felipearpa.tyche.config.LocalIosBundleIdProvider
import com.felipearpa.tyche.config.LocalJoinPoolUrlTemplateProvider
import com.felipearpa.tyche.config.LocalSignInLinkUrlTemplateProvider
import com.felipearpa.tyche.config.LocalWebClientIdProvider
import com.felipearpa.tyche.core.IosBundleIdProvider
import com.felipearpa.tyche.core.JoinPoolUrlTemplateProvider
import com.felipearpa.tyche.session.SignInLinkUrlTemplateProvider
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    factory<SignInLinkUrlTemplateProvider> { LocalSignInLinkUrlTemplateProvider() }
    factory<JoinPoolUrlTemplateProvider> { LocalJoinPoolUrlTemplateProvider() }
    factory<IosBundleIdProvider> { LocalIosBundleIdProvider() }
    factory<WebClientIdProvider> { LocalWebClientIdProvider() }
    viewModel { UsernameEditorViewModel(updateUsername = get()) }
}
