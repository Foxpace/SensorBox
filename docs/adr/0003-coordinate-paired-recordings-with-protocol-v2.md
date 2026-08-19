# Coordinate paired recordings with protocol version 2

Paired phone and watch recording uses a breaking, session-identified prepare, commit, abort, and stop protocol. Both sides must prepare before either side commits. Commands are idempotent, bounded by timeouts, and retried with the same session ID.

The phone owns the paired workflow. `WearOsLib` owns typed protocol encoding and transport, while phone and watch applications own command behavior. Unsupported protocol versions fail explicitly.
