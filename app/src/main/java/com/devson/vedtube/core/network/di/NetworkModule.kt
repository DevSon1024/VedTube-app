package com.devson.vedtube.core.network.di

import android.content.Context
import com.devson.vedtube.BuildConfig
import com.devson.vedtube.data.provider.youtube.cobalt.CobaltApiService
import com.devson.vedtube.data.provider.youtube.piped.PipedApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import java.net.Inet4Address
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val CACHE_SIZE_BYTES = 50L * 1024L * 1024L // 50 MB
    private const val TIMEOUT_SECONDS = 30L
    private const val PIPED_BASE_URL = "https://pipedapi.kavin.rocks/"
    private const val COBALT_BASE_URL = "https://co.wuk.sh/"

    @OptIn(ExperimentalSerializationApi::class)
    @Provides
    @Singleton
    fun providesJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = true
            encodeDefaults = true
            explicitNulls = false
        }
    }

    @Provides
    @Singleton
    fun providesOkHttpClient(
        @ApplicationContext context: Context
    ): OkHttpClient {
        val cookieStore = ConcurrentHashMap<String, List<Cookie>>()

        val cookieJar = object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                val existing = cookieStore[url.host].orEmpty().toMutableList()
                cookies.forEach { newCookie ->
                    existing.removeAll { it.name == newCookie.name }
                    existing.add(newCookie)
                }
                cookieStore[url.host] = existing
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                return cookieStore[url.host] ?: emptyList()
            }
        }

        // Force IPv4 lookup to prevent IPv6 routing hangs on restricted networks
        val ipv4Dns = object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                return try {
                    val addresses = Dns.SYSTEM.lookup(hostname)
                    val ipv4List = addresses.filterIsInstance<Inet4Address>()
                    if (ipv4List.isNotEmpty()) ipv4List else addresses
                } catch (e: Exception) {
                    Dns.SYSTEM.lookup(hostname)
                }
            }
        }

        val builder = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .cookieJar(cookieJar)
            .dns(ipv4Dns)
            .cache(Cache(File(context.cacheDir, "http_cache"), CACHE_SIZE_BYTES))
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .build()
                chain.proceed(request)
            }

        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            builder.addInterceptor(loggingInterceptor)
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun providesPipedApiService(
        okHttpClient: OkHttpClient,
        json: Json
    ): PipedApiService {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(PIPED_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(PipedApiService::class.java)
    }

    @Provides
    @Singleton
    fun providesCobaltApiService(
        okHttpClient: OkHttpClient,
        json: Json
    ): CobaltApiService {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(COBALT_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(CobaltApiService::class.java)
    }
}
