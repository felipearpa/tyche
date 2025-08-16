package com.felipearpa.tyche.session.authentication.domain

sealed class EmailAndPasswordSignInException : Throwable() {
    data object InvalidCredentials : EmailAndPasswordSignInException() {
        private fun readResolve(): Any = InvalidCredentials
    }
}
