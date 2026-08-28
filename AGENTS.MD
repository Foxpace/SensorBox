# VoidLauncher

## Project

This project is a SensorBox app, which records raw Android and Wear OS sensor samples to local CSV files. The app uses Android's system folder picker, and recordings never require cloud storage or an account.

The intent of the project is to provide a simple and reliable way to record sensor data from Android and Wear OS devices, while maintaining user privacy and control over their data.


## Coding style

The simplicity is above all. The code should be simple, readable and verbose
enough that it is simple to understand. Methods and logic parts should be small enough to skim through
and have nuanced gaps between them.

Everything is named by the intent of the user, and only major contracts can have names
containing technical terms. Practically, the navigation in code is the navigation of the user.

The app uses MVI architecture - UI is dumb, calls the ViewModel, and it orchestrates data sources and APIs.
The ViewModel comes back with a state that is rendered by the UI. The state is immutable and contains all the data needed to render the UI.

Every screen is its own module and cross-cutting concerns are in their own modules.
The modules are small and have a single responsibility.
Every screen has root composable, where the viewmodel and UI meet ends.
The UI only talks to viewmodel, not repos. Repos and usecases are part of the viewmodels.
UI is dumb and only renders the state. The root composable delegates intents and performs
native side effects. It can use other classes to do so.

## Testing

Every ViewModel and repo has its own tests on unit level.
Fakes are only permitted and mocks only for third party implemenations, which
would be hard to fake.

Tests follow the same coding style as the main code and BDD - descriptions are composed of
Given, When, Then and test contains these comments too as its id divided.

Fixtures are used to create conditions for test executiona and ideally should be reused
across tests.

The tests should be simple and readable, ideally only one line per statement in the description of the test.

Run only tests, which are required and at the end of large implementation, run the whole test suite.

## Glossary

### Recording domain

- Recording - the activity of capturing data.
- Recording session - one start-to-stop recording operation. It may run on the phone alone or on both phone and watch.
- Recording setup - editable choices made before recording.
- Recording request - the immutable command created from the setup when recording starts.
- Recording source - an independently managed data producer, such as sensors or GPS. One source may create several files.
- Paired recording - one session coordinated across phone and watch.
- Measurement - one device's stored output for a recording session. A metadata-only measurement is valid.
- Measurement file - one stored data stream within a measurement.
- Measurement metadata - facts about the whole measurement, not a user-visible recording source.
- Sample - one timestamped observation, normally stored as one row.
- Recording archive - the selected phone directory containing phone and synced watch measurements.
- Sync - copying watch measurements to the phone archive without merging or removing the watch originals.
- Sensor - a sensor exposed by the phone or watch. Sensor type names its kind, such as accelerometer.
- Use phone and watch in domain code. Reserve Android and Wear OS for platform code.

### MVI

- Screen - stateless UI that renders state and sends intents.
- Intent - something the UI asks the ViewModel to handle. Do not use Action as a synonym.
- State - immutable data needed to render a screen.
- Effect - a one-time operation outside state rendering, such as navigation or requesting permission.
- ViewModel - owns state, handles intents, and coordinates domain operations.
- Reducer - a pure state transition.
- Contract - the state, intents, effects, and reducer used by a screen.
- Repo - a boundary for reading or changing stored data or platform state.
- Use case - one named domain operation or user goal.

## People

You are agent.
Me as user I am programmer, and we are building this project in collaboration.
Explain your ideas, ask questions, give suggestions and think about possible architectural improvements
based on the current state of the project. Review the code and give feedback.
Be straight and go to the point.
