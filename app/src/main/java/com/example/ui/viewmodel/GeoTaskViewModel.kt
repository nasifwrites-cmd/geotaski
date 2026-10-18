package com.example.ui.viewmodel

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.GeoTaskApplication
import com.example.data.model.GeoReminder
import com.example.data.model.ReminderWithTasks
import com.example.service.LocationMonitoringService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SimpleUiState(
    val reminders: List<ReminderWithTasks> = emptyList(),
    val currentLocation: Location? = null,
    val isAddOpen: Boolean = false,
    val isLocationActive: Boolean = false
)

class GeoTaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as GeoTaskApplication).repository
    private val _isAddOpen = MutableStateFlow(false)

    val uiState: StateFlow<SimpleUiState> = combine(
        repository.allRemindersWithTasks,
        LocationMonitoringService.currentLocationFlow,
        LocationMonitoringService.isServiceRunning,
        _isAddOpen
    ) { reminders, location, isRunning, isAddOpen ->
        SimpleUiState(
            reminders = reminders,
            currentLocation = location,
            isAddOpen = isAddOpen,
            isLocationActive = isRunning
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SimpleUiState()
    )

    init {
        // Automatically start the background location watcher
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

    fun addReminder(task: String, placeName: String, lat: Double, lng: Double, radiusMeters: Float = 150f) {
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
            // also toggle child tasks if any
            item.tasks.forEach {
                repository.setTaskCompleted(it.id, isNowTriggered)
            }
        }
    }

    fun deleteReminder(item: ReminderWithTasks) {
        viewModelScope.launch {
            repository.deleteReminder(item.reminder)
        }
    }

    fun testTrigger(item: ReminderWithTasks) {
        // Immediately simulate arriving at this spot so notification triggers
        LocationMonitoringService.simulateLocation(
            getApplication(),
            item.reminder.latitude,
            item.reminder.longitude
        )
    }

    fun openAddDialog() {
        _isAddOpen.value = true
    }

    fun closeAddDialog() {
        _isAddOpen.value = false
    }
}
