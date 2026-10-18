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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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

private data class SimplePlace(
    val name: String,
    val emoji: String,
    val lat: Double,
    val lng: Double
)

private val QUICK_PLACES = listOf(
    SimplePlace("Supermarket", "🛒", 23.7937, 90.4066),
    SimplePlace("Office", "🏢", 23.7806, 90.4152),
    SimplePlace("Home", "🏠", 23.7950, 90.4120),
    SimplePlace("Pharmacy", "💊", 23.7990, 90.4100)
)

private val RADIUS_PRESETS = listOf(50, 100, 200, 500, 1000)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddSimpleReminderDialog(
    currentLocation: Location?,
    onDismiss: () -> Unit,
    onSave: (task: String, placeName: String, lat: Double, lng: Double, radiusMeters: Float) -> Unit
) {
    var taskText by remember { mutableStateOf("") }

    // Place state
    var isManualLocation by remember { mutableStateOf(false) }
    var placeName by remember { mutableStateOf("Supermarket") }
    var latText by remember {
        mutableStateOf(
            currentLocation?.latitude?.toString() ?: QUICK_PLACES[0].lat.toString()
        )
    }
    var lngText by remember {
        mutableStateOf(
            currentLocation?.longitude?.toString() ?: QUICK_PLACES[0].lng.toString()
        )
    }

    // Radius state (manually configurable)
    var radiusMetersText by remember { mutableStateOf("150") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
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
                                text = "Get notified when entering target radius",
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
                        label = { Text("Task to do") },
                        placeholder = { Text("e.g. Buy milk, return book") },
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
                                text = "Target Location",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Toggle manual location mode
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
                                    text = if (isManualLocation) "Presets" else "Manual Add",
                                    fontSize = 11.sp
                                )
                            }
                        }

                        if (!isManualLocation) {
                            // Quick Presets
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                QUICK_PLACES.forEach { place ->
                                    val isSelected = placeName == place.name
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            placeName = place.name
                                            latText = place.lat.toString()
                                            lngText = place.lng.toString()
                                        },
                                        label = { Text("${place.emoji} ${place.name}") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }

                                currentLocation?.let { loc ->
                                    FilterChip(
                                        selected = placeName == "My Current Spot",
                                        onClick = {
                                            placeName = "My Current Spot"
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
                                        label = { Text("Current Location") }
                                    )
                                }
                            }
                        } else {
                            // Manual Location Inputs
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = placeName,
                                    onValueChange = { placeName = it },
                                    label = { Text("Place / Address Name") },
                                    placeholder = { Text("e.g. Green Gym, City Library") },
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
                                            if (placeName.isBlank()) placeName = "My Current GPS Spot"
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
                                        Text("Paste Current GPS Coordinates", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Radius Option (Quick chips + Manual edit)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Geofence Radius",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${radiusMetersText.toIntOrNull() ?: 150} meters",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Preset Radius Chips
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            RADIUS_PRESETS.forEach { r ->
                                val isSelected = radiusMetersText == r.toString()
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { radiusMetersText = r.toString() },
                                    label = { Text("${r}m") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }

                        // Manual Radius Input field
                        OutlinedTextField(
                            value = radiusMetersText,
                            onValueChange = { radiusMetersText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Custom Radius (meters)") },
                            placeholder = { Text("e.g. 75, 150, 300, 1000") },
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
                        val parsedRadius = radiusMetersText.toFloatOrNull() ?: 150f
                        val isValid = taskText.isNotBlank() && placeName.isNotBlank() && parsedLat != null && parsedLng != null

                        Button(
                            onClick = {
                                if (isValid && parsedLat != null && parsedLng != null) {
                                    onSave(taskText, placeName, parsedLat, parsedLng, parsedRadius.coerceAtLeast(20f))
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
