package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.GeoTaskApplication
import com.example.service.BuzzAlertHelper
import com.example.service.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(NotificationHelper.EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) return

        val repository = GeoTaskApplication.instance.repository
        val notificationManager = NotificationManagerCompat.from(context)

        // Stop continuous buzzing alarm immediately on any user action
        BuzzAlertHelper.stopAlarm(context)

        when (intent.action) {
            ACTION_STOP_ALARM -> {
                // Dismiss notification and silence alarm
                notificationManager.cancel(reminderId.toInt())
            }
            ACTION_MARK_DONE -> {
                // Dismiss notification, silence alarm, mark tasks done
                notificationManager.cancel(reminderId.toInt())
                CoroutineScope(Dispatchers.IO).launch {
                    val tasks = repository.getTasksForReminder(reminderId)
                    tasks.forEach { task ->
                        repository.setTaskCompleted(task.id, true)
                    }
                    repository.updateTriggerStatus(reminderId, isTriggered = true)
                }
            }
            ACTION_SNOOZE -> {
                notificationManager.cancel(reminderId.toInt())
                CoroutineScope(Dispatchers.IO).launch {
                    repository.snoozeReminder(reminderId, minutes = 15)
                }
            }
        }
    }

    companion object {
        const val ACTION_STOP_ALARM = "com.example.geotask.ACTION_STOP_ALARM"
        const val ACTION_MARK_DONE = "com.example.geotask.ACTION_MARK_DONE"
        const val ACTION_SNOOZE = "com.example.geotask.ACTION_SNOOZE"
    }
}
