package com.felipearpa.tyche.session.authentication.domain

sealed class GoogleSignInException : Throwable() {
    data object InvalidCredential : GoogleSignInException() {
        private fun readResolve(): Any = InvalidCredential
    }

    data object AccountExistsWithDifferentCredential : GoogleSignInException() {
        private fun readResolve(): Any = AccountExistsWithDifferentCredential
    }

    data object NetworkError : GoogleSignInException() {
        private fun readResolve(): Any = NetworkError
    }

    data object Cancelled : GoogleSignInException() {
        private fun readResolve(): Any = Cancelled
    }
}
