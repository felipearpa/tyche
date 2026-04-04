package com.felipearpa.tyche.core.di

import com.felipearpa.network.NetworkExceptionHandler
import com.felipearpa.network.ktor.KtorExceptionHandler
import org.koin.dsl.module

val coreModule = module {
    factory<NetworkExceptionHandler> { KtorExceptionHandler() }
}
