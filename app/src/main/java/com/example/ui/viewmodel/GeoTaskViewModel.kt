package com.example.ui.viewmodel

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.GeoTaskApplication
import com.example.data.model.GeoReminder
import com.example.data.model.ReminderWithTasks
import com.example.data.model.SavedLocationPreset
import com.example.service.BuzzAlertHelper
import com.example.service.LocationMonitoringService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    val alarmLocationTitle: String = ""
)

class GeoTaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as GeoTaskApplication).repository
    private val _isAddOpen = MutableStateFlow(false)
    private val _isSaveCurrentSpotOpen = MutableStateFlow(false)

    val uiState: StateFlow<SimpleUiState> = combine(
        repository.allRemindersWithTasks,
        repository.allPresets,
        LocationMonitoringService.currentLocationFlow,
        LocationMonitoringService.isServiceRunning,
        _isAddOpen,
        _isSaveCurrentSpotOpen,
        BuzzAlertHelper.isAlarmActive,
        BuzzAlertHelper.alarmLocationTitle
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
            alarmLocationTitle = args[7] as String
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
                        radiusMeters = 150f
                    ),
                    listOf("Milk", "Coffee beans")
                )
                repository.insertReminderWithTasks(
                    GeoReminder(
                        title = "Submit project sprint report",
                        latitude = 23.7806,
                        longitude = 90.4152,
                        locationName = "Tech Hub Office",
                        radiusMeters = 200f
                    ),
                    listOf("Sprint report")
                )
            }
        }
    }

    fun addReminder(task: String, placeName: String, lat: Double, lng: Double, radiusMeters: Float = 10f) {
        if (task.isBlank() || placeName.isBlank()) return
        viewModelScope.launch {
            repository.insertReminderWithTasks(
                GeoReminder(
                    title = task.trim(),
                    latitude = lat,
                    longitude = lng,
                    locationName = placeName.trim(),
                    radiusMeters = radiusMeters,
                    triggerType = "ENTER",
                    isEnabled = true
                ),
                listOf(task.trim())
            )
            _isAddOpen.value = false
        }
    }

    fun toggleReminder(item: ReminderWithTasks) {
        viewModelScope.launch {
            val isNowTriggered = !item.reminder.isTriggered
            repository.updateTriggerStatus(item.reminder.id, isNowTriggered)
            item.tasks.forEach {
                repository.setTaskCompleted(it.id, isNowTriggered)
            }
            if (isNowTriggered) {
                stopAlarm()
            }
        }
    }

    fun deleteReminder(item: ReminderWithTasks) {
        viewModelScope.launch {
            repository.deleteReminder(item.reminder)
        }
    }

    fun testTrigger(item: ReminderWithTasks) {
        // Starts continuous looping buzzing alarm that won't stop until manually clicked
        BuzzAlertHelper.startContinuousAlarm(getApplication(), item.reminder.locationName)
        LocationMonitoringService.simulateLocation(
            getApplication(),
            item.reminder.latitude,
            item.reminder.longitude
        )
    }

    fun stopAlarm() {
        BuzzAlertHelper.stopAlarm(getApplication())
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
}
