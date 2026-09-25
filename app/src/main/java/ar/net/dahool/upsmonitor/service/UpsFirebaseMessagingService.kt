package ar.net.dahool.upsmonitor.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import ar.net.dahool.upsmonitor.MainActivity
import ar.net.dahool.upsmonitor.R
import ar.net.dahool.upsmonitor.data.model.DeviceRegistration
import ar.net.dahool.upsmonitor.data.repository.RegistrationResult
import ar.net.dahool.upsmonitor.data.repository.UpsRepository
import ar.net.dahool.upsmonitor.util.SettingsRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class UpsFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "UpsFirebaseMsgService"
        private const val CHANNEL_ID = "ups_alerts"
        private const val CHANNEL_NAME = "UPS Alerts"
        private var notificationId = 1000
    }

    // This service can be started by the system without any Activity alive,
    // so registration work uses its own scope rather than a ViewModel's.
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Log the token for debugging/manual setup if needed.
        Log.i(TAG, "FCM Registration Token: $token")
        registerNewToken(token)
    }

    private fun registerNewToken(token: String) {
        serviceScope.launch {
            val settings = SettingsRepository(applicationContext)
            val repository = UpsRepository()

            val baseUrl = settings.serverUrlFlow.first()
            if (baseUrl.isBlank()) {
                Log.d(TAG, "No server configured yet; skipping registration of new token.")
                return@launch
            }

            val deviceId = settings.getOrCreateDeviceId()
            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim()

            val registration = DeviceRegistration(
                deviceToken = token,
                deviceName = deviceName,
                deviceId = deviceId
            )

            when (val result = repository.registerDevice(baseUrl, registration)) {
                is RegistrationResult.Success -> {
                    Log.i(TAG, "New FCM token registered with $baseUrl")
                    settings.saveLastRegistration(token, baseUrl)
                }
                is RegistrationResult.Error -> {
                    Log.w(TAG, "Failed to register new FCM token: ${result.message}")
                }
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM message received from: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "UPS Alert"
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "Check your UPS status."

        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Ensure channel exists (safe to call multiple times)
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts from your UPS monitor server"
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)

        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(notificationId++, notification)
    }
}

