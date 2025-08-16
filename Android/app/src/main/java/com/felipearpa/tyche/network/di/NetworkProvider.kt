package com.felipearpa.tyche.network.di

import com.felipearpa.network.serializer.LocalDateTimeSerializer
import com.felipearpa.tyche.core.network.UrlBasePathProvider
import com.felipearpa.tyche.network.LocalUrlBasePathProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkProvider {
    @Provides
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)

    @Provides
    fun provideOkHttpClient(httpLoggingInterceptor: HttpLoggingInterceptor): OkHttpClient =
        OkHttpClient.Builder().apply {
            interceptors().add(httpLoggingInterceptor)
        }.build()

    @Provides
    fun provideUrlBasePathProvider(): UrlBasePathProvider = LocalUrlBasePathProvider()

    @Provides
    fun provideRetrofit(
        urlBasePathProvider: UrlBasePathProvider,
        okHttpClient: OkHttpClient,
    ): Retrofit {
        val json = Json {
            ignoreUnknownKeys = true
            serializersModule = SerializersModule {
                contextual(LocalDateTime::class, LocalDateTimeSerializer)
            }
        }

        return Retrofit.Builder()
            .baseUrl(urlBasePathProvider.basePath)
            .addConverterFactory(json.asConverterFactory("application/json; charset=UTF-8".toMediaType()))
            .client(okHttpClient)
            .build()
    }
}
