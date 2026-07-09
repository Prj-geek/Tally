package com.tally.app.data.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.tally.app.BuildConfig
import com.tally.app.data.remote.api.SimklApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val SIMKL_BASE_URL = "https://api.simkl.com/"
    private const val APP_NAME = "tally"
    private const val APP_VERSION = "1.0"

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val simklInterceptor = Interceptor { chain ->
            val original = chain.request()
            val url = original.url.newBuilder()
                .addQueryParameter("client_id", BuildConfig.SIMKL_CLIENT_ID)
                .addQueryParameter("app-name", APP_NAME)
                .addQueryParameter("app-version", APP_VERSION)
                .build()
            val request = original.newBuilder()
                .url(url)
                .header("User-Agent", "$APP_NAME/$APP_VERSION")
                .build()
            chain.proceed(request)
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(simklInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(SIMKL_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideSimklApiService(retrofit: Retrofit): SimklApiService {
        return retrofit.create(SimklApiService::class.java)
    }
}
