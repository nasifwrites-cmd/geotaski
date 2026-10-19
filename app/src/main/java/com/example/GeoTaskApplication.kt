package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.data.local.AppDatabase
import com.example.data.repository.GeoReminderRepository

class GeoTaskApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: GeoReminderRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getInstance(this)
        repository = GeoReminderRepository(database.geoTaskDao())

        createNotificationChannels()
        com.example.service.GoogleKeepGeofenceManager.getInstance(this).syncWithDatabase()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // High priority channel for when user enters/exits location zone
            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                getString(R.string.channel_reminders_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.channel_reminders_desc)
                enableVibration(false)
                setShowBadge(true)
            }

            // Low priority channel for ongoing background location monitoring service
            val monitorChannel = NotificationChannel(
                CHANNEL_MONITOR,
                getString(R.string.channel_monitor_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_monitor_desc)
                setShowBadge(false)
            }

            notificationManager?.createNotificationChannel(reminderChannel)
            notificationManager?.createNotificationChannel(monitorChannel)
        }
    }

    companion object {
        const val CHANNEL_REMINDERS = "geotask_reminders_channel"
        const val CHANNEL_MONITOR = "geotask_monitor_channel"

        lateinit var instance: GeoTaskApplication
            private set
    }
}
