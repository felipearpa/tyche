package com.felipearpa.tyche.session.authentication.domain

sealed class SignInWithEmailLinkException : Throwable() {
    data object AuthenticationFailed : SignInWithEmailLinkException() {
        private fun readResolve(): Any = AuthenticationFailed
    }

    data object InvalidEmailLink : SignInWithEmailLinkException() {
        private fun readResolve(): Any = InvalidEmailLink
    }
}