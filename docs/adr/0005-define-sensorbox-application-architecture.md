# Define the SensorBox application architecture

SensorBox organizes code by user workflow and keeps platform code at the edge. `core-common` owns application outcomes and shared clocks. `core` owns reusable Android storage, preferences, and diagnostics. `recording-core` executes a complete recording plan without Android dependencies. `sensorservices` turns that plan into Android sensor and storage work. `WearOsLib` owns Wear command models and Google Play transport. `app` and `wear` own presentation and the phone or watch behavior behind received commands.

Each screen owns immutable state, user actions, effects, its ViewModel, and the use cases needed by that ViewModel. Root screens connect Compose to the ViewModel and perform native effects. Child UI renders state and reports user actions. Application navigation follows user workflows rather than technical tiers.

Expected failures cross module interfaces as `AppResult<T>` with stable `AppErrorCode` values. Infrastructure adapters translate platform exceptions. Diagnostics remain local to the device. Tests use fakes at real seams and assert the outcomes seen by production callers.

CSV is reserved for recorded sample streams. Other structured data that crosses a process boundary or is stored on disk uses Kotlin serialization with explicit serializable models. This includes Wear commands, Wear file metadata, measurement metadata, and diagnostic entries. JSON codecs live beside their models and reject malformed input at the boundary.

Dependencies point toward policy. Android and Google Play types do not enter `recording-core`, protocol models, application outcomes, or presentation state. Phone and watch applications may depend on Android adapters and Wear transport, while those lower modules do not depend on application presentation or paired-recording policy.
