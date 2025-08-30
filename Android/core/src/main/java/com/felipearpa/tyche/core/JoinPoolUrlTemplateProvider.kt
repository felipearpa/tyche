package com.felipearpa.tyche.core

fun interface JoinPoolUrlTemplateProvider {
    operator fun invoke(): String
}
