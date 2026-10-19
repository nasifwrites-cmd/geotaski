package com.example.ui.screens

import android.location.Location
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.SavedLocationPreset
import com.example.ui.theme.StatusInsideGeofence
import java.util.Locale

private data class PredefinedPlace(
    val name: String,
    val lat: Double,
    val lng: Double
)

private val DEFAULT_PLACES = listOf(
    PredefinedPlace("Supermarket", 23.7937, 90.4066),
    PredefinedPlace("Office", 23.7806, 90.4152),
    PredefinedPlace("Home", 23.7950, 90.4120),
    PredefinedPlace("Pharmacy", 23.7990, 90.4100)
)

private val STANDARD_RADII = listOf(5, 10, 15, 20)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddSimpleReminderDialog(
    currentLocation: Location?,
    savedPresets: List<SavedLocationPreset>,
    initialPreset: SavedLocationPreset? = null,
    isAcquiringLocation: Boolean = false,
    onRequestPreciseLocation: ((Location) -> Unit) -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (task: String, placeName: String, lat: Double, lng: Double, radiusMeters: Float, alsoSavePreset: Boolean) -> Unit
) {
    var taskText by remember { mutableStateOf("") }

    // 0: Current GPS, 1: Saved Places
    var selectedLocationMode by remember {
        mutableIntStateOf(if (initialPreset != null) 1 else 0)
    }

    var placeName by remember {
        mutableStateOf(initialPreset?.name ?: "Current Spot")
    }

    var currentLat by remember {
        mutableDoubleStateOf(initialPreset?.latitude ?: currentLocation?.latitude ?: 0.0)
    }
    var currentLng by remember {
        mutableDoubleStateOf(initialPreset?.longitude ?: currentLocation?.longitude ?: 0.0)
    }
    var currentAccuracy by remember {
        mutableFloatStateOf(currentLocation?.accuracy ?: -1f)
    }

    var selectedRadius by remember {
        mutableIntStateOf(initialPreset?.defaultRadiusMeters?.toInt() ?: 10)
    }
    var customRadiusText by remember { mutableStateOf("") }
    var isCustomRadiusActive by remember { mutableStateOf(false) }

    var alsoSavePreset by remember { mutableStateOf(false) }
    var showCoordinatesDetail by remember { mutableStateOf(false) }

    // Trigger initial precise GPS lock if opening in Current Spot mode without coordinates
    LaunchedEffect(Unit) {
        if (selectedLocationMode == 0 && (currentLat == 0.0 || currentLng == 0.0)) {
            onRequestPreciseLocation { loc ->
                currentLat = loc.latitude
                currentLng = loc.longitude
                currentAccuracy = loc.accuracy
            }
        }
    }

    LaunchedEffect(currentLocation) {
        if (currentLocation != null && selectedLocationMode == 0 && (currentLat == 0.0 || currentLng == 0.0)) {
            currentLat = currentLocation.latitude
            currentLng = currentLocation.longitude
            currentAccuracy = currentLocation.accuracy
        }
    }

    val effectiveRadius = if (isCustomRadiusActive) {
        customRadiusText.toIntOrNull() ?: selectedRadius
    } else {
        selectedRadius
    }

    val hasCoordinates = currentLat != 0.0 && currentLng != 0.0
    val isReadyToSave = taskText.isNotBlank() && placeName.isNotBlank() && hasCoordinates

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 680.dp)
        ) {
            LazyColumn(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "New Reminder",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Alarm triggers when you arrive within radius",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("dialog_close_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                // 1. Task Input
                item {
                    OutlinedTextField(
                        value = taskText,
                        onValueChange = { taskText = it },
                        label = { Text("Task Description") },
                        placeholder = { Text("e.g. Pick up documents, buy coffee") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("task_input_field"),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                // 2. Location Section with Clean Segmented Tabs
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Destination Place",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        TabRow(
                            selectedTabIndex = selectedLocationMode,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            divider = {},
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .height(40.dp)
                        ) {
                            Tab(
                                selected = selectedLocationMode == 0,
                                onClick = {
                                    selectedLocationMode = 0
                                    if (placeName.isBlank() || placeName == "Supermarket" || placeName == "Office" || placeName == "Home") {
                                        placeName = "Current Spot"
                                    }
                                    if (currentLocation != null) {
                                        currentLat = currentLocation.latitude
                                        currentLng = currentLocation.longitude
                                        currentAccuracy = currentLocation.accuracy
                                    } else {
                                        onRequestPreciseLocation { loc ->
                                            currentLat = loc.latitude
                                            currentLng = loc.longitude
                                            currentAccuracy = loc.accuracy
                                        }
                                    }
                                },
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MyLocation,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text("Current Spot", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            )

                            Tab(
                                selected = selectedLocationMode == 1,
                                onClick = { selectedLocationMode = 1 },
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bookmark,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text("Saved Places", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            )
                        }

                        // Mode 0: Current Spot (Effortless 1-tap lock)
                        if (selectedLocationMode == 0) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
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
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(if (hasCoordinates) StatusInsideGeofence else MaterialTheme.colorScheme.error)
                                        )
                                        Column {
                                            Text(
                                                text = if (isAcquiringLocation)
                                                    "Acquiring high-accuracy GPS..."
                                                else if (hasCoordinates)
                                                    "GPS Position Locked"
                                                else
                                                    "Waiting for GPS coordinates...",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (currentAccuracy > 0)
                                                    "Precision: ±${currentAccuracy.toInt()} meters"
                                                else if (hasCoordinates)
                                                    "Coordinates available"
                                                else
                                                    "Tap refresh button to lock GPS",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            onRequestPreciseLocation { loc ->
                                                currentLat = loc.latitude
                                                currentLng = loc.longitude
                                                currentAccuracy = loc.accuracy
                                            }
                                        },
                                        enabled = !isAcquiringLocation,
                                        modifier = Modifier.testTag("btn_get_exact_gps")
                                    ) {
                                        if (isAcquiringLocation) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Refresh GPS",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = placeName,
                                onValueChange = { placeName = it },
                                label = { Text("Place Label") },
                                placeholder = { Text("e.g. My Current Spot, Corner Shop, Office") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("manual_place_name"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Checkbox(
                                    checked = alsoSavePreset,
                                    onCheckedChange = { alsoSavePreset = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Text(
                                    text = "Save to My Places for future use",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Mode 1: Saved Places (Pick with 1 tap)
                        if (selectedLocationMode == 1) {
                            val allPlaces = savedPresets.map {
                                PredefinedPlace(it.name, it.latitude, it.longitude)
                            } + DEFAULT_PLACES

                            Text(
                                text = "Select a saved destination:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                allPlaces.forEach { place ->
                                    val isSelected = placeName == place.name && currentLat == place.lat && currentLng == place.lng
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            placeName = place.name
                                            currentLat = place.lat
                                            currentLng = place.lng
                                        },
                                        leadingIcon = {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.LocationOn,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        },
                                        label = { Text(place.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                }
                            }
                        }

                        // Subtle expandable coordinates view for power users
                        if (hasCoordinates) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { showCoordinatesDetail = !showCoordinatesDetail }
                                ) {
                                    Text(
                                        text = if (showCoordinatesDetail) "Hide Coordinates" else "View Coordinates",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = String.format(Locale.US, "%.4f, %.4f", currentLat, currentLng),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }

                            AnimatedVisibility(visible = showCoordinatesDetail) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Lat: $currentLat\nLng: $currentLng",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Proximity Radius (5m, 10m, 15m, 20m Segmented Buttons)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Proximity Radius",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(2.dp)
                            ) {
                                Text(
                                    text = "${effectiveRadius}m radius",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // 4 Big Clean Segmented Pills: 5m, 10m, 15m, 20m
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            STANDARD_RADII.forEach { r ->
                                val isSelected = !isCustomRadiusActive && selectedRadius == r
                                Surface(
                                    onClick = {
                                        selectedRadius = r
                                        isCustomRadiusActive = false
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "${r}m",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (r == 10) "Default" else if (r == 5) "Tight" else if (r == 20) "Wide" else "Standard",
                                            fontSize = 9.sp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Custom radius expander toggle if needed
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Loud buzzing alert triggers when entering this radius",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            TextButton(
                                onClick = { isCustomRadiusActive = !isCustomRadiusActive }
                            ) {
                                Text(
                                    text = if (isCustomRadiusActive) "Use Presets" else "Custom...",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        AnimatedVisibility(visible = isCustomRadiusActive) {
                            OutlinedTextField(
                                value = customRadiusText,
                                onValueChange = { customRadiusText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Custom Radius in meters") },
                                placeholder = { Text("e.g. 25, 50, 100") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("manual_radius_input"),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                // 4. Action Buttons
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                if (isReadyToSave) {
                                    onSave(
                                        taskText.trim(),
                                        placeName.trim(),
                                        currentLat,
                                        currentLng,
                                        effectiveRadius.toFloat().coerceAtLeast(3f),
                                        alsoSavePreset
                                    )
                                }
                            },
                            enabled = isReadyToSave,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_task_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Set Reminder", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Super smooth, 2-click dialog to save current spot as a named preset for future use
 */
@Composable
fun SaveCurrentLocationPresetDialog(
    currentLocation: Location?,
    isAcquiringLocation: Boolean = false,
    onRequestPreciseLocation: ((Location) -> Unit) -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (name: String, lat: Double, lng: Double, radius: Float) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedRadius by remember { mutableIntStateOf(10) }

    var lat by remember {
        mutableDoubleStateOf(currentLocation?.latitude ?: 0.0)
    }
    var lng by remember {
        mutableDoubleStateOf(currentLocation?.longitude ?: 0.0)
    }
    var accuracy by remember {
        mutableFloatStateOf(currentLocation?.accuracy ?: -1f)
    }
    var hasCaptured by remember { mutableStateOf(currentLocation != null && currentLocation.latitude != 0.0) }

    // Auto lock GPS if not available
    LaunchedEffect(Unit) {
        if (!hasCaptured) {
            onRequestPreciseLocation { loc ->
                lat = loc.latitude
                lng = loc.longitude
                accuracy = loc.accuracy
                hasCaptured = true
            }
        }
    }

    LaunchedEffect(currentLocation) {
        if (currentLocation != null && !hasCaptured) {
            lat = currentLocation.latitude
            lng = currentLocation.longitude
            accuracy = currentLocation.accuracy
            hasCaptured = true
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Save Spot",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Save current location to reuse in reminders",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // GPS Lock status card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
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
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (hasCaptured) StatusInsideGeofence else MaterialTheme.colorScheme.error)
                            )
                            Column {
                                Text(
                                    text = if (isAcquiringLocation)
                                        "Acquiring precise GPS lock..."
                                    else if (hasCaptured)
                                        "Exact Location Locked"
                                    else
                                        "Waiting for GPS...",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (accuracy > 0)
                                        "Accuracy: ±${accuracy.toInt()} meters (High Precision)"
                                    else if (hasCaptured)
                                        String.format(Locale.US, "%.5f, %.5f", lat, lng)
                                    else
                                        "Tap refresh to acquire",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                onRequestPreciseLocation { loc ->
                                    lat = loc.latitude
                                    lng = loc.longitude
                                    accuracy = loc.accuracy
                                    hasCaptured = true
                                }
                            },
                            enabled = !isAcquiringLocation,
                            modifier = Modifier.testTag("save_spot_capture_gps_btn")
                        ) {
                            if (isAcquiringLocation) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh GPS",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Place name input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Place Name") },
                    placeholder = { Text("e.g. Office, Gym, Home, Grocery Store") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_spot_name_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                // 4 Clean Radius Pills
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Default Radius",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        STANDARD_RADII.forEach { r ->
                            val isSelected = selectedRadius == r
                            Surface(
                                onClick = { selectedRadius = r },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "${r}m",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp)
                                )
                            }
                        }
                    }
                }

                // Actions
                val canSave = name.isNotBlank() && hasCaptured && lat != 0.0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (canSave) {
                                onSave(name.trim(), lat, lng, selectedRadius.toFloat())
                            }
                        },
                        enabled = canSave,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_spot_confirm_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Save Place", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
