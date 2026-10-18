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

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var locationManager: LocationManager? = null
    private var nativeLocationListener: LocationListener? = null
    private var wakeLock: PowerManager.WakeLock? = null

    // Track state for each reminder
    private val previousInsideState = mutableMapOf<Long, Boolean>()

    override fun onCreate() {
        super.onCreate()
        _isServiceRunning.value = true

        // Acquire WakeLock so CPU keeps processing GPS even when screen is locked/off
        acquireWakeLock()

        startForegroundWithNotification(0, "Active")
        setupHighFrequencyLocationTracking()
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
                }
                processLocationUpdate(mockLoc)
            }
        }
        // START_STICKY ensures OS automatically recreates service if killed
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // When user swipes away / closes app from Recent Apps, restart service immediately
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
                "GeoTask::PersistentLocationWakeLock"
            )?.apply {
                setReferenceCounted(false)
                acquire(24 * 60 * 60 * 1000L) // Safe 24-hour lease
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
     * Configures ultra-fast, high-precision GPS tracking (1 second interval, 0m distance filter)
     * so that arriving at 5m, 10m, 15m, 20m triggers the alarm immediately.
     */
    @SuppressLint("MissingPermission")
    private fun setupHighFrequencyLocationTracking() {
        try {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                1000L // update every 1 second!
            ).apply {
                setMinUpdateIntervalMillis(500L) // Fastest update 500ms
                setMinUpdateDistanceMeters(0f) // Sub-meter sensitivity
                setMaxUpdateDelayMillis(1000L) // Deliver immediately, no batching
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

            // Request last known position immediately
            fusedLocationClient?.lastLocation?.addOnSuccessListener { location ->
                if (location != null) {
                    processLocationUpdate(location)
                }
            }

            // Also keep native GPS provider active as parallel fallback
            setupNativeLocationListener()

        } catch (e: Exception) {
            Log.e(TAG, "Fused location setup error: ${e.message}")
            setupNativeLocationListener()
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupNativeLocationListener() {
        try {
            locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            nativeLocationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    processLocationUpdate(location)
                }
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            val isGpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
            if (isGpsEnabled) {
                locationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L,
                    0f,
                    nativeLocationListener as LocationListener,
                    Looper.getMainLooper()
                )
            } else {
                locationManager?.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    1000L,
                    0f,
                    nativeLocationListener as LocationListener,
                    Looper.getMainLooper()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Native location listener error: ${e.message}")
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
                if (reminder.snoozedUntil != null && reminder.snoozedUntil > now) {
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
                val isCurrentlyInside = distanceMeters <= reminder.radiusMeters
                val wasInside = previousInsideState[reminder.id] ?: false

                if (reminder.triggerType == "ENTER") {
                    // Trigger alarm immediately as soon as user enters the radius!
                    val isEnteringNow = !wasInside && isCurrentlyInside
                    val isInsideNotTriggered = isCurrentlyInside && !reminder.isTriggered

                    if (isEnteringNow || isInsideNotTriggered) {
                        triggerNotificationForReminder(reminder)
                        repository.updateTriggerStatus(reminder.id, isTriggered = true, timestamp = now)
                    }
                } else if (reminder.triggerType == "EXIT") {
                    if (wasInside && !isCurrentlyInside) {
                        triggerNotificationForReminder(reminder)
                        repository.updateTriggerStatus(reminder.id, isTriggered = true, timestamp = now)
                    }
                }

                previousInsideState[reminder.id] = isCurrentlyInside
            }
        }
    }

    private suspend fun triggerNotificationForReminder(reminder: GeoReminder) {
        val repository = GeoTaskApplication.instance.repository
        val tasks = repository.getTasksForReminder(reminder.id)
        NotificationHelper.showProximityNotification(this, reminder, tasks)
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceRunning.value = false
        releaseWakeLock()
        locationCallback?.let { fusedLocationClient?.removeLocationUpdates(it) }
        nativeLocationListener?.let { locationManager?.removeUpdates(it) }
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "LocationMonitoring"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START_SERVICE = "com.example.geotask.START_SERVICE"
        const val ACTION_STOP_SERVICE = "com.example.geotask.STOP_SERVICE"
        const val ACTION_SIMULATE_LOCATION = "com.example.geotask.SIMULATE_LOCATION"
        const val EXTRA_LATITUDE = "extra_latitude"
        const val EXTRA_LONGITUDE = "extra_longitude"

        private val _currentLocationFlow = MutableStateFlow<Location?>(null)
        val currentLocationFlow = _currentLocationFlow.asStateFlow()

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning = _isServiceRunning.asStateFlow()

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
