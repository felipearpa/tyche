package com.felipearpa.tyche.session.di

import android.content.Context
import com.felipearpa.network.serializer.LocalDateTimeSerializer
import com.felipearpa.tyche.core.data.StorageInKeyStore
import com.felipearpa.tyche.core.network.UrlBasePathProvider
import com.felipearpa.tyche.session.AccountStorage
import com.felipearpa.tyche.session.AccountStorageInKeyStore
import com.felipearpa.tyche.session.Auth
import com.felipearpa.tyche.session.AuthTokenFirebaseRetriever
import com.felipearpa.tyche.session.AuthTokenRetriever
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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
import javax.inject.Singleton
import io.ktor.client.plugins.auth.Auth as KtorAuth

private const val ACCOUNT_FILE_NAME = "account"
private const val ACCOUNT_KEY = "account"

@Module
@InstallIn(SingletonComponent::class)
object AuthProvider {
    @Provides
    @Singleton
    fun provideAccountStorage(@ApplicationContext context: Context): AccountStorage =
        AccountStorageInKeyStore(
            StorageInKeyStore(
                context = context,
                key = ACCOUNT_KEY,
                filename = ACCOUNT_FILE_NAME,
            ),
        )

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideAuthTokenRetriever(firebaseAuth: FirebaseAuth): AuthTokenRetriever =
        AuthTokenFirebaseRetriever(firebaseAuth = firebaseAuth)

    @Auth
    @Provides
    @Singleton
    fun provideAuthHttpClient(
        urlBasePathProvider: UrlBasePathProvider,
        authTokenRetriever: AuthTokenRetriever,
    ): HttpClient =
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
                        authTokenRetriever.authToken()?.let { token ->
                            BearerTokens(token, "")
                        }
                    }
                }
            }
            defaultRequest {
                url(urlBasePathProvider.basePath)
            }
        }
}
