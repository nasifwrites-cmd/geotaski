package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.GeoTaskApplication
import com.example.service.BuzzAlertHelper
import com.example.service.GoogleKeepGeofenceManager
import com.example.service.LocationMonitoringService
import com.example.service.NotificationHelper
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered by Google Play Services Geofencing subsystem
 * (the exact Google Keep / Google Tasks location reminder technology).
 *
 * This receiver wakes up the device and rings the alarm immediately upon
 * entering the geofence perimeter, even when the app is killed or screen is off!
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null) {
            Log.w(TAG, "Received null GeofencingEvent")
            return
        }

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, "Google Play Services Geofencing error code ${geofencingEvent.errorCode}: $errorMessage")
            return
        }

        val transitionType = geofencingEvent.geofenceTransition
        val isTargetTransition = transitionType == Geofence.GEOFENCE_TRANSITION_ENTER ||
                transitionType == Geofence.GEOFENCE_TRANSITION_DWELL ||
                transitionType == Geofence.GEOFENCE_TRANSITION_EXIT

        if (!isTargetTransition) return

        val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = GeoTaskApplication.instance.repository
                val now = System.currentTimeMillis()

                for (geofence in triggeringGeofences) {
                    val reminderId = geofence.requestId.toLongOrNull() ?: continue
                    val reminder = repository.getReminderById(reminderId) ?: continue

                    // If already handled, disabled, or triggered, skip
                    if (!reminder.isEnabled || reminder.isTriggered) continue
                    if (reminder.snoozedUntil != null && reminder.snoozedUntil > now) continue

                    Log.d(TAG, "Google Keep Geofence TRIGGERED for reminder #${reminder.id} (${reminder.locationName})!")

                    // 1. Mark in memory to prevent double trigger by foreground service
                    LocationMonitoringService.markReminderFinished(reminder.id)

                    // 2. Update database trigger status
                    repository.updateTriggerStatus(reminder.id, isTriggered = true, timestamp = now)

                    // 3. Remove this geofence from Google Play Services client
                    GoogleKeepGeofenceManager.getInstance(context).removeGeofence(reminder.id)

                    // 4. Fetch tasks
                    val tasks = repository.getTasksForReminder(reminder.id)

                    // 5. Fire continuous loud buzzing alarm and vibration
                    BuzzAlertHelper.startContinuousAlarm(context, reminder.locationName, reminder.id)

                    // 6. Show high-priority heads-up notification with action buttons
                    NotificationHelper.showProximityNotification(
                        context = context,
                        reminder = reminder,
                        tasks = tasks
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling geofence transition: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_GEOFENCE_EVENT = "com.example.action.ACTION_GEOFENCE_EVENT"
        private const val TAG = "GeofenceReceiver"
    }
}
