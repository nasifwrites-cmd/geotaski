package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_presets")
data class SavedLocationPreset(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val defaultRadiusMeters: Float = 150f,
    val createdAt: Long = System.currentTimeMillis()
)
