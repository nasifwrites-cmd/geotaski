package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "geo_reminders")
data class GeoReminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val latitude: Double,
    val longitude: Double,
    val locationName: String,
    val radiusMeters: Float = 5f,
    /**
     * "ENTER" - Trigger when entering the geographic radius
     * "EXIT" - Trigger when leaving the geographic radius
     */
    val triggerType: String = "ENTER",
    val isEnabled: Boolean = true,
    val isTriggered: Boolean = false,
    val lastTriggeredAt: Long? = null,
    val snoozedUntil: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
