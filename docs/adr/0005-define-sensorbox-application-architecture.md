# Define the SensorBox application architecture

SensorBox organizes code by user workflow and keeps platform code at the edge. `core-common` owns application outcomes and shared clocks. `core` owns reusable Android storage, preferences, and diagnostics. `recording-core` executes a complete recording request without Android dependencies. `sensorservices` turns that request into Android sensor and storage work. `WearOsLib` owns Wear command models, Google Play transport, and the acknowledged exchange mechanics used by recording start and stop. `app` and `wear` own presentation, recording-session decisions, and the phone or watch behavior behind received commands.

Each screen owns immutable state, intents, effects, its ViewModel, and the use cases needed by that ViewModel. Root composables connect Compose to the ViewModel and perform native effects. Child UI renders state and sends intents. Application navigation follows user workflows rather than technical tiers.

Expected failures cross module interfaces as `AppResult<T>` with stable `AppErrorCode` values. Infrastructure adapters translate platform exceptions. Diagnostics remain local to the device. Tests use fakes at real seams and assert the outcomes seen by production callers.

CSV is reserved for recorded sample streams. Other structured data that crosses a process boundary or is stored on disk uses Kotlin serialization with explicit serializable models. This includes Wear commands, Wear file metadata, measurement metadata, and diagnostic entries. JSON codecs live beside their models and reject malformed input at the boundary.

Dependencies point toward policy. Android and Google Play types do not enter `recording-core`, protocol models, application outcomes, or presentation state. Phone and watch applications may depend on Android adapters and Wear transport, while those lower modules do not depend on application presentation or paired-recording policy.

Stateful implementations expose interfaces named for each caller's intent. One phone recording controller backs UI start and stop, automatic phone-stop handling, and stop requests received from the watch, but each caller sees only its focused interface. Sensor availability similarly owns phone sources, watch discovery, connection state, and received watch sensors behind one observable use case. Android foreground-service lifecycle is coordinated by a host session so the service itself remains an intent and lifecycle adapter.

## Small-application dependency exception

SensorBox is a small application with substantial legacy code. A ViewModel may depend directly on a focused repository or use case when another delegation layer would not create a useful seam. This exception does not relax MVI: rendering UI receives immutable state and sends intents, ViewModels own state transitions, repositories return product values, and Android or vendor UI types stay outside ViewModels, contracts, and domain ports.

New code should make feature ownership visible through focused contracts and packages for shell, onboarding, recording, measurements, and settings. The legacy `presentation.main` package may remain while the application is small; moving files only to satisfy a directory convention is not required. This exception ends when a touched area mixes screen contracts, bypasses its ViewModel, or exposes mutable or platform-owned state. Cross-cutting preferences, diagnostics, clocks, formatting, and theme remain in core modules. Recording session state and events belong to `recording-core`; `sensorservices` only implements Android recording adapters.

## Failure policy

Operational failures are recorded and exposed through the process-wide `AppFailureStore`. The first visible failure remains until the user dismisses it; later failures are still recorded. Phone and Wear shells render only localized messages selected by stable `AppErrorCode` values. Diagnostic messages, causes, file paths, and context are never rendered.

Validation guidance, missing permissions, an unselected archive, an empty source selection, and user cancellation remain local screen outcomes. A source that fails after recording has started is also non-terminal: the engine stops and removes that source, emits `RecordingEvent.SourceFailed`, and continues the session. The foreground host records the source and cleanup errors. Start failures, terminal cleanup failures, and host failures remain operational failures and use the global path.

## Effects and platform boundaries

Each state owner delivers one-time effects through a buffered channel exposed as a Flow. Roots may execute native operations such as permission launchers, document pickers, navigation, and share intents, but they do not read repositories or stores. Roots translate native results into typed product values and return launch failures to their ViewModel. Platform adapters hide sensor, location, power, resource, file, and content-provider APIs behind focused ports.
