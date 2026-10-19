package com.example.data.repository

import com.example.data.local.GeoTaskDao
import com.example.data.model.GeoReminder
import com.example.data.model.GeoTaskItem
import com.example.data.model.ReminderWithTasks
import kotlinx.coroutines.flow.Flow

class GeoReminderRepository(private val dao: GeoTaskDao) {

    val allRemindersWithTasks: Flow<List<ReminderWithTasks>> = dao.getAllRemindersWithTasks()

    fun getReminderWithTasksById(id: Long): Flow<ReminderWithTasks?> = dao.getReminderWithTasksById(id)

    suspend fun getActiveReminders(): List<GeoReminder> = dao.getActiveReminders()

    suspend fun getReminderById(id: Long): GeoReminder? = dao.getReminderById(id)

    suspend fun insertReminderWithTasks(
        reminder: GeoReminder,
        taskTitles: List<String>
    ): Long {
        val reminderId = dao.insertReminder(reminder)
        val validTasks = taskTitles
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { title ->
                GeoTaskItem(
                    reminderId = reminderId,
                    title = title,
                    isCompleted = false
                )
            }
        if (validTasks.isNotEmpty()) {
            dao.insertTasks(validTasks)
        }
        return reminderId
    }

    suspend fun updateReminderWithTasks(
        reminder: GeoReminder,
        newTasks: List<String>
    ) {
        dao.updateReminder(reminder)
        dao.deleteTasksByReminderId(reminder.id)
        val tasksToInsert = newTasks
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { title ->
                GeoTaskItem(
                    reminderId = reminder.id,
                    title = title,
                    isCompleted = false
                )
            }
        if (tasksToInsert.isNotEmpty()) {
            dao.insertTasks(tasksToInsert)
        }
    }

    suspend fun deleteReminder(reminder: GeoReminder) {
        dao.deleteReminder(reminder)
    }

    suspend fun deleteReminderById(id: Long) {
        dao.deleteReminderById(id)
    }

    suspend fun toggleReminderEnabled(id: Long, isEnabled: Boolean) {
        dao.toggleReminderEnabled(id, isEnabled)
    }

    suspend fun setTaskCompleted(taskId: Long, isCompleted: Boolean) {
        dao.setTaskCompleted(taskId, isCompleted)
    }

    suspend fun addTaskToReminder(reminderId: Long, title: String): Long {
        return dao.insertTask(
            GeoTaskItem(
                reminderId = reminderId,
                title = title.trim(),
                isCompleted = false
            )
        )
    }

    suspend fun deleteTask(taskId: Long) {
        dao.deleteTaskById(taskId)
    }

    suspend fun updateTriggerStatus(id: Long, isTriggered: Boolean, timestamp: Long = System.currentTimeMillis()) {
        dao.updateTriggerStatus(id, isTriggered, timestamp)
    }

    suspend fun snoozeReminder(id: Long, minutes: Int = 15) {
        val snoozeTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
        dao.setSnooze(id, snoozeTime)
    }

    suspend fun getTasksForReminder(reminderId: Long): List<GeoTaskItem> {
        return dao.getTasksForReminder(reminderId)
    }

    suspend fun finishReminder(reminderId: Long) {
        dao.finishReminder(reminderId)
        val tasks = dao.getTasksForReminder(reminderId)
        tasks.forEach {
            dao.setTaskCompleted(it.id, true)
        }
    }

    val allPresets: Flow<List<com.example.data.model.SavedLocationPreset>> = dao.getAllPresets()

    suspend fun savePreset(name: String, lat: Double, lng: Double, radius: Float = 5f): Long {
        return dao.insertPreset(
            com.example.data.model.SavedLocationPreset(
                name = name.trim(),
                latitude = lat,
                longitude = lng,
                defaultRadiusMeters = radius
            )
        )
    }

    suspend fun deletePreset(id: Long) {
        dao.deletePresetById(id)
    }
}
