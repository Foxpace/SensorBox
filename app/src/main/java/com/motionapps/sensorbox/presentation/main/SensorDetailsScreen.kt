package com.motionapps.sensorbox.presentation.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationAvailability
import com.motionapps.sensorbox.R
import com.motionapps.sensorbox.domain.sensors.SensorDescriptor
import com.motionapps.sensorservices.handlers.GPSHandler

@Composable
fun SensorDetailsScreen(
    state: RecordingState,
    onBack: () -> Unit,
    onPreview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sensor = state.detailsSensorType?.let { type -> state.sensors.firstOrNull { it.type == type } }
    val canPreview = state.detailsSensorType == null || sensor?.type != Sensor.TYPE_STEP_DETECTOR
    val title = when {
        state.detailsSensorType == null -> stringResource(R.string.gps)
        sensor != null -> sensor.name
        else -> stringResource(R.string.sensor_unavailable)
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SensorBoxTopAppBar(title, onBack) }
        if (state.detailsSensorType == null) {
            item { GpsDetails(state) }
        } else if (sensor != null) {
            item { HardwareSensorDetails(sensor) }
        }
        if ((state.detailsSensorType == null || sensor != null) && canPreview) {
            item {
                SensorBoxPrimaryButton(
                    label = stringResource(R.string.preview),
                    onClick = onPreview,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun HardwareSensorDetails(sensor: SensorDescriptor) {
    SensorBoxPanel {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val unit = sensorUnit(sensor.type)
            val type = if (sensor.stringType.isBlank()) {
                stringResource(R.string.detail_sensor_type_value, sensor.type)
            } else {
                sensor.stringType
            }
            DetailRow(stringResource(R.string.detail_name), sensor.name)
            DetailRow(stringResource(R.string.detail_version), sensor.version.toString())
            DetailRow(stringResource(R.string.detail_vendor), sensor.vendor)
            DetailRow(stringResource(R.string.detail_resolution, unit), sensor.resolution.toString())
            DetailRow(stringResource(R.string.detail_power), formatDecimal(sensor.power))
            DetailRow(stringResource(R.string.detail_maximum_range, unit), formatDecimal(sensor.maximumRange))
            DetailRow(
                stringResource(R.string.detail_minimum_delay),
                stringResource(R.string.detail_delay_value, sensor.minimumDelayMicros),
            )
            DetailRow(
                stringResource(R.string.detail_maximum_delay),
                stringResource(R.string.detail_delay_value, sensor.maximumDelayMicros),
            )
            DetailRow(
                stringResource(R.string.detail_android_sensor_type),
                type,
            )
            DetailRow(stringResource(R.string.detail_reporting_mode), reportingModeLabel(sensor.reportingMode))
            DetailRow(
                stringResource(R.string.detail_wakeup_sensor),
                stringResource(if (sensor.isWakeUpSensor) R.string.yes else R.string.no),
            )
        }
    }
}

@Composable
private fun GpsDetails(state: RecordingState) {
    var permissionRevision by remember { mutableStateOf(0) }
    val permissionRequest = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        permissionRevision += 1
    }
    val details = rememberGpsDetails(
        intervalSeconds = state.preferences.gpsIntervalSeconds,
        minimumDistanceMeters = state.preferences.gpsMinDistanceMeters,
        permissionRevision = permissionRevision,
    )
    val unavailableValue = stringResource(if (details.hasPermission) R.string.waiting else R.string.unavailable)
    SensorBoxPanel {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GpsPermission(details.hasPermission) {
                permissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                )
            }
            GpsDetailRows(details, state, unavailableValue)
        }
    }
}

@Composable
private fun GpsDetailRows(details: GpsDetailsState, state: RecordingState, unavailableValue: String) {
    DetailRow(
        stringResource(R.string.detail_latitude),
        details.location?.latitude?.toString() ?: unavailableValue,
    )
    DetailRow(
        stringResource(R.string.detail_longitude),
        details.location?.longitude?.toString() ?: unavailableValue,
    )
    DetailRow(stringResource(R.string.detail_altitude), localizedValue(details.location?.altitude, unavailableValue))
    DetailRow(stringResource(R.string.detail_accuracy), localizedValue(details.location?.accuracy, unavailableValue))
    DetailRow(stringResource(R.string.detail_speed), localizedValue(details.location?.speed, unavailableValue))
    DetailRow(stringResource(R.string.detail_bearing), localizedValue(details.location?.bearing, unavailableValue))
    DetailRow(stringResource(R.string.detail_provider), details.location?.provider ?: unavailableValue)
    DetailRow(stringResource(R.string.detail_available), locationAvailabilityLabel(details.isAvailable))
    DetailRow(
        stringResource(R.string.detail_update_interval),
        pluralStringResource(
            R.plurals.seconds_count,
            state.preferences.gpsIntervalSeconds,
            state.preferences.gpsIntervalSeconds,
        ),
    )
    DetailRow(
        stringResource(R.string.detail_minimum_distance),
        pluralStringResource(
            R.plurals.meters_count,
            state.preferences.gpsMinDistanceMeters,
            state.preferences.gpsMinDistanceMeters,
        ),
    )
}

private fun localizedValue(value: Number?, unavailableValue: String): String =
    value?.let(::formatDecimal) ?: unavailableValue

@Composable
internal fun GpsPermission(hasPermission: Boolean, onRequest: () -> Unit) {
    DetailRow(
        stringResource(R.string.location_permission),
        stringResource(if (hasPermission) R.string.granted else R.string.required),
    )
    if (hasPermission) return
    Text(
        stringResource(R.string.location_permission_explanation),
        color = MaterialTheme.colorScheme.error,
    )
    SensorBoxSecondaryButton(
        label = stringResource(R.string.grant_location_permission),
        onClick = onRequest,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
internal fun rememberGpsDetails(
    intervalSeconds: Int,
    minimumDistanceMeters: Int,
    permissionRevision: Int,
): GpsDetailsState {
    val context = LocalContext.current
    val hasPermission = remember(context, permissionRevision) { context.hasLocationPermission() }
    var location by remember { mutableStateOf<Location?>(null) }
    var isAvailable by remember { mutableStateOf<Boolean?>(null) }
    val gpsHandler = remember { GPSHandler() }

    DisposableEffect(gpsHandler, hasPermission, intervalSeconds, minimumDistanceMeters) {
        var active = true
        if (hasPermission) {
            gpsHandler.configure(intervalSeconds, minimumDistanceMeters)
            gpsHandler.addCallback(
                context,
                GpsDetailsCallback(
                    onLocation = { if (active && it != null) location = it },
                    onAvailability = { if (active) isAvailable = it },
                ),
            )
        }
        onDispose {
            active = false
            gpsHandler.gpsOff()
        }
    }
    return GpsDetailsState(hasPermission, location, isAvailable)
}

@Composable
internal fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun reportingModeLabel(mode: Int): String = when (mode) {
    0 -> stringResource(R.string.reporting_continuous)
    1 -> stringResource(R.string.reporting_on_change)
    2 -> stringResource(R.string.reporting_one_shot)
    3 -> stringResource(R.string.reporting_special_trigger)
    else -> stringResource(R.string.unknown_with_value, mode)
}

@Composable
internal fun sensorUnit(type: Int): String = when (type) {
    Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GRAVITY, Sensor.TYPE_LINEAR_ACCELERATION ->
        stringResource(R.string.unit_acceleration)

    Sensor.TYPE_GYROSCOPE -> stringResource(R.string.unit_angular_velocity)

    Sensor.TYPE_RELATIVE_HUMIDITY -> stringResource(R.string.unit_percent)

    Sensor.TYPE_MAGNETIC_FIELD -> stringResource(R.string.unit_magnetic_field)

    Sensor.TYPE_PROXIMITY -> stringResource(R.string.unit_centimeters)

    Sensor.TYPE_PRESSURE -> stringResource(R.string.unit_pressure)

    Sensor.TYPE_LIGHT -> stringResource(R.string.unit_lux)

    Sensor.TYPE_AMBIENT_TEMPERATURE -> stringResource(R.string.unit_temperature)

    Sensor.TYPE_STEP_COUNTER, Sensor.TYPE_STEP_DETECTOR -> stringResource(R.string.unit_steps)

    Sensor.TYPE_HEART_RATE -> stringResource(R.string.unit_heart_rate)

    else -> stringResource(R.string.unit_none)
}

internal fun formatDecimal(value: Number): String = "%.2f".format(value.toDouble())

@Composable
private fun locationAvailabilityLabel(isAvailable: Boolean?): String = when (isAvailable) {
    true -> stringResource(R.string.yes)
    false -> stringResource(R.string.no)
    null -> stringResource(R.string.unknown)
}

private fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

internal data class GpsDetailsState(val hasPermission: Boolean, val location: Location?, val isAvailable: Boolean?)

private class GpsDetailsCallback(
    private val onLocation: (Location?) -> Unit,
    private val onAvailability: (Boolean?) -> Unit,
) : GPSHandler.OnLocationChangedCallback {
    override fun onLocationChanged(location: Location?) = onLocation(location)

    override fun onLastLocationSuccess(location: Location?) = onLocation(location)

    override fun onAvailabilityChanged(locationAvailability: LocationAvailability?) =
        onAvailability(locationAvailability?.isLocationAvailable)
}
