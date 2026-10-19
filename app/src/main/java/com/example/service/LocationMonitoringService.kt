package com.example.service

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.ServiceCompat
import com.example.GeoTaskApplication
import com.example.data.model.GeoReminder
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LocationMonitoringService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var locationManager: LocationManager? = null
    private var gpsLocationListener: LocationListener? = null
    private var networkLocationListener: LocationListener? = null
    private var wakeLock: PowerManager.WakeLock? = null

    // Track state for each reminder to detect the exact moment of entry
    private val previousInsideState = mutableMapOf<Long, Boolean>()
    private val triggeredOrFinishedIds = java.util.Collections.synchronizedSet(mutableSetOf<Long>())

    override fun onCreate() {
        super.onCreate()
        instance = this
        _isServiceRunning.value = true

        acquireWakeLock()
        startForegroundWithNotification(0, "Active")
        setupUltraFastLocationTracking()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SERVICE -> {
                releaseWakeLock()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_SIMULATE_LOCATION -> {
                val lat = intent.getDoubleExtra(EXTRA_LATITUDE, 0.0)
                val lng = intent.getDoubleExtra(EXTRA_LONGITUDE, 0.0)
                val mockLoc = Location("simulated").apply {
                    latitude = lat
                    longitude = lng
                    time = System.currentTimeMillis()
                    accuracy = 1.0f
                }
                processLocationUpdate(mockLoc)
            }
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        try {
            val restartServiceIntent = Intent(applicationContext, LocationMonitoringService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            val restartPendingIntent = PendingIntent.getService(
                applicationContext,
                101,
                restartServiceIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.set(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 1000L,
                restartPendingIntent
            )
        } catch (e: Exception) {
            Log.w(TAG, "Could not schedule service restart: ${e.message}")
        }
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "GeoTask::HighSpeedLocationWakeLock"
            )?.apply {
                setReferenceCounted(false)
                acquire(24 * 60 * 60 * 1000L)
            }
        } catch (e: Exception) {
            Log.w(TAG, "WakeLock error: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.w(TAG, "WakeLock release error: ${e.message}")
        }
    }

    private fun startForegroundWithNotification(activeCount: Int, coords: String) {
        val notification = NotificationHelper.buildMonitoringNotification(this, activeCount, coords)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    /**
     * Ultra-fast multi-source location tracking:
     * Combines Google Fused Provider (500ms) + Native GPS + Native Network/Cell in parallel
     * so that entering a 5m, 10m, 15m, 20m radius triggers the alarm in real time!
     */
    @SuppressLint("MissingPermission")
    private fun setupUltraFastLocationTracking() {
        // 1. Google Play Services Fused Location Client
        try {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                500L // Fast 500ms update interval!
            ).apply {
                setMinUpdateIntervalMillis(0L) // Fastest possible rate (zero delay)
                setMinUpdateDistanceMeters(0f) // 0-meter distance filter (instant detection)
                setMaxUpdateDelayMillis(0L) // Zero batching latency
                setWaitForAccurateLocation(false)
            }.build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val location = result.lastLocation ?: return
                    processLocationUpdate(location)
                }
            }

            fusedLocationClient?.requestLocationUpdates(
                locationRequest,
                locationCallback as LocationCallback,
                Looper.getMainLooper()
            )

            fusedLocationClient?.lastLocation?.addOnSuccessListener { location ->
                if (location != null) {
                    processLocationUpdate(location)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fused location setup error: ${e.message}")
        }

        // 2. Parallel Native System Location Providers (BOTH GPS & Network running together!)
        setupParallelNativeLocationListeners()
    }

    @SuppressLint("MissingPermission")
    private fun setupParallelNativeLocationListeners() {
        try {
            locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return

            // Native GPS Listener (Satellite)
            gpsLocationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    processLocationUpdate(location)
                }
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            // Native Network Listener (Cell Towers + Wi-Fi) - instant lock indoors and on streets
            networkLocationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    _currentLocationFlow.value = location
                    // Only process trigger logic from network provider if accuracy is high
                    if (!location.hasAccuracy() || location.accuracy <= 10f) {
                        processLocationUpdate(location)
                    }
                }
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
                locationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    500L,
                    0f,
                    gpsLocationListener as LocationListener,
                    Looper.getMainLooper()
                )
            }

            if (locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true) {
                locationManager?.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    500L,
                    0f,
                    networkLocationListener as LocationListener,
                    Looper.getMainLooper()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Native location listener setup error: ${e.message}")
        }
    }

    private fun processLocationUpdate(location: Location) {
        _currentLocationFlow.value = location
        val coordsText = String.format("%.4f, %.4f", location.latitude, location.longitude)

        serviceScope.launch {
            val repository = GeoTaskApplication.instance.repository
            val reminders = repository.getActiveReminders()

            startForegroundWithNotification(reminders.size, coordsText)

            val now = System.currentTimeMillis()

            for (reminder in reminders) {
                // If reminder is already triggered, disabled, or marked handled in memory, skip completely!
                if (triggeredOrFinishedIds.contains(reminder.id) || !reminder.isEnabled || reminder.isTriggered) {
                    continue
                }

                if (reminder.snoozedUntil != null && reminder.snoozedUntil > now) {
                    continue
                }

                // If location fix has poor accuracy (e.g. 15-50m drift), do not trigger tight radius (e.g. 1-10m) false alarm!
                if (location.hasAccuracy() && reminder.radiusMeters <= 10f && location.accuracy > 8f) {
                    Log.d(TAG, "Skipping trigger check: fix accuracy (${location.accuracy}m) is too coarse for tight radius ${reminder.radiusMeters}m")
                    continue
                }

                val distanceResults = FloatArray(1)
                Location.distanceBetween(
                    location.latitude,
                    location.longitude,
                    reminder.latitude,
                    reminder.longitude,
                    distanceResults
                )
                val distanceMeters = distanceResults[0]

                // STRICT RADIUS: Distance must strictly be within user-specified radius!
                // If radius is 1m, user must be <= 1.0 meter away. No extra buffer!
                val isCurrentlyInside = distanceMeters <= reminder.radiusMeters
                val wasInside = previousInsideState[reminder.id] ?: false

                if (reminder.triggerType == "ENTER") {
                    if (isCurrentlyInside) {
                        Log.d(TAG, "STRICT ARRIVAL CONFIRMED: distance ${distanceMeters}m <= radius ${reminder.radiusMeters}m. Triggering alarm!")
                        // Lock state immediately so no subsequent update can re-trigger!
                        triggeredOrFinishedIds.add(reminder.id)
                        previousInsideState[reminder.id] = true
                        repository.updateTriggerStatus(reminder.id, isTriggered = true, timestamp = now)
                        triggerNotificationForReminder(reminder)
                    }
                } else if (reminder.triggerType == "EXIT") {
                    if (wasInside && !isCurrentlyInside) {
                        triggeredOrFinishedIds.add(reminder.id)
                        previousInsideState[reminder.id] = false
                        repository.updateTriggerStatus(reminder.id, isTriggered = true, timestamp = now)
                        triggerNotificationForReminder(reminder)
                    }
                }

                previousInsideState[reminder.id] = isCurrentlyInside
            }
        }
    }

    private suspend fun triggerNotificationForReminder(reminder: GeoReminder) {
        // Prevent duplicate trigger by Google Play Services geofence
        com.example.service.GoogleKeepGeofenceManager.getInstance(this).removeGeofence(reminder.id)
        val repository = GeoTaskApplication.instance.repository
        val tasks = repository.getTasksForReminder(reminder.id)
        NotificationHelper.showProximityNotification(this, reminder, tasks)
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        _isServiceRunning.value = false
        releaseWakeLock()
        locationCallback?.let { fusedLocationClient?.removeLocationUpdates(it) }
        gpsLocationListener?.let { locationManager?.removeUpdates(it) }
        networkLocationListener?.let { locationManager?.removeUpdates(it) }
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "LocationMonitoring"
        private const val NOTIFICATION_ID = 1001

        private var instance: LocationMonitoringService? = null

        const val ACTION_START_SERVICE = "com.example.geotask.START_SERVICE"
        const val ACTION_STOP_SERVICE = "com.example.geotask.STOP_SERVICE"
        const val ACTION_SIMULATE_LOCATION = "com.example.geotask.SIMULATE_LOCATION"
        const val EXTRA_LATITUDE = "extra_latitude"
        const val EXTRA_LONGITUDE = "extra_longitude"

        private val _currentLocationFlow = MutableStateFlow<Location?>(null)
        val currentLocationFlow = _currentLocationFlow.asStateFlow()

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning = _isServiceRunning.asStateFlow()

        fun markReminderFinished(reminderId: Long) {
            instance?.apply {
                triggeredOrFinishedIds.add(reminderId)
                previousInsideState[reminderId] = true
            }
        }

        fun resetReminderState(reminderId: Long) {
            instance?.apply {
                triggeredOrFinishedIds.remove(reminderId)
                previousInsideState.remove(reminderId)
            }
        }

        fun startService(context: Context) {
            val intent = Intent(context, LocationMonitoringService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, LocationMonitoringService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }

        fun simulateLocation(context: Context, latitude: Double, longitude: Double) {
            val intent = Intent(context, LocationMonitoringService::class.java).apply {
                action = ACTION_SIMULATE_LOCATION
                putExtra(EXTRA_LATITUDE, latitude)
                putExtra(EXTRA_LONGITUDE, longitude)
            }
            context.startService(intent)
        }
    }
}
