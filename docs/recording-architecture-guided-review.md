# Recording architecture guided review

This review follows one recording request from the phone UI, through the phone and Wear OS command boundaries, into the Android sensor sources. Read the files in order. At each step, check that the caller expresses a user intent and the dependency owns the implementation detail.

## 1. Start with the architecture decisions

1. `adr/0005-define-sensorbox-application-architecture.md`
   - Defines module ownership, MVI boundaries, dependency direction, and serialization rules.
   - CSV is reserved for sensor sample streams. Other structured data sent or stored by the app uses Kotlin serialization JSON.
2. `adr/0006-prefer-direct-user-intent-execution.md`
   - Defines direct `start` and `stop` execution.
   - A countdown belongs to the ViewModel and must not prepare folders, services, sources, or Wear commands.
3. `adr/0002-separate-recording-engine-from-android-adapters.md`
   - Defines the platform-neutral engine and the Android source adapters around it.

## 2. Follow the phone UI intent

Read:

- `../app/src/main/java/com/tomasrepcik/sensorbox/presentation/main/RecordingContract.kt`
- `../app/src/main/java/com/tomasrepcik/sensorbox/presentation/main/RecordingViewModel.kt`
- `../app/src/main/java/com/tomasrepcik/sensorbox/presentation/main/RecordScreen.kt`
- `../app/src/main/java/com/tomasrepcik/sensorbox/di/RecordingModule.kt`

The root UI sends intents to `RecordingViewModel`. The ViewModel owns the countdown in `startJob`. Until the countdown reaches zero, it does not call the recording use case. The UI only renders `RecordingState.startCountdownSeconds`.

The useful seams injected into the ViewModel are small and named by intent:

- `AvailableSensorsUseCase`
- `RecordingPermissionsUseCase`
- `RecordingArchiveRepository`
- `RecordingControlUseCase`
- `SendWearCommandUseCase`

Check that these interfaces isolate real side effects or a module boundary. There is no general-purpose workflow gateway.

## 3. Follow phone recording coordination

Read:

- `../app/src/main/java/com/tomasrepcik/sensorbox/domain/recording/RecordingControlUseCase.kt`
- `../app/src/main/java/com/tomasrepcik/sensorbox/domain/paired/PairedRecordingCoordinator.kt`
- `../app/src/main/java/com/tomasrepcik/sensorbox/domain/paired/PairedRecordingModel.kt`
- `../app/src/main/java/com/tomasrepcik/sensorbox/domain/recording/PhoneRecordingController.kt`

The execution path is:

```text
RecordingViewModel
  -> RecordingControlUseCase
  -> PairedRecordingCoordinator
       -> PhoneRecordingController
       -> SendWearCommandUseCase (only when watch sources are selected)
```

`PairedRecordingCoordinator` owns one small state gate. It starts the phone first, sends a direct watch start command, and cleans up both sides if the watch fails. It has no prepare/commit/rollback protocol and no mutex. Model-to-command conversions live beside the models in `PairedRecordingModel.kt`.

Check the stale-command rule: a stop received from the watch only affects the matching active recording session.

Also check the disconnected-timer rule. Once the watch start command has been sent, losing its result does not tear down the phone recording. The phone and watch services receive the same duration and each local `RecordingEngine` schedules its own stop. Automatic peer notifications are best effort after local cleanup.

## 4. Review the JSON command boundary

Read:

- `../WearOsLib/src/main/java/com/tomasrepcik/sensorbox/wearoslib/protocol/WearCommand.kt`
- `../WearOsLib/src/main/java/com/tomasrepcik/sensorbox/wearoslib/protocol/WearCommandCodec.kt`
- `../WearOsLib/src/main/java/com/tomasrepcik/sensorbox/wearoslib/protocol/SendWearCommandUseCase.kt`

Commands are serializable data classes inside a versioned JSON envelope. Sensor discovery is explicit: phone sends `RequestAvailableSensors`, and Wear replies with `AvailableSensors`. Start and stop are also direct commands. No caller should build an ad-hoc payload.

## 5. Follow commands received on each device

Phone side:

- `../app/src/main/java/com/tomasrepcik/sensorbox/domain/paired/PhoneWatchMessageDispatcher.kt`
- `../app/src/main/java/com/tomasrepcik/sensorbox/domain/paired/PhoneWatchCommandHandler.kt`
- `../app/src/main/java/com/tomasrepcik/sensorbox/domain/paired/WatchRecordingResultInbox.kt`

Wear side:

- `../wear/src/main/java/com/tomasrepcik/sensorbox/communication/WearMessageDispatcher.kt`
- `../wear/src/main/java/com/tomasrepcik/sensorbox/communication/WearCommandHandler.kt`
- `../wear/src/main/java/com/tomasrepcik/sensorbox/communication/WearCommandEnvironment.kt`
- `../wear/src/main/java/com/tomasrepcik/sensorbox/domain/recording/WatchRecordingControlUseCase.kt`
- `../wear/src/main/java/com/tomasrepcik/sensorbox/di/RecordingModule.kt`

Dispatchers decode and route. Handlers state-gate commands and execute them. On Wear, `WearRecordingRequirementsUseCase` isolates checks that are needed at the moment recording starts, while `WatchRecordingControlUseCase` isolates starting and stopping the Android service. These are deliberate seams because they wrap separate effects and make command behavior testable.

## 6. Follow the service into the recording engine

Read:

- `../sensorservices/src/main/java/com/tomasrepcik/sensorbox/sensorservices/services/RecordingService.kt`
- `../sensorservices/src/main/java/com/tomasrepcik/sensorbox/sensorservices/services/RecordingHostResources.kt`
- `../sensorservices/src/main/java/com/tomasrepcik/sensorbox/sensorservices/serviceController/ServiceControllerFactory.kt`
- `../sensorservices/src/main/java/com/tomasrepcik/sensorbox/sensorservices/serviceController/ServiceController.kt`
- `../recording-core/src/main/kotlin/com/tomasrepcik/sensorbox/recording/RecordingEngine.kt`
- `../recording-core/src/main/kotlin/com/tomasrepcik/sensorbox/recording/RecordingSource.kt`
- `../recording-core/src/main/kotlin/com/tomasrepcik/sensorbox/recording/RecordingModel.kt`

`RecordingService` routes lifecycle and intents. `RecordingHostResources` owns Android host concerns such as notification, wake lock, receiver, and alarm. The factory assembles a controller. The controller translates the platform recording request into the engine recording request. The engine then starts or stops ordered `RecordingSource` instances and knows nothing about Android.

The key engine seam is `RecordingSource`. It is intentionally small: `start(spec)` and `stop(context)`.

## 7. Review Android source construction

Read:

- `../sensorservices/src/main/java/com/tomasrepcik/sensorbox/sensorservices/serviceController/AndroidRecordingSources.kt`
- `../sensorservices/src/main/java/com/tomasrepcik/sensorbox/sensorservices/serviceController/SessionMetadataRecordingSource.kt`
- `../sensorservices/src/main/java/com/tomasrepcik/sensorbox/sensorservices/serviceController/SensorRecordingSource.kt`
- `../sensorservices/src/main/java/com/tomasrepcik/sensorbox/sensorservices/serviceController/GpsRecordingSource.kt`
- `../sensorservices/src/main/java/com/tomasrepcik/sensorbox/sensorservices/serviceController/ActivityRecordingSource.kt`
- `../sensorservices/src/main/java/com/tomasrepcik/sensorbox/sensorservices/serviceController/SignificantMotionRecordingSource.kt`

`AndroidRecordingSources` is only an assembly point. It receives a `Context` long enough to obtain specific platform handles, but does not store the `Context`. Each source adapter lives in its own file and translates one engine source spec into one platform handler.

## 8. Review storage and transfer boundaries

Read:

- `../sensorservices/src/main/java/com/tomasrepcik/sensorbox/sensorservices/handlers/StorageHandler.kt`
- `../sensorservices/src/main/java/com/tomasrepcik/sensorbox/sensorservices/handlers/measurements/MeasurementMetadataWriter.kt`
- `../core/src/main/java/com/tomasrepcik/sensorbox/core/error/FileDiagnostics.kt`
- `../WearOsLib/src/main/java/com/tomasrepcik/sensorbox/wearoslib/files/WearFileMetadata.kt`
- `../WearOsLib/src/main/java/com/tomasrepcik/sensorbox/wearoslib/files/WearFilePathCodec.kt`
- `../WearOsLib/src/main/java/com/tomasrepcik/sensorbox/wearoslib/files/GooglePlayWearFileTransferClient.kt`

`StorageHandler` is a linear document-storage adapter. Extra measurement metadata, diagnostic entries, Wear file metadata, and command payloads use Kotlin serialization JSON. CSV remains the format for high-volume sensor samples. The Google Play transfer client is a linear open/write/close adapter; it does not own protocol decisions.

## 9. Finish with behavior tests

Read these after the production path so each fake has an obvious purpose:

- `../recording-core/src/test/kotlin/com/tomasrepcik/sensorbox/recording/RecordingEngineTest.kt`
- `../app/src/test/java/com/tomasrepcik/sensorbox/domain/paired/PairedRecordingCoordinatorTest.kt`
- `../wear/src/test/java/com/tomasrepcik/sensorbox/communication/WearCommandHandlerTest.kt`
- `../app/src/test/java/com/tomasrepcik/sensorbox/presentation/main/FeatureViewModelTest.kt`
- `../WearOsLib/src/test/java/com/tomasrepcik/sensorbox/wearoslib/protocol/WearCommandCodecTest.kt`

The tests should answer four questions: what user intent entered, which seam was called, what observable state or command resulted, and what cleanup happened after failure.
