package com.example.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class ReminderWithTasks(
    @Embedded
    val reminder: GeoReminder,
    @Relation(
        parentColumn = "id",
        entityColumn = "reminderId"
    )
    val tasks: List<GeoTaskItem> = emptyList()
) {
    val totalTasks: Int get() = tasks.size
    val completedTasks: Int get() = tasks.count { it.isCompleted }
    val pendingTasks: Int get() = tasks.count { !it.isCompleted }
}
