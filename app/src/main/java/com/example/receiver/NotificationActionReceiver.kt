package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.GeoTaskApplication
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

        when (intent.action) {
            ACTION_MARK_DONE -> {
                // Dismiss notification
                notificationManager.cancel(reminderId.toInt())
                CoroutineScope(Dispatchers.IO).launch {
                    val tasks = repository.getTasksForReminder(reminderId)
                    // Mark all tasks completed
                    tasks.forEach { task ->
                        repository.setTaskCompleted(task.id, true)
                    }
                    // Mark reminder triggered
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
        const val ACTION_MARK_DONE = "com.example.geotask.ACTION_MARK_DONE"
        const val ACTION_SNOOZE = "com.example.geotask.ACTION_SNOOZE"
    }
}
