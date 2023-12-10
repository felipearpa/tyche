package com.felipearpa.tyche.network.di

import com.felipearpa.tyche.core.network.UrlBasePathProvider
import com.felipearpa.tyche.network.LocalUrlBasePathProvider
import com.google.gson.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.ZonedDateTime
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
    fun provideGson(): Gson =
        GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY)
            .serializeNulls()
            .setLenient()
            .registerTypeAdapter(
                LocalDateTime::class.java,
                JsonDeserializer { jsonElement: JsonElement, _: Type, _: JsonDeserializationContext ->
                    ZonedDateTime.parse(jsonElement.asJsonPrimitive.asString).toLocalDateTime()
                })
            .create()

    @Provides
    @Singleton
    fun provideUrlBasePathProvider(): UrlBasePathProvider = LocalUrlBasePathProvider()

    @Provides
    @Singleton
    fun provideRetrofit(
        urlBasePathProvider: UrlBasePathProvider,
        gson: Gson,
        okHttpClient: OkHttpClient
    ): Retrofit =
        Retrofit.Builder().apply {
            baseUrl(urlBasePathProvider.basePath)
            addConverterFactory(GsonConverterFactory.create(gson))
            client(okHttpClient)
        }.build()
}