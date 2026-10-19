package com.example.ui.screens

import android.Manifest
import android.location.Location
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ReminderWithTasks
import com.example.ui.theme.StatusInsideGeofence
import com.example.ui.viewmodel.GeoTaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: GeoTaskViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedPresetForReminder by remember { mutableStateOf<com.example.data.model.SavedLocationPreset?>(null) }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionsLauncher.launch(permissions.toTypedArray())
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "GeoTask",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    // Formal "Save Location" button
                    Surface(
                        onClick = { viewModel.openSaveCurrentSpotDialog() },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.testTag("save_current_spot_top_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Save Spot",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Clean formal GPS status pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(StatusInsideGeofence)
                        )
                        Text(
                            text = "GPS Active",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_reminder_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                    Text("New Reminder", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Urgent continuous alarm banner when sound is ringing
            if (uiState.isAlarmActive) {
                Surface(
                    color = MaterialTheme.colorScheme.error,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("alarm_ringing_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Loud Alarm Active",
                                    color = MaterialTheme.colorScheme.onError,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Arrived at: ${uiState.alarmLocationTitle.ifBlank { "Target Location" }}",
                                    color = MaterialTheme.colorScheme.onError.copy(alpha = 0.9f),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.stopAlarmAndFinish(uiState.ringingReminderId) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onError,
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("stop_alarm_banner_btn")
                        ) {
                            Text("STOP & FINISH", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Saved Presets Row
            if (uiState.savedPresets.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Saved Presets",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(uiState.savedPresets, key = { it.id }) { preset ->
                            Surface(
                                onClick = {
                                    selectedPresetForReminder = preset
                                    viewModel.openAddDialog()
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bookmark,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${preset.name} (${preset.defaultRadiusMeters.toInt()}m)",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                    IconButton(
                                        onClick = { viewModel.deleteCustomPreset(preset.id) },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Delete Preset",
                                            modifier = Modifier.size(10.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (uiState.reminders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No location tasks yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tap 'New Reminder' to configure an alert for a destination",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = uiState.reminders,
                        key = { it.reminder.id }
                    ) { item ->
                        val isCardRinging = uiState.isAlarmActive && uiState.ringingReminderId == item.reminder.id
                        SimpleReminderCard(
                            item = item,
                            currentLocation = uiState.currentLocation,
                            isRinging = isCardRinging,
                            onToggle = { viewModel.toggleReminder(item) },
                            onTest = { viewModel.testTrigger(item) },
                            onStopAlarm = { viewModel.stopAlarmAndFinish(item.reminder.id) },
                            onDelete = { viewModel.deleteReminder(item) }
                        )
                    }

                    // Subtle footer requested by user: "Made by Nasif"
                    item {
                        Text(
                            text = "Made by Nasif",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 18.dp, bottom = 48.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    if (uiState.isAddOpen) {
        AddSimpleReminderDialog(
            currentLocation = uiState.currentLocation,
            savedPresets = uiState.savedPresets,
            initialPreset = selectedPresetForReminder,
            isAcquiringLocation = uiState.isAcquiringLocation,
            onRequestPreciseLocation = { callback ->
                viewModel.requestPreciseLocation(callback)
            },
            onDismiss = {
                selectedPresetForReminder = null
                viewModel.closeAddDialog()
            },
            onSave = { task, placeName, lat, lng, radius, alsoSavePreset ->
                selectedPresetForReminder = null
                viewModel.addReminder(task, placeName, lat, lng, radius)
                if (alsoSavePreset) {
                    viewModel.saveCustomPreset(placeName, lat, lng, radius)
                }
            }
        )
    }

    if (uiState.isSaveCurrentSpotOpen) {
        SaveCurrentLocationPresetDialog(
            currentLocation = uiState.currentLocation,
            isAcquiringLocation = uiState.isAcquiringLocation,
            onRequestPreciseLocation = { callback ->
                viewModel.requestPreciseLocation(callback)
            },
            onDismiss = { viewModel.closeSaveCurrentSpotDialog() },
            onSave = { name, lat, lng, radius ->
                viewModel.saveCustomPreset(name, lat, lng, radius)
            }
        )
    }
}

@Composable
private fun SimpleReminderCard(
    item: ReminderWithTasks,
    currentLocation: Location?,
    isRinging: Boolean = false,
    onToggle: () -> Unit,
    onTest: () -> Unit,
    onStopAlarm: () -> Unit,
    onDelete: () -> Unit
) {
    val isDone = item.reminder.isTriggered || item.tasks.all { it.isCompleted && it.title.isNotBlank() }

    val distanceInfo = remember(item.reminder, currentLocation) {
        if (currentLocation == null) {
            "Nearby"
        } else {
            val results = FloatArray(1)
            Location.distanceBetween(
                currentLocation.latitude,
                currentLocation.longitude,
                item.reminder.latitude,
                item.reminder.longitude,
                results
            )
            val d = results[0]
            if (d <= item.reminder.radiusMeters) {
                "Inside geofence"
            } else if (d < 1000) {
                "${d.toInt()}m away"
            } else {
                String.format("%.1fkm away", d / 1000f)
            }
        }
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRinging)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            else if (isDone)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            else
                MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            if (isRinging) 2.dp else 1.dp,
            if (isRinging) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reminder_card_${item.reminder.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isDone,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .size(36.dp)
                    .testTag("checkbox_${item.reminder.id}")
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.reminder.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (isDone)
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${item.reminder.locationName} (${item.reminder.radiusMeters.toInt()}m)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "•",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = distanceInfo,
                        fontSize = 12.sp,
                        color = if (distanceInfo == "Inside geofence")
                            StatusInsideGeofence
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (distanceInfo == "Inside geofence") FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isRinging) {
                    Button(
                        onClick = onStopAlarm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("card_stop_alarm_btn_${item.reminder.id}")
                    ) {
                        Text("STOP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Surface(
                        onClick = onTest,
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.testTag("test_btn_${item.reminder.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Test Alarm",
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Test",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("delete_btn_${item.reminder.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
