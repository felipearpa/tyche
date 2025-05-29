package com.felipearpa.tyche.network.di

import com.felipearpa.tyche.core.network.UrlBasePathProvider
import com.felipearpa.tyche.core.network.retrofit.LocalDateTimeSerializer
import com.felipearpa.tyche.network.LocalUrlBasePathProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.time.LocalDateTime
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkProvider {
    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)

    @Provides
    @Singleton
    fun provideOkHttpClient(httpLoggingInterceptor: HttpLoggingInterceptor): OkHttpClient =
        OkHttpClient.Builder().apply {
            interceptors().add(httpLoggingInterceptor)
        }.build()

    @Provides
    @Singleton
    fun provideUrlBasePathProvider(): UrlBasePathProvider = LocalUrlBasePathProvider()

    @Provides
    @Singleton
    fun provideRetrofit(
        urlBasePathProvider: UrlBasePathProvider,
        okHttpClient: OkHttpClient,
    ): Retrofit {
        val json = Json {
            serializersModule = SerializersModule {
                contextual(LocalDateTime::class, LocalDateTimeSerializer)
            }
        }

        return Retrofit.Builder()
            .baseUrl(urlBasePathProvider.basePath)
            .addConverterFactory(json.asConverterFactory("application/json; charset=UTF8".toMediaType()))
            .client(okHttpClient)
            .build()
    }
}
