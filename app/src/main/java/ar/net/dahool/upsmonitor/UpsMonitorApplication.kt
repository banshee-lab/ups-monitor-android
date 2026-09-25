package ar.net.dahool.upsmonitor

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

class UpsMonitorApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            "ups_alerts",
            "UPS Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Status alerts from your UPS monitor server"
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }
}
