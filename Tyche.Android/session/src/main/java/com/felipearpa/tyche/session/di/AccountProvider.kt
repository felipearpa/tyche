package com.felipearpa.tyche.session.di

import android.content.Context
import com.felipearpa.tyche.core.data.StorageInKeyStore
import com.felipearpa.tyche.core.network.UrlBasePathProvider
import com.felipearpa.tyche.session.AccountStorage
import com.felipearpa.tyche.session.AccountStorageInKeyStore
import com.felipearpa.tyche.session.Auth
import com.felipearpa.tyche.session.AuthInterceptor
import com.felipearpa.tyche.session.AuthTokenFirebaseRetriever
import com.felipearpa.tyche.session.AuthTokenRetriever
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
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
                filename = ACCOUNT_FILE_NAME
            )
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
        authInterceptor: AuthInterceptor
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
        gson: Gson,
        @Auth okHttpClient: OkHttpClient
    ): Retrofit =
        Retrofit.Builder().apply {
            baseUrl(urlBasePathProvider.basePath)
            addConverterFactory(GsonConverterFactory.create(gson))
            client(okHttpClient)
        }.build()
}