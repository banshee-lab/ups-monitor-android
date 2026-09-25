package ar.net.dahool.upsmonitor.data.repository

import ar.net.dahool.upsmonitor.data.model.DeviceRegistration
import ar.net.dahool.upsmonitor.data.model.UpsMetrics
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

sealed class UpsResult {
    data class Success(val metrics: UpsMetrics) : UpsResult()
    data class Error(val message: String) : UpsResult()
}

sealed class RegistrationResult {
    object Success : RegistrationResult()
    data class Error(val message: String) : RegistrationResult()
}

class UpsRepository {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private var cachedBaseUrl: String? = null
    private var cachedService: UpsApiService? = null

    private fun buildService(baseUrl: String): UpsApiService {
        // Ensure URL ends with /
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        if (normalizedUrl == cachedBaseUrl && cachedService != null) {
            return cachedService!!
        }
        val service = Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(UpsApiService::class.java)
        cachedBaseUrl = normalizedUrl
        cachedService = service
        return service
    }

    suspend fun fetchStatus(baseUrl: String): UpsResult {
        if (baseUrl.isBlank()) {
            return UpsResult.Error("Server URL is not configured. Tap the settings icon to set it.")
        }
        return try {
            val metrics = buildService(baseUrl).getStatus()
            UpsResult.Success(metrics)
        } catch (e: java.net.ConnectException) {
            UpsResult.Error("Cannot reach server at $baseUrl.\nCheck the URL and your network connection.")
        } catch (e: java.net.SocketTimeoutException) {
            UpsResult.Error("Connection timed out.\nThe server took too long to respond.")
        } catch (e: retrofit2.HttpException) {
            UpsResult.Error("Server returned HTTP ${e.code()}.")
        } catch (e: Exception) {
            UpsResult.Error("Unexpected error: ${e.localizedMessage}")
        }
    }

    suspend fun registerDevice(baseUrl: String, registration: DeviceRegistration): RegistrationResult {
        if (baseUrl.isBlank()) {
            return RegistrationResult.Error("Server URL is not configured.")
        }
        return try {
            buildService(baseUrl).registerDevice(registration)
            RegistrationResult.Success
        } catch (e: java.net.ConnectException) {
            RegistrationResult.Error("Cannot reach server at $baseUrl.")
        } catch (e: java.net.SocketTimeoutException) {
            RegistrationResult.Error("Registration timed out.")
        } catch (e: retrofit2.HttpException) {
            RegistrationResult.Error("Server returned HTTP ${e.code()} during registration.")
        } catch (e: Exception) {
            RegistrationResult.Error("Unexpected error during registration: ${e.localizedMessage}")
        }
    }
}

