package ar.net.dahool.upsmonitor.data.repository

import ar.net.dahool.upsmonitor.data.model.DeviceRegistration
import ar.net.dahool.upsmonitor.data.model.UpsMetrics
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface UpsApiService {
    @GET("api/status")
    suspend fun getStatus(): UpsMetrics

    @POST("api/register")
    suspend fun registerDevice(@Body registration: DeviceRegistration)
}

