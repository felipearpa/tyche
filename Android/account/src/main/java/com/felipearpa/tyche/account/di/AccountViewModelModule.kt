package com.felipearpa.tyche.account.di

import com.felipearpa.tyche.account.byemail.EmailLinkSignInViewModel
import com.felipearpa.tyche.account.byemail.EmailSignInViewModel
import com.felipearpa.tyche.account.byemailandpassword.EmailAndPasswordSignInViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val accountViewModelModule = module {
    viewModel { EmailSignInViewModel(sendSignInLinkToEmail = get()) }
    viewModel { EmailAndPasswordSignInViewModel(signInWithEmailAndPassword = get()) }
    viewModel { EmailLinkSignInViewModel(signInLinkToEmail = get()) }
}
