package com.felipearpa.tyche.session.authentication.domain

sealed class SignInWithEmailAndPasswordException : Throwable() {
    data object AuthenticationFailed : SignInWithEmailAndPasswordException() {
        private fun readResolve(): Any = AuthenticationFailed
    }

    data object InvalidCredentials : SignInWithEmailAndPasswordException() {
        private fun readResolve(): Any = InvalidCredentials
    }
}