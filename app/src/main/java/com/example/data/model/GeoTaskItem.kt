package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "geo_tasks",
    foreignKeys = [
        ForeignKey(
            entity = GeoReminder::class,
            parentColumns = ["id"],
            childColumns = ["reminderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["reminderId"])]
)
data class GeoTaskItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val reminderId: Long,
    val title: String,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
