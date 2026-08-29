# Separate recording execution from Android and Wear adapters

Recording execution, source ordering, failure cleanup, duration limits, and terminal events live in the pure Kotlin `recording-core` module. Callers submit a complete recording request through direct start and stop operations. Android sensor capture, foreground hosting, notifications, wake locks, battery callbacks, GPS, and storage streams remain in `sensorservices`.

The phone owns session decisions when one user recording includes phone and watch sources. `WearOsLib` owns serializable command models, JSON encoding, connection discovery, byte transport, and the acknowledged recording-command exchange. That exchange correlates results by recording session and operation, bounds retries and timeouts, and translates peer failures into application outcomes. The phone and watch applications own command behavior and decide how a recording session reacts to those outcomes. The acknowledged exchange is limited to recording start and stop commands; discovery and result replies remain one-way messages.

This split keeps local recording execution independent of Android and Wear transport. A failed start stops every selected source so no sensor continues recording, but the application does not maintain a prepare, commit, abort, or rollback protocol.
