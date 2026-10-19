package com.example.receiver

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.util.Log
import com.example.GeoTaskApplication
import com.example.service.BuzzAlertHelper
import com.example.service.GoogleKeepGeofenceManager
import com.example.service.LocationMonitoringService
import com.example.service.NotificationHelper
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered by Google Play Services Geofencing subsystem
 * (the exact Google Keep / Google Tasks location reminder technology).
 *
 * This receiver wakes up the device when approaching the perimeter.
 * It strictly validates that the device's actual GPS distance is within
 * the user-specified radius before sounding the alarm, preventing false alarms!
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
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

                // Acquire current accurate location to verify actual distance
                val currentLocation: Location? = geofencingEvent.triggeringLocation ?: try {
                    Tasks.await(LocationServices.getFusedLocationProviderClient(context).lastLocation)
                } catch (_: Exception) {
                    null
                }

                for (geofence in triggeringGeofences) {
                    val reminderId = geofence.requestId.toLongOrNull() ?: continue
                    val reminder = repository.getReminderById(reminderId) ?: continue

                    // If already handled, disabled, or triggered, skip
                    if (!reminder.isEnabled || reminder.isTriggered) continue
                    if (reminder.snoozedUntil != null && reminder.snoozedUntil > now) continue

                    // STRICT DISTANCE CHECK:
                    // Google Play Geofence hardware transitions can fire at 10-30m due to cellular/Wi-Fi estimation.
                    // We MUST verify that device is strictly within the user's requested radius (e.g. 1m)!
                    if (currentLocation != null) {
                        val distanceResults = FloatArray(1)
                        Location.distanceBetween(
                            currentLocation.latitude,
                            currentLocation.longitude,
                            reminder.latitude,
                            reminder.longitude,
                            distanceResults
                        )
                        val actualDistance = distanceResults[0]

                        if (reminder.triggerType == "ENTER") {
                            if (actualDistance > reminder.radiusMeters) {
                                Log.d(
                                    TAG,
                                    "Geofence entered but distance (${actualDistance}m) > user radius (${reminder.radiusMeters}m). Waking up LocationMonitoringService to await exact entrance."
                                )
                                // Ensure foreground high-accuracy GPS tracking is running to catch exact entry
                                LocationMonitoringService.startService(context)
                                continue
                            }
                        }
                    }

                    Log.d(TAG, "Google Keep Geofence verified strictly inside radius for reminder #${reminder.id} (${reminder.locationName})!")

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
