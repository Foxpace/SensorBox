# Separate recording execution from Android and Wear adapters

Recording execution, source ordering, failure cleanup, duration limits, and terminal events live in the pure Kotlin `recording-core` module. Callers submit a complete recording request through direct start and stop operations. Android sensor capture, foreground hosting, notifications, wake locks, battery callbacks, GPS, and storage streams remain in `sensorservices`.

The phone owns coordination when one user recording includes phone and watch sources. `WearOsLib` owns serializable command models, JSON encoding, connection discovery, and byte transport. The phone and watch applications own command behavior. Session commands are idempotent, bounded by timeouts, and identified by one recording session ID.

This split keeps local recording execution independent of Android and Wear transport. A failed start stops every selected source so no sensor continues recording, but the application does not maintain a prepare, commit, abort, or rollback protocol.
