package com.felipearpa.tyche.account.di

import com.felipearpa.tyche.account.byemail.EmailLinkSignInViewModel
import com.felipearpa.tyche.account.byemail.EmailSignInViewModel
import com.felipearpa.tyche.account.byemailandpassword.EmailAndPasswordSignInViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val accountViewModelModule = module {
    viewModel { EmailSignInViewModel(sendSignInLinkToEmailUseCase = get()) }
    viewModel { EmailAndPasswordSignInViewModel(signInWithEmailAndPasswordUseCase = get()) }
    viewModel { EmailLinkSignInViewModel(signInLinkToEmailUseCase = get()) }
}
