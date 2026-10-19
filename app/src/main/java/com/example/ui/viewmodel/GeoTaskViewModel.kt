package com.example.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.GeoTaskApplication
import com.example.data.model.GeoReminder
import com.example.data.model.ReminderWithTasks
import com.example.data.model.SavedLocationPreset
import com.example.service.BuzzAlertHelper
import com.example.service.LocationMonitoringService
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SimpleUiState(
    val reminders: List<ReminderWithTasks> = emptyList(),
    val savedPresets: List<SavedLocationPreset> = emptyList(),
    val currentLocation: Location? = null,
    val isAddOpen: Boolean = false,
    val isSaveCurrentSpotOpen: Boolean = false,
    val isLocationActive: Boolean = false,
    val isAlarmActive: Boolean = false,
    val alarmLocationTitle: String = "",
    val ringingReminderId: Long = -1L,
    val isAcquiringLocation: Boolean = false
)

class GeoTaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as GeoTaskApplication).repository
    private val _isAddOpen = MutableStateFlow(false)
    private val _isSaveCurrentSpotOpen = MutableStateFlow(false)
    private val _isAcquiringLocation = MutableStateFlow(false)

    val uiState: StateFlow<SimpleUiState> = combine(
        repository.allRemindersWithTasks,
        repository.allPresets,
        LocationMonitoringService.currentLocationFlow,
        LocationMonitoringService.isServiceRunning,
        _isAddOpen,
        _isSaveCurrentSpotOpen,
        BuzzAlertHelper.isAlarmActive,
        BuzzAlertHelper.alarmLocationTitle,
        BuzzAlertHelper.ringingReminderId,
        _isAcquiringLocation
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        SimpleUiState(
            reminders = args[0] as List<ReminderWithTasks>,
            savedPresets = args[1] as List<SavedLocationPreset>,
            currentLocation = args[2] as Location?,
            isLocationActive = args[3] as Boolean,
            isAddOpen = args[4] as Boolean,
            isSaveCurrentSpotOpen = args[5] as Boolean,
            isAlarmActive = args[6] as Boolean,
            alarmLocationTitle = args[7] as String,
            ringingReminderId = args[8] as Long,
            isAcquiringLocation = args[9] as Boolean
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SimpleUiState()
    )

    init {
        LocationMonitoringService.startService(application)
        seedInitialSimpleTasks()
    }

    private fun seedInitialSimpleTasks() {
        viewModelScope.launch {
            val existing = repository.getActiveReminders()
            if (existing.isEmpty()) {
                repository.insertReminderWithTasks(
                    GeoReminder(
                        title = "Buy milk & coffee beans",
                        latitude = 23.7937,
                        longitude = 90.4066,
                        locationName = "City Supermarket",
                        radiusMeters = 10f
                    ),
                    listOf("Milk", "Coffee beans")
                )
                repository.insertReminderWithTasks(
                    GeoReminder(
                        title = "Submit project sprint report",
                        latitude = 23.7806,
                        longitude = 90.4152,
                        locationName = "Tech Hub Office",
                        radiusMeters = 15f
                    ),
                    listOf("Sprint report")
                )
            }
        }
    }

    fun addReminder(task: String, placeName: String, lat: Double, lng: Double, radiusMeters: Float = 10f) {
        if (task.isBlank() || placeName.isBlank()) return
        viewModelScope.launch {
            val reminder = GeoReminder(
                title = task.trim(),
                latitude = lat,
                longitude = lng,
                locationName = placeName.trim(),
                radiusMeters = radiusMeters,
                triggerType = "ENTER",
                isEnabled = true
            )
            val newId = repository.insertReminderWithTasks(reminder, listOf(task.trim()))
            com.example.service.GoogleKeepGeofenceManager.getInstance(getApplication())
                .registerGeofence(reminder.copy(id = newId))
            _isAddOpen.value = false
        }
    }

    fun toggleReminder(item: ReminderWithTasks) {
        viewModelScope.launch {
            val isNowTriggered = !item.reminder.isTriggered
            if (isNowTriggered) {
                // User marked task as done
                com.example.service.GoogleKeepGeofenceManager.getInstance(getApplication())
                    .removeGeofence(item.reminder.id)
                LocationMonitoringService.markReminderFinished(item.reminder.id)
                repository.finishReminder(item.reminder.id)
                stopAlarmAndFinish(item.reminder.id)
            } else {
                // User un-checked to re-arm reminder
                LocationMonitoringService.resetReminderState(item.reminder.id)
                repository.updateTriggerStatus(item.reminder.id, isTriggered = false)
                repository.toggleReminderEnabled(item.reminder.id, isEnabled = true)
                item.tasks.forEach {
                    repository.setTaskCompleted(it.id, false)
                }
                com.example.service.GoogleKeepGeofenceManager.getInstance(getApplication())
                    .registerGeofence(item.reminder.copy(isTriggered = false, isEnabled = true))
            }
        }
    }

    fun deleteReminder(item: ReminderWithTasks) {
        viewModelScope.launch {
            com.example.service.GoogleKeepGeofenceManager.getInstance(getApplication())
                .removeGeofence(item.reminder.id)
            LocationMonitoringService.markReminderFinished(item.reminder.id)
            stopAlarmAndFinish(item.reminder.id)
            repository.deleteReminder(item.reminder)
        }
    }

    fun testTrigger(item: ReminderWithTasks) {
        // Starts continuous looping buzzing alarm and simulates arrival
        BuzzAlertHelper.startContinuousAlarm(getApplication(), item.reminder.locationName, item.reminder.id)
        LocationMonitoringService.simulateLocation(
            getApplication(),
            item.reminder.latitude,
            item.reminder.longitude
        )
    }

    fun stopAlarmAndFinish(reminderId: Long? = null) {
        val targetId = if (reminderId != null && reminderId != -1L) {
            reminderId
        } else {
            BuzzAlertHelper.ringingReminderId.value
        }
        BuzzAlertHelper.stopAlarm(getApplication())
        if (targetId != -1L) {
            com.example.service.GoogleKeepGeofenceManager.getInstance(getApplication())
                .removeGeofence(targetId)
            LocationMonitoringService.markReminderFinished(targetId)
            viewModelScope.launch {
                repository.finishReminder(targetId)
            }
        }
    }

    fun stopAlarm(reminderId: Long? = null) {
        stopAlarmAndFinish(reminderId)
    }

    fun saveCustomPreset(name: String, lat: Double, lng: Double, radius: Float = 10f) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.savePreset(name.trim(), lat, lng, radius)
            _isSaveCurrentSpotOpen.value = false
        }
    }

    fun deleteCustomPreset(id: Long) {
        viewModelScope.launch {
            repository.deletePreset(id)
        }
    }

    fun openAddDialog() {
        _isAddOpen.value = true
    }

    fun closeAddDialog() {
        _isAddOpen.value = false
    }

    fun openSaveCurrentSpotDialog() {
        _isSaveCurrentSpotOpen.value = true
    }

    fun closeSaveCurrentSpotDialog() {
        _isSaveCurrentSpotOpen.value = false
    }

    @SuppressLint("MissingPermission")
    fun requestPreciseLocation(onLocationAcquired: (Location) -> Unit) {
        _isAcquiringLocation.value = true
        val context = getApplication<Application>()
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()

            fusedClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cts.token
            ).addOnSuccessListener { loc ->
                if (loc != null) {
                    _isAcquiringLocation.value = false
                    onLocationAcquired(loc)
                } else {
                    fusedClient.lastLocation.addOnSuccessListener { lastFused ->
                        if (lastFused != null) {
                            _isAcquiringLocation.value = false
                            onLocationAcquired(lastFused)
                        } else {
                            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                            val fallback = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                                ?: lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                            _isAcquiringLocation.value = false
                            if (fallback != null) {
                                onLocationAcquired(fallback)
                            }
                        }
                    }.addOnFailureListener {
                        _isAcquiringLocation.value = false
                    }
                }
            }.addOnFailureListener {
                _isAcquiringLocation.value = false
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                val fallback = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (fallback != null) {
                    onLocationAcquired(fallback)
                }
            }
        } catch (e: Exception) {
            _isAcquiringLocation.value = false
        }
    }
}
