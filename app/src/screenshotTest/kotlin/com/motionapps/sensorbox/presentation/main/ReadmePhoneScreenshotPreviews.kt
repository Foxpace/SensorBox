package com.motionapps.sensorbox.presentation.main

import android.content.res.Configuration
import android.hardware.Sensor
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.motionapps.sensorbox.core.preferences.AppThemeMode
import com.motionapps.sensorbox.domain.sensors.SensorDescriptor
import com.motionapps.sensorbox.ui.theme.SensorBoxTheme

@PreviewTest
@ReadmePhonePreview
@Composable
fun readmeSourceSelection() = PhoneReadmeFrame {
    RecordScreen(state = readmeRecordingState(), onIntent = {})
}

@PreviewTest
@ReadmePhonePreview
@Composable
fun readmeMeasurementSetup() = PhoneReadmeFrame {
    MeasurementSetupScreen(
        state = readmeRecordingState().copy(selectedSensorIds = setOf(Sensor.TYPE_GRAVITY)),
        onIntent = {},
        onBack = {},
    )
}

@PreviewTest
@ReadmePhonePreview
@Composable
fun readmeIntroWelcome() = OnboardingReadmePreview(page = 0)

@PreviewTest
@ReadmePhonePreview
@Composable
fun readmeIntroPrivacy() = OnboardingReadmePreview(page = 1)

@PreviewTest
@ReadmePhonePreview
@Composable
fun readmeIntroPolicy() = OnboardingReadmePreview(page = 2)

@PreviewTest
@ReadmePhonePreview
@Composable
fun readmeIntroLifecycle() = OnboardingReadmePreview(page = 3)

@PreviewTest
@ReadmePhonePreview
@Composable
fun readmeIntroBattery() = OnboardingReadmePreview(page = 4)

@PreviewTest
@ReadmePhonePreview
@Composable
fun readmeIntroStorage() = OnboardingReadmePreview(page = 5, storagePath = "Documents/SensorBox")

@Composable
private fun OnboardingReadmePreview(page: Int, storagePath: String? = null) = PhoneReadmeFrame {
    OnboardingScreen(state = OnboardingState(page = page, storagePath = storagePath), onIntent = {})
}

@Composable
private fun PhoneReadmeFrame(content: @Composable () -> Unit) {
    SensorBoxTheme(themeMode = AppThemeMode.DARK, dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize(), content = content)
    }
}

private fun readmeRecordingState() = RecordingState(
    sensors = listOf(
        SensorDescriptor(Sensor.TYPE_GRAVITY, "Gravity Sensor", "Google"),
        SensorDescriptor(Sensor.TYPE_PRESSURE, "ICP20100 Pressure Sensor", "InvenSense"),
        SensorDescriptor(Sensor.TYPE_ACCELEROMETER, "LSM6DSV Accelerometer", "STMicro"),
        SensorDescriptor(Sensor.TYPE_GYROSCOPE, "LSM6DSV Gyroscope", "STMicro"),
        SensorDescriptor(Sensor.TYPE_LINEAR_ACCELERATION, "Linear Acceleration Sensor", "Google"),
        SensorDescriptor(Sensor.TYPE_MAGNETIC_FIELD, "MMC56X3X Magnetometer", "MEMSIC"),
        SensorDescriptor(Sensor.TYPE_ROTATION_VECTOR, "Rotation Vector Sensor", "Google"),
    ),
    storagePath = "Documents/SensorBox",
)

@Preview(
    device = README_PHONE_DEVICE,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
private annotation class ReadmePhonePreview

private const val README_PHONE_DEVICE = "spec:width=1080px,height=2400px,dpi=420"
