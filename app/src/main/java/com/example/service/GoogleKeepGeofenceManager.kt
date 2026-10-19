package com.example.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.GeoTaskApplication
import com.example.data.model.GeoReminder
import com.example.receiver.GeofenceBroadcastReceiver
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manages Google Play Services Geofencing API - the exact architecture used by
 * Google Keep and Google Tasks for battery-efficient, system-level location reminders.
 *
 * It registers geofences directly into the hardware geofence subsystem via Google Play Services,
 * ensuring reminders trigger reliably even when the device is asleep or the app is closed.
 */
class GoogleKeepGeofenceManager private constructor(private val context: Context) {

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
            action = GeofenceBroadcastReceiver.ACTION_GEOFENCE_EVENT
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        PendingIntent.getBroadcast(context, GEOFENCE_PENDING_INTENT_REQUEST_CODE, intent, flags)
    }

    /**
     * Builds and registers a Google Play Services Geofence for a GeoReminder.
     */
    @SuppressLint("MissingPermission")
    fun registerGeofence(reminder: GeoReminder) {
        if (!reminder.isEnabled || reminder.isTriggered) return

        try {
            val geofence = Geofence.Builder()
                .setRequestId(reminder.id.toString())
                .setCircularRegion(
                    reminder.latitude,
                    reminder.longitude,
                    reminder.radiusMeters.coerceAtLeast(1f)
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(
                    if (reminder.triggerType == "EXIT") {
                        Geofence.GEOFENCE_TRANSITION_EXIT
                    } else {
                        Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL
                    }
                )
                .setLoiteringDelay(1000) // 1s loitering delay for ultra-fast trigger
                .setNotificationResponsiveness(0) // 0ms delay for immediate alert
                .build()

            val request = GeofencingRequest.Builder()
                .setInitialTrigger(
                    if (reminder.triggerType == "EXIT") GeofencingRequest.INITIAL_TRIGGER_EXIT
                    else (GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_DWELL)
                )
                .addGeofence(geofence)
                .build()

            geofencingClient.addGeofences(request, geofencePendingIntent)
                .addOnSuccessListener {
                    Log.d(TAG, "Google Keep Geofence registered for reminder #${reminder.id} (${reminder.locationName}) with radius ${reminder.radiusMeters}m")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to register Google Keep Geofence for reminder #${reminder.id}: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding geofence for #${reminder.id}: ${e.message}")
        }
    }

    /**
     * Removes an active geofence by reminder ID.
     */
    fun removeGeofence(reminderId: Long) {
        try {
            geofencingClient.removeGeofences(listOf(reminderId.toString()))
                .addOnSuccessListener {
                    Log.d(TAG, "Google Keep Geofence removed for reminder #$reminderId")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to remove Google Keep Geofence for #$reminderId: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing geofence for #$reminderId: ${e.message}")
        }
    }

    /**
     * Synchronizes all active, non-triggered reminders with Google Play Services Geofencing.
     */
    @SuppressLint("MissingPermission")
    fun syncWithDatabase() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = GeoTaskApplication.instance.repository
                val activeReminders = repository.getActiveReminders()
                syncAllGeofences(activeReminders)
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing geofences with database: ${e.message}")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun syncAllGeofences(reminders: List<GeoReminder>) {
        val validReminders = reminders.filter { it.isEnabled && !it.isTriggered }
        if (validReminders.isEmpty()) {
            clearAllGeofences()
            return
        }

        try {
            val geofenceList = validReminders.map { reminder ->
                Geofence.Builder()
                    .setRequestId(reminder.id.toString())
                    .setCircularRegion(
                        reminder.latitude,
                        reminder.longitude,
                        reminder.radiusMeters.coerceAtLeast(1f)
                    )
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(
                        if (reminder.triggerType == "EXIT") {
                            Geofence.GEOFENCE_TRANSITION_EXIT
                        } else {
                            Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL
                        }
                    )
                    .setLoiteringDelay(1000)
                    .setNotificationResponsiveness(0)
                    .build()
            }

            val request = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_DWELL)
                .addGeofences(geofenceList)
                .build()

            geofencingClient.removeGeofences(geofencePendingIntent)
                .addOnCompleteListener {
                    geofencingClient.addGeofences(request, geofencePendingIntent)
                        .addOnSuccessListener {
                            Log.d(TAG, "Synced ${geofenceList.size} Google Keep geofence(s) successfully.")
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Failed to register geofence batch: ${e.message}")
                        }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in syncAllGeofences: ${e.message}")
        }
    }

    fun clearAllGeofences() {
        try {
            geofencingClient.removeGeofences(geofencePendingIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing all geofences: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "GoogleKeepGeofenceMgr"
        private const val GEOFENCE_PENDING_INTENT_REQUEST_CODE = 8800

        @Volatile
        private var INSTANCE: GoogleKeepGeofenceManager? = null

        fun getInstance(context: Context): GoogleKeepGeofenceManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GoogleKeepGeofenceManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
