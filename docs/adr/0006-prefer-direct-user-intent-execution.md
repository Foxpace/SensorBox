# Prefer direct user-intent execution

SensorBox favors direct execution over staged technical protocols. Recording starts from one complete plan and stops from one explicit reason. Phone and watch coordination uses start and stop commands. Current workflow state rejects duplicate or conflicting user intents, so orchestration does not add a mutex when one owner already serializes those intents.

Interfaces exist for external dependencies or roles with multiple meaningful adapters. A one-off interface used only to fake a pass-through class is removed. Internal construction uses factories when an Android lifecycle entry point must assemble several session objects. Factory and method names state which user operation or domain object they create.

Recording source adapters live in separate files. They do not retain an Android `Context`; an operation receives the current context or a factory uses it while constructing the session. Model conversions, validation, serialization, and factories live beside their models instead of inside orchestration methods.

Wear commands are serializable data classes encoded as versioned JSON on both phone and watch. Sensor discovery is an explicit `RequestAvailableSensors` command answered by `AvailableSensors`. ViewModels own presentation state and delegate storage selection, permission checks, Wear discovery, recording control, and session observation to user-intent use cases.

A delayed start is presentation behavior. The recording ViewModel exposes and advances the countdown, then calls the recording use case after it reaches zero. No folder, service, recording source, or Wear start command is created while the countdown is running.

After a paired start reaches each device, each foreground service owns its recording independently. The phone copies the same duration into the Wear start command, and both services pass that duration to their own `RecordingEngine`. Each engine schedules its own local duration stop. Losing the phone-to-Wear connection does not stop either recording or cancel either timer. If the Wear start command was sent but its result is lost, the phone keeps recording; an explicit Wear rejection still fails and cleans up the paired start. When a local timer expires, that device finishes first and treats the peer stop notification as best effort. A delivery failure cannot reopen the finished session or prevent another recording from starting.
