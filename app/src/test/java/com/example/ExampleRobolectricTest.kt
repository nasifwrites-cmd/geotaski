package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.GeoReminder
import com.example.data.model.GeoTaskItem
import com.example.data.model.ReminderWithTasks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("GeoTask", appName)
    }

    @Test
    fun `test ReminderWithTasks pending and completed counts`() {
        val reminder = GeoReminder(
            id = 1,
            title = "Test Location",
            latitude = 23.79,
            longitude = 90.41,
            locationName = "Test Place"
        )
        val tasks = listOf(
            GeoTaskItem(id = 10, reminderId = 1, title = "Task 1", isCompleted = true),
            GeoTaskItem(id = 11, reminderId = 1, title = "Task 2", isCompleted = false),
            GeoTaskItem(id = 12, reminderId = 1, title = "Task 3", isCompleted = false)
        )
        val combined = ReminderWithTasks(reminder, tasks)

        assertEquals(3, combined.totalTasks)
        assertEquals(1, combined.completedTasks)
        assertEquals(2, combined.pendingTasks)
    }
}
