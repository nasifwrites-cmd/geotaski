package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.GeoTaskApplication
import com.example.MainActivity
import com.example.R
import com.example.data.model.GeoReminder
import com.example.data.model.GeoTaskItem
import com.example.receiver.NotificationActionReceiver

object NotificationHelper {

    fun showProximityNotification(
        context: Context,
        reminder: GeoReminder,
        tasks: List<GeoTaskItem>
    ) {
        val notificationManager = NotificationManagerCompat.from(context)

        // Main Tap Intent: Opens app
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_REMINDER_ID, reminder.id)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            reminder.id.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Complete All
        val doneIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MARK_DONE
            putExtra(EXTRA_REMINDER_ID, reminder.id)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            (reminder.id * 10 + 1).toInt(),
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Snooze 15 mins
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SNOOZE
            putExtra(EXTRA_REMINDER_ID, reminder.id)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (reminder.id * 10 + 2).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val titlePrefix = if (reminder.triggerType == "EXIT") "🚗 Left" else "📍 Arrived at"
        val headerTitle = "$titlePrefix ${reminder.locationName}"

        val pendingTasks = tasks.filter { !it.isCompleted }
        val taskSummary = if (pendingTasks.isNotEmpty()) {
            "Tasks: " + pendingTasks.joinToString(", ") { it.title }
        } else if (reminder.description.isNotBlank()) {
            reminder.description
        } else {
            reminder.title
        }

        // Expandable Inbox style for tasks
        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle("${reminder.title} ($headerTitle)")
            .setSummaryText("${tasks.count { it.isCompleted }}/${tasks.size} completed")

        if (reminder.description.isNotBlank()) {
            inboxStyle.addLine("📝 ${reminder.description}")
        }
        pendingTasks.take(4).forEach {
            inboxStyle.addLine("☐ ${it.title}")
        }
        if (pendingTasks.size > 4) {
            inboxStyle.addLine("+ ${pendingTasks.size - 4} more tasks...")
        }

        val notification = NotificationCompat.Builder(context, GeoTaskApplication.CHANNEL_REMINDERS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(reminder.title)
            .setContentText(taskSummary)
            .setSubText(reminder.locationName)
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(
                android.R.drawable.checkbox_on_background,
                context.getString(R.string.action_complete),
                donePendingIntent
            )
            .addAction(
                android.R.drawable.ic_lock_idle_alarm,
                context.getString(R.string.action_snooze),
                snoozePendingIntent
            )
            .build()

        try {
            notificationManager.notify(reminder.id.toInt(), notification)
        } catch (_: SecurityException) {
            // Handled when notification permission is not yet granted
        }
    }

    fun buildMonitoringNotification(
        context: Context,
        activeCount: Int,
        lastKnownCoords: String
    ): Notification {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            9999,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, GeoTaskApplication.CHANNEL_MONITOR)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("GeoTask • Proximity Guard Active")
            .setContentText("Monitoring $activeCount location reminder(s) | GPS: $lastKnownCoords")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .build()
    }

    const val EXTRA_REMINDER_ID = "extra_reminder_id"
}
