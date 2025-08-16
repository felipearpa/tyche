package com.felipearpa.tyche.session

fun interface SignInLinkUrlTemplateProvider {
    operator fun invoke(): String
}
