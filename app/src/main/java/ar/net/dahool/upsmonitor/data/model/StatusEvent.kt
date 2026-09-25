package ar.net.dahool.upsmonitor.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@JsonClass(generateAdapter = true)
data class StatusEvent(
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
