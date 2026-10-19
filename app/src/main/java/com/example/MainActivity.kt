package com.example

import android.content.Intent
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

        handleIntent(intent)

        setContent {
            GeoTaskTheme {
                HomeScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val reminderId = intent?.getLongExtra(NotificationHelper.EXTRA_REMINDER_ID, -1L) ?: -1L
        val autoStop = intent?.getBooleanExtra(NotificationHelper.EXTRA_AUTO_STOP, false) ?: false
        if (reminderId != -1L && autoStop) {
            viewModel.stopAlarmAndFinish(reminderId)
        }
    }
}
