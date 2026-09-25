package ar.net.dahool.upsmonitor.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DeviceRegistration(
    @Json(name = "device_token") val deviceToken: String,
    @Json(name = "device_name") val deviceName: String,
    @Json(name = "device_id") val deviceId: String
)
