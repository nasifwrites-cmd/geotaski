package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.service.NotificationHelper
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.GeoTaskTheme
import com.example.ui.viewmodel.GeoTaskViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: GeoTaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check if opened from notification tap
        val reminderId = intent?.getLongExtra(NotificationHelper.EXTRA_REMINDER_ID, -1L) ?: -1L
        if (reminderId != -1L) {
            // Can highlight or focus reminder
        }

        setContent {
            GeoTaskTheme {
                HomeScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
