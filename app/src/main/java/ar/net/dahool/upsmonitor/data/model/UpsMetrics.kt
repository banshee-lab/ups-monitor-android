package ar.net.dahool.upsmonitor.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

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
    @Json(name = "runtime_formatted") val runtimeFormatted: String,
    @Json(name = "last_incident") val lastIncident: Incident? = null
) {
    val batteryChargeInt: Int get() = batteryCharge.toIntOrNull() ?: 0
    val upsLoadInt: Int get() = upsLoad.toIntOrNull() ?: 0
    val isOnline: Boolean get() = status.contains("Online", ignoreCase = true)
    val isOnBattery: Boolean get() = status.contains("Battery", ignoreCase = true)
    val isCritical: Boolean get() = status.contains("Low Battery", ignoreCase = true)
}

@JsonClass(generateAdapter = true)
data class Incident(
    @Json(name = "id") val id: Int,
    @Json(name = "status") val status: String,
    @Json(name = "description") val description: String,
    @Json(name = "changed_at") val changedAt: String
) {
    fun getFormattedChangedAt(context: android.content.Context): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val date = sdf.parse(changedAt) ?: return changedAt
            val dateFormat = android.text.format.DateFormat.getDateFormat(context)
            val timeFormat = android.text.format.DateFormat.getTimeFormat(context)
            "${dateFormat.format(date)} ${timeFormat.format(date)}"
        } catch (e: Exception) {
            changedAt
        }
    }
}
