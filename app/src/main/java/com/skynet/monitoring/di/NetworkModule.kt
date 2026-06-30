package com.skynet.monitoring.di

import com.google.gson.Gson
import com.skynet.monitoring.BuildConfig
import com.skynet.monitoring.data.api.ApiService
import com.skynet.monitoring.data.api.AuthInterceptor
import com.skynet.monitoring.data.api.ServerUrlInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        serverUrlInterceptor: ServerUrlInterceptor,
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.NONE
        }
        val builder = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
        // Override alamat server HANYA di build debug (alat bantu tes). Build release
        // selalu pakai BuildConfig.BASE_URL resmi — tak bisa ditimpa.
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(serverUrlInterceptor)
        }
        return builder
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService =
        retrofit.create(ApiService::class.java)
}
