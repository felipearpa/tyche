package com.felipearpa.tyche.session.di

import com.felipearpa.network.serializer.LocalDateTimeSerializer
import com.felipearpa.tyche.core.data.StorageInKeyStore
import com.felipearpa.tyche.core.network.UrlBasePathProvider
import com.felipearpa.tyche.session.AccountStorage
import com.felipearpa.tyche.session.AccountStorageInKeyStore
import com.felipearpa.tyche.session.AuthTokenFirebaseRetriever
import com.felipearpa.tyche.session.AuthTokenRetriever
import com.google.firebase.auth.FirebaseAuth
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import com.felipearpa.tyche.core.network.HttpClientQualifier
import org.koin.core.qualifier.named
import org.koin.dsl.module
import io.ktor.client.plugins.auth.Auth as KtorAuth

private const val ACCOUNT_FILE_NAME = "account"
private const val ACCOUNT_KEY = "account"

val sessionModule = module {
    single<AccountStorage> {
        AccountStorageInKeyStore(
            StorageInKeyStore(
                context = get(),
                key = ACCOUNT_KEY,
                filename = ACCOUNT_FILE_NAME,
            ),
        )
    }

    single { FirebaseAuth.getInstance() }

    single<AuthTokenRetriever> {
        AuthTokenFirebaseRetriever(firebaseAuth = get())
    }

    single(named(HttpClientQualifier.Auth)) {
        HttpClient(OkHttp) {
            expectSuccess = true
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        serializersModule = SerializersModule {
                            contextual(LocalDateTime::class, LocalDateTimeSerializer)
                        }
                    },
                )
            }
            install(Logging) {
                level = LogLevel.ALL
            }
            install(KtorAuth) {
                bearer {
                    loadTokens {
                        get<AuthTokenRetriever>().authToken()?.let { token ->
                            BearerTokens(token, "")
                        }
                    }
                }
            }
            defaultRequest {
                url(get<UrlBasePathProvider>().basePath)
            }
        }
    }
}
