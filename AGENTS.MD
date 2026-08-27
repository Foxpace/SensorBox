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
UI is dumb and only renders the state. Root is responsible for delegating actions, events and
native side effects - it can use other classes to do so.

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

## Terminology

- Screen / Root Screen - a screen is a module that contains all the code for a single screen. It has its own ViewModel, UI and repos. It is responsible for rendering the state and delegating actions to the ViewModel.
- State - a state is an immutable data class that contains all the data needed to render the UI. It is returned by the ViewModel and rendered by the UI.
- Action - an action is a user interaction that is delegated to the ViewModel. It can be a button click, a gesture, a keyboard shortcut, etc.
- User intent - a user intent is a high-level action that the user wants to perform. It is translated into one or more actions that are delegated to the ViewModel. Naming is based on user intent, not technical terms. For example, "Open App" is a user intent, while "LaunchActivity" is a technical term. Contract between layers can be merged with technical term.
- Contract - defines communication between layers. Techncial terms can be used for these classes too.
- Repo - a repo is a class that is responsible for fetching data from a data source. It can be a local database, a remote API, or any other data source. It is used by the ViewModel to fetch data and return it as a state.
- Usecase - a usecase is a class that contains business logic and orchestrates data from multiple repos. It is used by the ViewModel to perform complex actions and return a state. It equals to a user intention to do something.

## People

You are agent.
Me as user I am programmer, and we are building this project in collaboration.
Explain your ideas, ask questions, give suggestions and think about possible architectural improvements
based on the current state of the project. Review the code and give feedback.
Be straight and go to the point.