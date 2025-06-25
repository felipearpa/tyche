package com.felipearpa.tyche.session.di

import android.content.Context
import com.felipearpa.network.serializer.LocalDateTimeSerializer
import com.felipearpa.tyche.core.data.StorageInKeyStore
import com.felipearpa.tyche.core.network.UrlBasePathProvider
import com.felipearpa.tyche.session.AccountStorage
import com.felipearpa.tyche.session.AccountStorageInKeyStore
import com.felipearpa.tyche.session.Auth
import com.felipearpa.tyche.session.AuthInterceptor
import com.felipearpa.tyche.session.AuthTokenFirebaseRetriever
import com.felipearpa.tyche.session.AuthTokenRetriever
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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

private const val ACCOUNT_FILE_NAME = "account"
private const val ACCOUNT_KEY = "account"

@Module
@InstallIn(SingletonComponent::class)
object AuthProvider {
    @Provides
    @Singleton
    fun provideAuthInterceptor(authTokenRetriever: AuthTokenRetriever) =
        AuthInterceptor(authTokenRetriever = authTokenRetriever)

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
    fun provideOkHttpClient(
        httpLoggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: AuthInterceptor,
    ): OkHttpClient =
        OkHttpClient.Builder().apply {
            interceptors().add(httpLoggingInterceptor)
            interceptors().add(authInterceptor)
        }.build()

    @Auth
    @Provides
    @Singleton
    fun provideRetrofit(
        urlBasePathProvider: UrlBasePathProvider,
        @Auth okHttpClient: OkHttpClient,
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
