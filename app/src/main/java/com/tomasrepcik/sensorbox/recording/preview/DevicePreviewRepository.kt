package com.tomasrepcik.sensorbox.recording.preview

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationServices
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.core.failure.appResult
import com.tomasrepcik.sensorbox.recordinghost.sources.gps.GPSHandler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

data class PreviewLocation(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val accuracy: Float,
    val speed: Float,
    val bearing: Float,
    val provider: String?,
)

data class GpsPreviewData(
    val hasPermission: Boolean,
    val location: PreviewLocation? = null,
    val isAvailable: Boolean? = null,
)

data class SensorPreviewSample(val timestampNanos: Long, val values: List<Float>)

data class SensorPreviewData(val isAvailable: Boolean, val samples: List<SensorPreviewSample> = emptyList())

interface DevicePreviewRepository {
    fun observeGps(intervalSeconds: Int, minimumDistanceMeters: Int): Flow<AppResult<GpsPreviewData>>

    fun observeSensor(sensorType: Int): Flow<AppResult<SensorPreviewData>>

    fun batteryOptimizationExemption(): AppResult<Boolean>
}

class AndroidDevicePreviewRepository @Inject constructor(@ApplicationContext private val context: Context) :
    DevicePreviewRepository {
    override fun observeGps(intervalSeconds: Int, minimumDistanceMeters: Int): Flow<AppResult<GpsPreviewData>> =
        callbackFlow {
            if (!hasLocationPermission()) {
                trySend(AppResult.success(GpsPreviewData(hasPermission = false)))
                close()
                return@callbackFlow
            }

            var current = GpsPreviewData(hasPermission = true)
            trySend(AppResult.success(current))
            val handler = GPSHandler(LocationServices.getFusedLocationProviderClient(context))
            val callback = object : GPSHandler.OnLocationChangedCallback {
                override fun onLocationChanged(location: Location?) = publishLocation(location)

                override fun onLastLocationSuccess(location: Location?) = publishLocation(location)

                override fun onAvailabilityChanged(locationAvailability: LocationAvailability?) {
                    current = current.copy(isAvailable = locationAvailability?.isLocationAvailable)
                    trySend(AppResult.success(current))
                }

                private fun publishLocation(location: Location?) {
                    if (location == null) return
                    current = current.copy(location = location.toPreviewLocation())
                    trySend(AppResult.success(current))
                }
            }
            appResult(AppErrorCode.EXTERNAL_ACTION, "Start GPS preview") {
                handler.addCallback(callback, intervalSeconds, minimumDistanceMeters)
            }.onFailure { trySend(AppResult.failure(it)) }
            awaitClose { handler.gpsOff() }
        }

    override fun observeSensor(sensorType: Int): Flow<AppResult<SensorPreviewData>> = callbackFlow {
        val manager = context.getSystemService(SensorManager::class.java)
        val sensor = manager?.getDefaultSensor(sensorType)
        if (manager == null || sensor == null) {
            trySend(AppResult.success(SensorPreviewData(isAvailable = false)))
            close()
            return@callbackFlow
        }

        var samples = emptyList<SensorPreviewSample>()
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                samples = (samples + SensorPreviewSample(event.timestamp, event.values.toList()))
                    .takeLast(MAX_CHART_SAMPLES)
                trySend(AppResult.success(SensorPreviewData(isAvailable = true, samples = samples)))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        val registered = manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        trySend(AppResult.success(SensorPreviewData(isAvailable = registered)))
        awaitClose { manager.unregisterListener(listener) }
    }

    override fun batteryOptimizationExemption(): AppResult<Boolean> = appResult(
        AppErrorCode.EXTERNAL_ACTION,
        "Read battery optimization exemption",
    ) {
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun Location.toPreviewLocation() = PreviewLocation(
        latitude = latitude,
        longitude = longitude,
        altitude = altitude,
        accuracy = accuracy,
        speed = speed,
        bearing = bearing,
        provider = provider,
    )

    private companion object {
        const val MAX_CHART_SAMPLES = 90
    }
}
