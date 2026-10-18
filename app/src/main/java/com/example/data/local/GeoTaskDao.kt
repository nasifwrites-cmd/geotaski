package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.GeoReminder
import com.example.data.model.GeoTaskItem
import com.example.data.model.ReminderWithTasks
import kotlinx.coroutines.flow.Flow

@Dao
interface GeoTaskDao {

    @Transaction
    @Query("SELECT * FROM geo_reminders ORDER BY createdAt DESC")
    fun getAllRemindersWithTasks(): Flow<List<ReminderWithTasks>>

    @Transaction
    @Query("SELECT * FROM geo_reminders WHERE id = :id")
    fun getReminderWithTasksById(id: Long): Flow<ReminderWithTasks?>

    @Query("SELECT * FROM geo_reminders WHERE isEnabled = 1")
    suspend fun getActiveReminders(): List<GeoReminder>

    @Query("SELECT * FROM geo_reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): GeoReminder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: GeoReminder): Long

    @Update
    suspend fun updateReminder(reminder: GeoReminder)

    @Delete
    suspend fun deleteReminder(reminder: GeoReminder)

    @Query("DELETE FROM geo_reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Long)

    @Query("UPDATE geo_reminders SET isTriggered = :isTriggered, lastTriggeredAt = :timestamp WHERE id = :id")
    suspend fun updateTriggerStatus(id: Long, isTriggered: Boolean, timestamp: Long)

    @Query("UPDATE geo_reminders SET snoozedUntil = :snoozedUntil WHERE id = :id")
    suspend fun setSnooze(id: Long, snoozedUntil: Long?)

    @Query("UPDATE geo_reminders SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun toggleReminderEnabled(id: Long, isEnabled: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: GeoTaskItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<GeoTaskItem>): List<Long>

    @Update
    suspend fun updateTask(task: GeoTaskItem)

    @Delete
    suspend fun deleteTask(task: GeoTaskItem)

    @Query("DELETE FROM geo_tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Long)

    @Query("UPDATE geo_tasks SET isCompleted = :isCompleted WHERE id = :taskId")
    suspend fun setTaskCompleted(taskId: Long, isCompleted: Boolean)

    @Query("DELETE FROM geo_tasks WHERE reminderId = :reminderId")
    suspend fun deleteTasksByReminderId(reminderId: Long)

    @Query("SELECT * FROM geo_tasks WHERE reminderId = :reminderId")
    suspend fun getTasksForReminder(reminderId: Long): List<GeoTaskItem>

    @Query("SELECT * FROM saved_presets ORDER BY createdAt DESC")
    fun getAllPresets(): Flow<List<com.example.data.model.SavedLocationPreset>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: com.example.data.model.SavedLocationPreset): Long

    @Query("DELETE FROM saved_presets WHERE id = :id")
    suspend fun deletePresetById(id: Long)
}
