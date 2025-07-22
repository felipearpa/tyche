package com.felipearpa.tyche.session.authentication.domain

sealed class EmailLinkSignInException : Throwable() {
    data object InvalidEmailLink : EmailLinkSignInException() {
        private fun readResolve(): Any = InvalidEmailLink
    }
}
