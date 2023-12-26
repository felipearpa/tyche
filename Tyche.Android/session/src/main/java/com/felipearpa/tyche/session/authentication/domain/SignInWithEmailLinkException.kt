package com.felipearpa.tyche.session.authentication.domain

sealed class SignInWithEmailLinkException : Throwable() {
    data object AuthenticationFailed : SignInWithEmailLinkException()
    data object InvalidEmailLink : SignInWithEmailLinkException()
}