# Separate the recording engine from Android adapters

Recording state, scheduling policy, source coordination, cleanup, and terminal events live in a pure Kotlin `recording-core` module. Android sensor capture, foreground hosting, notifications, wake locks, battery callbacks, GPS, and storage streams remain adapters in `sensorservices`.

This split keeps the recording state machine testable without Android and prevents Wear transport policy from entering the local recording module.
