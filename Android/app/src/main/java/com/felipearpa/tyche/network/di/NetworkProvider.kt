package com.felipearpa.tyche.network.di

import com.felipearpa.network.serializer.LocalDateTimeSerializer
import com.felipearpa.tyche.core.network.UrlBasePathProvider
import com.felipearpa.tyche.network.LocalUrlBasePathProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

@Module
@InstallIn(SingletonComponent::class)
object NetworkProvider {
    @Provides
    fun provideUrlBasePathProvider(): UrlBasePathProvider = LocalUrlBasePathProvider()

    @Provides
    fun provideHttpClient(urlBasePathProvider: UrlBasePathProvider): HttpClient =
        HttpClient(OkHttp) {
            expectSuccess = true
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    serializersModule = SerializersModule {
                        contextual(LocalDateTime::class, LocalDateTimeSerializer)
                    }
                })
            }
            install(Logging) {
                level = LogLevel.ALL
            }
            defaultRequest {
                url(urlBasePathProvider.basePath)
            }
        }
}
