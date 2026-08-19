# SensorBox architecture refactor plan

Status: implemented and verified on 2026-08-19. The phone emulator workflow passes. The Wear-to-phone emulator workflow is built but awaits a one-time Android Studio phone/watch pairing.

This file is the implementation source of truth for the Android 17 hard-cut refactor. Update the status boxes as work lands. Do not preserve an old interface merely because a later step still uses it. Each completed step must leave the repository in a valid intermediate state.

Related decisions:

- [0001: Use explicit application outcomes](adr/0001-use-explicit-application-outcomes.md)
- [0002: Separate the recording engine from Android adapters](adr/0002-separate-recording-engine-from-android-adapters.md)
- [0003: Coordinate paired recordings with protocol version 2](adr/0003-coordinate-paired-recordings-with-protocol-v2.md)
- [0004: Own presentation state by user workflow](adr/0004-own-presentation-state-by-user-workflow.md)

## Completion rules

- [x] Every module interface uses `AppResult<T>` for expected outcomes. Kotlin `Result<T>` does not cross a module interface.
- [x] Every intermediate implementation state compiles and passes the checks relevant to its changes.
- [ ] The final state passes unit tests, Detekt, phone and watch lint, Android test compilation, and both emulator workflows.
- [x] Tests cross the same interfaces used by production callers.
- [x] Diagnostics remain local until the user copies or shares them.
- [x] `sensorservices` has no dependency on `wearoslib`.
- [x] Unsupported Wear protocol versions fail explicitly. No compatibility layer remains.

## Phase 1: outcomes and diagnostics

- [x] Replace exception-backed `AppError` with a data-only error.
- [x] Add stable error codes for connectivity, external actions, measurement, permission, preferences, storage, validation, timeout, conflict, and unknown failures.
- [x] Add `AppResult.Success` and `AppResult.Failure` plus composition helpers.
- [x] Translate expected exceptions at infrastructure adapters and rethrow coroutine cancellation.
- [x] Add `DiagnosticLogger` for writes and `DiagnosticsStore` for read, export, and clear operations.
- [x] Store structured, redacted diagnostics in app-private rotating files.
- [x] Keep the current and previous diagnostic file at no more than 1 MB each.
- [x] Expose view, copy, share-text, share-file, and clear actions from Settings.
- [x] Add tests for outcome composition, cancellation, redaction, rotation, and logger write failure.

Diagnostic entries may contain timestamps, severity, stable codes, operations, app/build information, Android/device model information, process names, session IDs, lifecycle states, stop reasons, source types, protocol versions, retry counts, sanitized exception classes, stack traces, durations, source counts, and sampling periods.

Diagnostic entries must not contain samples, coordinates, annotations, command payloads, full recording names, complete file names, SAF URIs, or persistent device identifiers.

## Phase 2: pure recording engine

- [x] Add a pure Kotlin `recording-core` module.
- [x] Define typed recording plans, session IDs, source types, stop reasons, states, commands, and terminal events.
- [x] Define the small source role used internally by the engine.
- [x] Prepare and start sources in stable order.
- [x] On start failure, stop all prepared sources in reverse order.
- [x] On stop, attempt every active source and combine failures.
- [x] Make repeated stop idempotent.
- [x] Emit typed stop events for duration expiry, low battery, source failure, platform destruction, and user requests.
- [x] Test partial start, reverse cleanup, repeated stop, timed stop, low-battery stop, and combined cleanup failure.

The durable state sequence is `Idle`, `Preparing`, `Prepared`, `Running`, and `Stopping`. Terminal results are `RecordingStarted`, `RecordingStartRejected`, and `RecordingStopped` events. The engine returns to `Idle` after a terminal event.

## Phase 3: Android recording adapters

- [x] Keep Android sensors, GPS, activity recognition, significant motion, storage streams, alarms, wake locks, battery callbacks, notifications, and foreground hosting in `sensorservices`.
- [x] Replace `Bundle` source configuration with typed configuration.
- [x] Make `MeasurementService` translate intents into engine commands.
- [x] Publish engine state through an application-scoped read-only session repository.
- [x] Deliver typed session events to application-provided observers.
- [x] Remove Wear commands, codecs, connection collaborators, and paired flags from `sensorservices`.
- [x] Remove the `sensorservices -> wearoslib` Gradle dependency.

## Phase 4: Wear protocol version 2

- [x] Add session identity to every paired recording command.
- [x] Add prepare, commit, abort, stop, and correlated acknowledgement messages with typed outcomes.
- [x] Make handlers idempotent and return the prior outcome for duplicate commands.
- [x] Reject a command for a conflicting active session with `CONFLICT`.
- [x] Reject unsupported protocol versions.
- [x] Keep command models, encoding, decoding, connection discovery, and byte transport in `WearOsLib`.
- [x] Add round-trip, unsupported-version, duplicate-command, and malformed-payload tests.

## Phase 5: paired recording workflow

- [x] Put phone and watch coordination in one phone-side workflow.
- [x] Prepare both sides before committing either side.
- [x] Give prepare 10 seconds and commit acknowledgement 5 seconds.
- [x] Retry each message twice with the same session ID.
- [x] Abort both sides after rejection or final timeout.
- [x] Never fall back to phone-only recording when the plan contains watch sources.
- [x] Propagate every automatic local stop to the watch through the typed session observer.
- [x] Stop both sides even if one stop fails, then return a combined outcome.
- [x] Test remote rejection, prepare timeout, commit compensation, duplicate messages, and partial stop failure.

## Phase 6: presentation ownership

- [x] Make the application shell own navigation and only the session facts needed for navigation and screen-awake policy.
- [x] Give onboarding its own state, actions, effects, and ViewModel.
- [x] Give the full recording journey one navigation-graph-scoped ViewModel.
- [x] Give settings its own state, actions, effects, and ViewModel.
- [x] Keep child routes stateless and pass focused state and actions.
- [x] Move recording request construction, validation, permission resolution, and start effects into the recording feature.
- [x] Test ViewModel actions, state transitions, effects, and representative error codes.

The recording journey includes sensor selection, sensor details, live preview, measurement setup, permission resolution, active recording, and annotations.

## Phase 7: typed Wear handlers

- [x] Keep listener services limited to callback validation, decoding, and dispatch.
- [x] Put phone command behavior in `PhoneWearCommandHandler` in `app`.
- [x] Put watch command behavior in `WearCommandHandler` in `wear`.
- [x] Keep application preferences, permissions, recording behavior, and navigation out of `WearOsLib`.
- [x] Test handlers with typed commands and local adapters, without Google Play Services callback objects.

## Final verification

- [x] `./gradlew testDebugUnitTest detekt`
- [x] `./gradlew :app:assembleDebug :wear:assembleDebug`
- [x] `./gradlew :app:lintDebug :wear:lintDebug`
- [x] `./gradlew :app:compileDebugAndroidTestKotlin :wear:compileDebugAndroidTestKotlin`
- [x] Run the phone sensor recording emulator workflow.
- [ ] Run the Wear-to-phone transfer emulator workflow.
- [x] Update README architecture and test instructions.
- [x] Confirm the final worktree contains no obsolete result, protocol, presentation, or service interfaces.

Wear workflow note: `tools/emulator/run_wear_sync_test.sh` reached its pairing precondition and exited with the expected guidance because `SensorBox_Wear_API_37` reports no paired phone. APKs and both instrumentation suites compile. Complete the one-time Pair Wearable flow in Android Studio, then rerun the script.
