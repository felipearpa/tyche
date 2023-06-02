package com.felipearpa.tyche.di

import android.content.Context
import com.felipearpa.tyche.core.network.NetworkExceptionHandler
import com.felipearpa.tyche.ui.network.RetrofitExceptionHandler
import com.felipearpa.tyche.user.AuthInterceptor
import com.google.gson.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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
    fun provideOkHttpClient(
        @ApplicationContext context: Context,
        httpLoggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: AuthInterceptor
    ): OkHttpClient =
        OkHttpClient.Builder().apply {
            interceptors().add(httpLoggingInterceptor)
            interceptors().add(authInterceptor)
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
    fun provideRetrofit(gson: Gson, okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder().apply {
            baseUrl("https://0cfe-181-59-233-121.ngrok-free.app")
            addConverterFactory(GsonConverterFactory.create(gson))
            client(okHttpClient)
        }.build()

    @Provides
    @Singleton
    fun provideNetworkExceptionHandler(): NetworkExceptionHandler = RetrofitExceptionHandler()
}