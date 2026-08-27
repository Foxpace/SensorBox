package com.tomasrepcik.sensorbox.presentation.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.google.android.gms.location.LocationServices
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.core.format.ValueFormats
import com.tomasrepcik.sensorbox.domain.sensors.SensorDescriptor
import com.tomasrepcik.sensorbox.domain.sensors.SensorReportingMode
import com.tomasrepcik.sensorbox.sensorservices.handlers.GPSHandler

@Composable
fun SensorDetailsScreen(
    state: RecordingState,
    onBack: () -> Unit,
    onPreview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sensor = state.detailsSensorType?.let { type ->
        val sensors = when (state.detailsSensorSource) {
            RecordingSensorSource.PHONE -> state.sensors
            RecordingSensorSource.WEAR -> state.wearSensors
        }
        sensors.firstOrNull { it.type == type }
    }
    val canPreview = state.detailsSensorType == null || sensor?.type != Sensor.TYPE_STEP_DETECTOR
    val title = when {
        state.detailsSensorType == null -> stringResource(R.string.gps)
        sensor != null -> sensor.name
        else -> stringResource(R.string.sensor_unavailable)
    }
    val showPreview = state.detailsSensorSource == RecordingSensorSource.PHONE &&
        (state.detailsSensorType == null || sensor != null) && canPreview
    Box(modifier.fillMaxSize()) {
        SensorDetailsList(state, sensor, title, showPreview, onBack)
        if (showPreview) {
            PreviewAction(onPreview, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun SensorDetailsList(
    state: RecordingState,
    sensor: SensorDescriptor?,
    title: String,
    showPreview: Boolean,
    onBack: () -> Unit,
) {
    SensorBoxBackScreen(
        title = title,
        onBack = onBack,
        bottomPadding = if (showPreview) 104.dp else 24.dp,
    ) {
        if (state.detailsSensorType == null) {
            item { GpsDetails(state) }
        } else if (sensor != null) {
            item {
                HardwareSensorDetails(sensor)
            }
        }
    }
}

@Composable
private fun HardwareSensorDetails(sensor: SensorDescriptor) {
    Column(Modifier.fillMaxWidth()) {
        val unit = sensorUnit(sensor.type)
        val type = if (sensor.stringType.isBlank()) {
            stringResource(R.string.detail_sensor_type_value, sensor.type)
        } else {
            sensor.stringType
        }
        ParameterRow(stringResource(R.string.detail_version), sensor.version.toString())
        ParameterRow(stringResource(R.string.detail_vendor), sensor.vendor)
        ParameterRow(stringResource(R.string.detail_resolution, unit), sensor.resolution.toString())
        ParameterRow(stringResource(R.string.detail_power), formatDecimal(sensor.power))
        ParameterRow(stringResource(R.string.detail_maximum_range, unit), formatDecimal(sensor.maximumRange))
        ParameterRow(
            stringResource(R.string.detail_minimum_delay),
            stringResource(R.string.detail_delay_value, sensor.minimumDelayMicros),
        )
        ParameterRow(
            stringResource(R.string.detail_maximum_delay),
            stringResource(R.string.detail_delay_value, sensor.maximumDelayMicros),
        )
        ParameterRow(stringResource(R.string.detail_android_sensor_type), type)
        ParameterRow(stringResource(R.string.detail_reporting_mode), reportingModeLabel(sensor.reportingMode))
        ParameterRow(
            stringResource(R.string.detail_wakeup_sensor),
            stringResource(if (sensor.isWakeUpSensor) R.string.yes else R.string.no),
            showDivider = false,
        )
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
        intervalSeconds = state.preferences.recording.gpsIntervalSeconds,
        minimumDistanceMeters = state.preferences.recording.gpsMinDistanceMeters,
        permissionRevision = permissionRevision,
    )
    val unavailableValue = stringResource(if (details.hasPermission) R.string.waiting else R.string.unavailable)
    Column(Modifier.fillMaxWidth()) {
        GpsPermissionStatus(details.hasPermission)
        GpsDetailRows(details, state, unavailableValue)
        if (!details.hasPermission) {
            GpsPermissionAction {
                permissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                )
            }
        }
    }
}

@Composable
private fun GpsDetailRows(details: GpsDetailsState, state: RecordingState, unavailableValue: String) {
    ParameterRow(
        stringResource(R.string.detail_latitude),
        details.location?.latitude?.toString() ?: unavailableValue,
    )
    ParameterRow(
        stringResource(R.string.detail_longitude),
        details.location?.longitude?.toString() ?: unavailableValue,
    )
    ParameterRow(stringResource(R.string.detail_altitude), localizedValue(details.location?.altitude, unavailableValue))
    ParameterRow(stringResource(R.string.detail_accuracy), localizedValue(details.location?.accuracy, unavailableValue))
    ParameterRow(stringResource(R.string.detail_speed), localizedValue(details.location?.speed, unavailableValue))
    ParameterRow(stringResource(R.string.detail_bearing), localizedValue(details.location?.bearing, unavailableValue))
    ParameterRow(stringResource(R.string.detail_provider), details.location?.provider ?: unavailableValue)
    ParameterRow(stringResource(R.string.detail_available), locationAvailabilityLabel(details.isAvailable))
    ParameterRow(
        stringResource(R.string.detail_update_interval),
        pluralStringResource(
            R.plurals.seconds_count,
            state.preferences.recording.gpsIntervalSeconds,
            state.preferences.recording.gpsIntervalSeconds,
        ),
    )
    ParameterRow(
        stringResource(R.string.detail_minimum_distance),
        pluralStringResource(
            R.plurals.meters_count,
            state.preferences.recording.gpsMinDistanceMeters,
            state.preferences.recording.gpsMinDistanceMeters,
        ),
        showDivider = false,
    )
}

private fun localizedValue(value: Number?, unavailableValue: String): String =
    value?.let(::formatDecimal) ?: unavailableValue

@Composable
internal fun GpsPermission(hasPermission: Boolean, onRequest: () -> Unit) {
    GpsPermissionStatus(hasPermission)
    if (!hasPermission) GpsPermissionAction(onRequest)
}

@Composable
private fun GpsPermissionStatus(hasPermission: Boolean) {
    ParameterRow(
        stringResource(R.string.location_permission),
        stringResource(if (hasPermission) R.string.granted else R.string.required),
    )
}

@Composable
private fun GpsPermissionAction(onRequest: () -> Unit) {
    Text(
        stringResource(R.string.location_permission_explanation),
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(top = 24.dp, bottom = 12.dp),
    )
    SensorBoxSecondaryButton(
        label = stringResource(R.string.grant_location_permission),
        onClick = onRequest,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ParameterRow(label: String, value: String, showDivider: Boolean = true) {
    DetailRow(label, value, Modifier.padding(vertical = 15.dp))
    if (showDivider) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f))
    }
}

@Composable
private fun PreviewAction(onPreview: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background) {
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            SensorBoxPrimaryButton(
                label = stringResource(R.string.preview),
                onClick = onPreview,
                modifier = Modifier.widthIn(min = 176.dp, max = 240.dp),
            )
        }
    }
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
    val gpsHandler = remember(context) {
        GPSHandler(LocationServices.getFusedLocationProviderClient(context))
    }

    DisposableEffect(gpsHandler, hasPermission, intervalSeconds, minimumDistanceMeters) {
        var active = true
        if (hasPermission) {
            gpsHandler.configure(intervalSeconds, minimumDistanceMeters)
            gpsHandler.addCallback(
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
internal fun DetailRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun reportingModeLabel(mode: SensorReportingMode): String = when (mode) {
    SensorReportingMode.CONTINUOUS -> stringResource(R.string.reporting_continuous)
    SensorReportingMode.ON_CHANGE -> stringResource(R.string.reporting_on_change)
    SensorReportingMode.ONE_SHOT -> stringResource(R.string.reporting_one_shot)
    SensorReportingMode.SPECIAL_TRIGGER -> stringResource(R.string.reporting_special_trigger)
    SensorReportingMode.UNKNOWN -> stringResource(R.string.unknown)
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

    else -> stringResource(R.string.unit_none)
}

internal fun formatDecimal(value: Number): String = ValueFormats.decimal(value)

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
