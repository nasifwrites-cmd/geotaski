package com.example.ui.screens

import android.location.Location
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
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
import java.util.Locale

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

private val RADIUS_PRESETS = listOf(5, 10, 15, 20)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddSimpleReminderDialog(
    currentLocation: Location?,
    savedPresets: List<SavedLocationPreset>,
    isAcquiringLocation: Boolean = false,
    onRequestPreciseLocation: ((Location) -> Unit) -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (task: String, placeName: String, lat: Double, lng: Double, radiusMeters: Float, alsoSavePreset: Boolean) -> Unit
) {
    var taskText by remember { mutableStateOf("") }
    var placeName by remember { mutableStateOf("") }

    var latText by remember {
        mutableStateOf(
            if (currentLocation != null) String.format(Locale.US, "%.6f", currentLocation.latitude)
            else ""
        )
    }
    var lngText by remember {
        mutableStateOf(
            if (currentLocation != null) String.format(Locale.US, "%.6f", currentLocation.longitude)
            else ""
        )
    }

    var acquiredAccuracy by remember { mutableFloatStateOf(currentLocation?.accuracy ?: -1f) }
    var hasExactGpsLock by remember { mutableStateOf(currentLocation != null) }
    var radiusMetersText by remember { mutableStateOf("10") }
    var alsoSavePreset by remember { mutableStateOf(false) }

    // If initial location was null but later received or dialog opens, initialize placeName
    LaunchedEffect(currentLocation) {
        if (currentLocation != null && latText.isBlank() && lngText.isBlank()) {
            latText = String.format(Locale.US, "%.6f", currentLocation.latitude)
            lngText = String.format(Locale.US, "%.6f", currentLocation.longitude)
            acquiredAccuracy = currentLocation.accuracy
            hasExactGpsLock = true
            if (placeName.isBlank()) {
                placeName = "Current Spot"
            }
        }
    }

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
                .heightIn(max = 680.dp)
        ) {
            LazyColumn(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                text = "Loud buzzing alert triggers upon entering radius",
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
                        label = { Text("Task Description") },
                        placeholder = { Text("e.g. Collect documents, purchase groceries") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("task_input_field"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // 2. Exact GPS Capture Button (Primary Action)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Destination Location",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Prominent "Get Exact Current Location" button
                        Button(
                            onClick = {
                                onRequestPreciseLocation { loc ->
                                    latText = String.format(Locale.US, "%.6f", loc.latitude)
                                    lngText = String.format(Locale.US, "%.6f", loc.longitude)
                                    acquiredAccuracy = loc.accuracy
                                    hasExactGpsLock = true
                                    if (placeName.isBlank()) {
                                        placeName = "Current Spot"
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_get_exact_gps"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isAcquiringLocation) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Fetching High-Precision GPS Lock...", fontSize = 12.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (hasExactGpsLock) "Update Exact Current Location" else "Get Exact Current Location",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Status Badge showing coordinates & GPS accuracy
                        if (latText.isNotBlank() && lngText.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "GPS Coordinates: $latText, $lngText",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = if (acquiredAccuracy > 0)
                                                "Accuracy: ±${acquiredAccuracy.toInt()}m (Satellite Lock)"
                                            else
                                                "High-precision coordinates set",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }

                        // Presets Chips
                        if (savedPresets.isNotEmpty() || DEFAULT_PLACES.isNotEmpty()) {
                            Text(
                                text = "Or pick from presets:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                savedPresets.forEach { preset ->
                                    val isSelected = placeName == preset.name
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            placeName = preset.name
                                            latText = String.format(Locale.US, "%.6f", preset.latitude)
                                            lngText = String.format(Locale.US, "%.6f", preset.longitude)
                                            radiusMetersText = preset.defaultRadiusMeters.toInt().toString()
                                            hasExactGpsLock = true
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Bookmark,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        },
                                        label = { Text(preset.name, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    )
                                }

                                DEFAULT_PLACES.forEach { place ->
                                    val isSelected = placeName == place.name
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            placeName = place.name
                                            latText = place.lat.toString()
                                            lngText = place.lng.toString()
                                            hasExactGpsLock = true
                                        },
                                        label = { Text(place.name, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }
                        }

                        // Place name
                        OutlinedTextField(
                            value = placeName,
                            onValueChange = { placeName = it },
                            label = { Text("Location Name") },
                            placeholder = { Text("e.g. Central Market, Branch Office, Gym") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manual_place_name"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Editable Latitude and Longitude
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

                        // Checkbox to also save preset
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
                                text = "Save this location as a reusable preset for next usage",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // 3. Proximity Radius (5, 10, 15, 20 + custom)
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
                    val parsedLat = latText.toDoubleOrNull()
                    val parsedLng = lngText.toDoubleOrNull()
                    val parsedRadius = radiusMetersText.toFloatOrNull() ?: 10f
                    val isValid = taskText.isNotBlank() && placeName.isNotBlank() && parsedLat != null && parsedLng != null

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
 * Dedicated dialog to capture exact GPS coordinates and save as preset for future usage
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
    var radiusText by remember { mutableStateOf("10") }

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

    LaunchedEffect(currentLocation) {
        if (currentLocation != null && !hasCaptured) {
            lat = currentLocation.latitude
            lng = currentLocation.longitude
            accuracy = currentLocation.accuracy
            hasCaptured = true
            if (name.isBlank()) {
                name = "Current Location"
            }
        }
    }

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
                            text = "Capture exact GPS position for future alerts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Explicit Button to Fetch Precise GPS Location
                Button(
                    onClick = {
                        onRequestPreciseLocation { loc ->
                            lat = loc.latitude
                            lng = loc.longitude
                            accuracy = loc.accuracy
                            hasCaptured = true
                            if (name.isBlank()) {
                                name = "Current Location"
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("save_spot_capture_gps_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isAcquiringLocation) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Acquiring Precise Satellite GPS...", fontSize = 12.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (hasCaptured) "Re-Capture Exact GPS" else "Capture Exact GPS Location",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Confirmation Surface showing exact coordinates & accuracy
                Surface(
                    color = if (hasCaptured) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (hasCaptured) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (hasCaptured) Icons.Default.CheckCircle else Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = if (hasCaptured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = if (hasCaptured)
                                    "Exact GPS: ${String.format(Locale.US, "%.6f, %.6f", lat, lng)}"
                                else
                                    "No GPS coordinates locked yet",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (hasCaptured) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (accuracy > 0)
                                    "Accuracy: ±${accuracy.toInt()} meters (High Precision)"
                                else if (hasCaptured)
                                    "Precise coordinates acquired"
                                else
                                    "Tap 'Capture Exact GPS Location' above",
                                fontSize = 11.sp,
                                color = if (hasCaptured) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
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

                    val canSave = name.isNotBlank() && hasCaptured && lat != 0.0
                    Button(
                        onClick = {
                            if (canSave) {
                                onSave(name, lat, lng, radiusText.toFloatOrNull() ?: 10f)
                            }
                        },
                        enabled = canSave,
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
