# Recording archive

The recording archive is the selected phone directory containing phone measurements and synced watch measurements.

## Language

**Measurement**

One device's stored output for a recording session. A phone and watch can each create a measurement for the same paired recording. A metadata-only measurement is valid.

_Avoid:_ Recording session, recording file, sensor measurement

**Measurement file**

One data stream inside a measurement, such as accelerometer samples or GPS coordinates.

_Avoid:_ Measurement

**Measurement metadata**

Session-level facts stored with a measurement, including its start time, notes, alarms, duration, and sensor ranges.

_Avoid:_ Sensor metadata

**Recording session**

One start-to-stop recording operation. It can run on the phone alone or on both phone and watch.

_Avoid:_ Measurement session

**Recording setup**

The editable choices made before a recording starts.

_Avoid:_ Measurement setup, recording configuration

**Recording request**

The immutable command created from the recording setup when recording starts.

_Avoid:_ Recording plan, measurement request, launch configuration

**Recording source**

An independently managed data producer, such as sensors or GPS. One recording source can create several measurement files.

_Avoid:_ Recording device

**Paired recording**

One recording session coordinated across the phone and watch.

_Avoid:_ watch recording

**Sample**

One timestamped observation, normally stored as one row in a measurement file.

_Avoid:_ Sensor measurement

**Sync**

Copying watch measurements to the phone recording archive without merging them or removing the watch originals.

_Avoid:_ Merge, move
