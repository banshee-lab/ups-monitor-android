package ar.net.dahool.upsmonitor.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UpsMetrics(
    @Json(name = "manufacturer") val manufacturer: String,
    @Json(name = "model") val model: String,
    @Json(name = "serial") val serial: String,
    @Json(name = "status") val status: String,
    @Json(name = "battery_charge") val batteryCharge: String,
    @Json(name = "battery_voltage") val batteryVoltage: String,
    @Json(name = "battery_voltage_nominal") val batteryVoltageNominal: String,
    @Json(name = "input_voltage") val inputVoltage: String,
    @Json(name = "input_voltage_nominal") val inputVoltageNominal: String,
    @Json(name = "ups_load") val upsLoad: String,
    @Json(name = "runtime_seconds") val runtimeSeconds: String,
    @Json(name = "runtime_formatted") val runtimeFormatted: String
) {
    val batteryChargeInt: Int get() = batteryCharge.toIntOrNull() ?: 0
    val upsLoadInt: Int get() = upsLoad.toIntOrNull() ?: 0
    val isOnline: Boolean get() = status.contains("Online", ignoreCase = true)
    val isOnBattery: Boolean get() = status.contains("Battery", ignoreCase = true)
    val isCritical: Boolean get() = status.contains("Low Battery", ignoreCase = true)
}
