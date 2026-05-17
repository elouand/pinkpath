package com.traveling.di

import com.traveling.NetworkConfig
import com.traveling.data.local.SessionManager
import com.traveling.data.remote.AuthApi
import com.traveling.data.remote.ItineraryApi
import com.traveling.data.remote.NominatimApi
import com.traveling.data.remote.PhotonApi
import com.traveling.data.remote.PhotonResponse
import com.traveling.data.remote.PlaceApi
import com.traveling.data.remote.PostApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideAuthInterceptor(sessionManager: SessionManager): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()
            val token = sessionManager.getAuthToken()
            
            val requestBuilder = originalRequest.newBuilder()
            if (token != null) {
                requestBuilder.header("Authorization", "Bearer $token")
            }
            
            chain.proceed(requestBuilder.build())
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: Interceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(NetworkConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun providePostApi(retrofit: Retrofit): PostApi = retrofit.create(PostApi::class.java)

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun providePlaceApi(retrofit: Retrofit): PlaceApi = retrofit.create(PlaceApi::class.java)

    @Provides
    @Singleton
    fun provideItineraryApi(retrofit: Retrofit): ItineraryApi = retrofit.create(ItineraryApi::class.java)

    @Provides
    @Singleton
    fun providePhotonApi(): PhotonApi {
        val nominatimClient = okhttp3.OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", "TravelingApp/1.0 (student project)")
                        .build()
                )
            }
            .build()

        val nominatimApi = Retrofit.Builder()
            .baseUrl("https://nominatim.openstreetmap.org/")
            .client(nominatimClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NominatimApi::class.java)

        return object : PhotonApi {
            override suspend fun search(
                query: String, limit: Int, lang: String, lat: Double?, lon: Double?
            ): PhotonResponse {
                val results = nominatimApi.search(query = query, limit = limit, lang = lang, lat = lat, lon = lon)
                return PhotonResponse(features = results.map { it.toPhotonFeature() })
            }
        }
    }
}
