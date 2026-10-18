package com.example.ui.screens

import android.location.Location
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.SavedLocationPreset

private data class SimplePlace(
    val name: String,
    val lat: Double,
    val lng: Double
)

private val DEFAULT_PLACES = listOf(
    SimplePlace("Supermarket", 23.7937, 90.4066),
    SimplePlace("Office", 23.7806, 90.4152),
    SimplePlace("Home", 23.7950, 90.4120),
    SimplePlace("Pharmacy", 23.7990, 90.4100)
)

// Default radius options requested: 5m, 10m, 15m, 20m
private val RADIUS_PRESETS = listOf(5, 10, 15, 20)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddSimpleReminderDialog(
    currentLocation: Location?,
    savedPresets: List<SavedLocationPreset>,
    onDismiss: () -> Unit,
    onSave: (task: String, placeName: String, lat: Double, lng: Double, radiusMeters: Float, alsoSavePreset: Boolean) -> Unit
) {
    var taskText by remember { mutableStateOf("") }

    // Place selection
    var isManualLocation by remember { mutableStateOf(false) }
    var placeName by remember { mutableStateOf(DEFAULT_PLACES[0].name) }
    var latText by remember {
        mutableStateOf(
            currentLocation?.latitude?.toString() ?: DEFAULT_PLACES[0].lat.toString()
        )
    }
    var lngText by remember {
        mutableStateOf(
            currentLocation?.longitude?.toString() ?: DEFAULT_PLACES[0].lng.toString()
        )
    }

    // Default radius set to 10m
    var radiusMetersText by remember { mutableStateOf("10") }
    var alsoSavePreset by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 660.dp)
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
                                text = "New Location Reminder",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Continuous alert upon reaching proximity",
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

                // 1. Task input
                item {
                    OutlinedTextField(
                        value = taskText,
                        onValueChange = { taskText = it },
                        label = { Text("Task") },
                        placeholder = { Text("e.g. Collect documents, purchase groceries") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("task_input_field"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // 2. Location selection (Presets or Manual)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Destination Location",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedButton(
                                onClick = { isManualLocation = !isManualLocation },
                                modifier = Modifier.height(32.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EditLocation,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isManualLocation) "Presets" else "Custom Coordinates",
                                    fontSize = 11.sp
                                )
                            }
                        }

                        if (!isManualLocation) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Default presets
                                DEFAULT_PLACES.forEach { place ->
                                    val isSelected = placeName == place.name
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            placeName = place.name
                                            latText = place.lat.toString()
                                            lngText = place.lng.toString()
                                        },
                                        label = { Text(place.name) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }

                                // User-saved presets
                                savedPresets.forEach { preset ->
                                    val isSelected = placeName == preset.name
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            placeName = preset.name
                                            latText = preset.latitude.toString()
                                            lngText = preset.longitude.toString()
                                            radiusMetersText = preset.defaultRadiusMeters.toInt().toString()
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Bookmark,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        },
                                        label = { Text(preset.name) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    )
                                }

                                // Current Location option
                                currentLocation?.let { loc ->
                                    FilterChip(
                                        selected = placeName == "Current Position",
                                        onClick = {
                                            placeName = "Current Position"
                                            latText = String.format("%.6f", loc.latitude)
                                            lngText = String.format("%.6f", loc.longitude)
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.MyLocation,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        label = { Text("Current Position") }
                                    )
                                }
                            }
                        } else {
                            // Manual Location Inputs
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = placeName,
                                    onValueChange = { placeName = it },
                                    label = { Text("Location Name") },
                                    placeholder = { Text("e.g. Headquarters, Central Archive") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("manual_place_name"),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = latText,
                                        onValueChange = { latText = it },
                                        label = { Text("Latitude") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("manual_lat_input"),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    OutlinedTextField(
                                        value = lngText,
                                        onValueChange = { lngText = it },
                                        label = { Text("Longitude") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("manual_lng_input"),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }

                                currentLocation?.let { loc ->
                                    Button(
                                        onClick = {
                                            latText = String.format("%.6f", loc.latitude)
                                            lngText = String.format("%.6f", loc.longitude)
                                            if (placeName.isBlank()) placeName = "Current Position"
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MyLocation,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Use Current GPS Coordinates", fontSize = 12.sp)
                                    }
                                }

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
                                        text = "Save this location as a reusable preset",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Radius Option (5m, 10m, 15m, 20m + manual)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Proximity Radius",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${radiusMetersText.toIntOrNull() ?: 10} m",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Default requested options: 5m, 10m, 15m, 20m
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            RADIUS_PRESETS.forEach { r ->
                                val isSelected = radiusMetersText == r.toString()
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { radiusMetersText = r.toString() },
                                    label = { Text("${r} m") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }

                        OutlinedTextField(
                            value = radiusMetersText,
                            onValueChange = { radiusMetersText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Custom Radius (meters)") },
                            placeholder = { Text("e.g. 5, 10, 15, 20") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manual_radius_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // 4. Actions
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
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel")
                        }

                        val parsedLat = latText.toDoubleOrNull()
                        val parsedLng = lngText.toDoubleOrNull()
                        val parsedRadius = radiusMetersText.toFloatOrNull() ?: 10f
                        val isValid = taskText.isNotBlank() && placeName.isNotBlank() && parsedLat != null && parsedLng != null

                        Button(
                            onClick = {
                                if (isValid && parsedLat != null && parsedLng != null) {
                                    onSave(
                                        taskText,
                                        placeName,
                                        parsedLat,
                                        parsedLng,
                                        parsedRadius.coerceAtLeast(3f),
                                        alsoSavePreset
                                    )
                                }
                            },
                            enabled = isValid,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_task_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Set Reminder")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog to save current GPS location as a preset for future usage
 */
@Composable
fun SaveCurrentLocationPresetDialog(
    currentLocation: Location?,
    onDismiss: () -> Unit,
    onSave: (name: String, lat: Double, lng: Double, radius: Float) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var radiusText by remember { mutableStateOf("10") }

    val lat = currentLocation?.latitude ?: 23.7937
    val lng = currentLocation?.longitude ?: 90.4066

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Save Location Preset",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Save coordinates for future reminders",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "Coordinates: ${String.format("%.5f, %.5f", lat, lng)}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (currentLocation != null) "Accurate GPS fix" else "Default position",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Location Name") },
                    placeholder = { Text("e.g. Branch Office, Gym, Residence") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_spot_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = radiusText,
                    onValueChange = { radiusText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Default Radius (meters)") },
                    placeholder = { Text("5, 10, 15, 20") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(name, lat, lng, radiusText.toFloatOrNull() ?: 10f)
                            }
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_spot_confirm_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Location")
                    }
                }
            }
        }
    }
}
