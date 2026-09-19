<p align="center">
  <img src="AppImages/icon.png" width="132" alt="SensorBox app icon">
</p>

<h1 align="center">SensorBox</h1>

<p align="center">
  A private, local-first sensor recorder for Android phones and Wear OS watches.
</p>

<p align="center">
  <img alt="Android 10+" src="https://img.shields.io/badge/Android-10%2B-3DDC84?logo=android&amp;logoColor=white">
  <img alt="Wear OS" src="https://img.shields.io/badge/Wear%20OS-supported-4285F4?logo=wearos&amp;logoColor=white">
  <img alt="Jetpack Compose" src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&amp;logoColor=white">
</p>

## See it in use

<table>
  <tr>
    <td width="33%"><img src="docs/images/sensorbox-phone-record.png" alt="SensorBox phone sensor selection"></td>
    <td width="33%"><img src="docs/images/sensorbox-phone-setup.png" alt="SensorBox recording setup"></td>
    <td width="33%"><img src="docs/images/sensorbox-wear.png" alt="SensorBox Wear OS dashboard"></td>
  </tr>
  <tr>
    <td align="center"><sub>Choose the sensors and sources to record.</sub></td>
    <td align="center"><sub>Set the recording options before you start.</sub></td>
    <td align="center"><sub>Record from the watch or pair it with the phone.</sub></td>
  </tr>
</table>

## The idea

SensorBox records raw sensor samples from an Android phone or Wear OS watch into CSV files in a folder you choose. Recordings stay on your devices. There is no account, analytics service, or cloud storage in the recording path.

This is a revamp of the previous SensorBox app after several years. The project has gone through a full refactor, and many problems from the previous version have been solved. Issues are still expected while the new version settles, especially across different phones, sensors, and Wear OS devices.

> The behaviour was not persited 1:1 completly, but all major features and couple new were added. For example file browser was added or improved sync of files between wearable and phone. One of features which was removed was actually heart rate monitoring and other possible medical implementations as they do not fit into the domain of the app.

## What it does

- Records available phone and watch sensors at the selected sampling period
- Records GPS and activity recognition data when requested
- Runs recordings in a visible foreground service with explicit stop handling
- Shows a live sensor signal on Wear OS before recording
- Records on the watch independently when a phone is not available
- Starts and stops paired phone and watch recordings together
- Syncs watch measurements to the phone without deleting the watch originals
- Writes measurements to a folder selected through Android's system folder picker
- Adds metadata and annotations to recording sessions

## How it is built

SensorBox uses feature-first modules and a pragmatic MVI flow:

```text
Composable -> Intent -> ViewModel -> use case -> repository -> State + Effect
```

The `app` module owns the phone UI and recording flows. The `wear` module owns the Wear OS UI and watch recording. `recording-core` contains the platform-independent recording state machine. `sensorservices` adapts Android sensors, GPS, foreground services, and file storage. `core` and `core-common` contain shared preferences, storage, diagnostics, and result types. `WearOsLib` contains the versioned phone-to-watch commands and file transport.

Hilt wires Android implementations behind testable interfaces. Phone and watch keep their own recording sessions and local files. The phone can later sync completed watch measurements into the selected recording archive.

## Build it

You need JDK 17 and Android SDK 37. A Wear OS device or emulator is only needed to run the watch app and paired tests.

```sh
./gradlew :app:assembleDebug :wear:assembleDebug
./gradlew testDebugUnitTest detekt
./gradlew :app:lintDebug :wear:lintDebug
```

No Firebase project, Maps key, secrets file, account, or external-storage permission is required.

## Release it

The Android release workflow builds, signs, verifies, and uploads the phone and Wear OS bundles using environment-provided credentials:

```sh
bundle install
./release_android.sh --validate-only
```

The default Google Play destinations are draft releases on `internal` for the phone and `wear:qa` for Wear OS. See the [Google Play submission pack](store/google_play/README.md) for the required environment variables, listing materials, and remaining submission checks.

SensorBox is licensed under the [Apache License 2.0](LICENSE).
