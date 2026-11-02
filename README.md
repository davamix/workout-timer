# Workout Timer

An Android interval timer for round-based workouts, built for personal use on a 10" tablet running Android 8.0.

A workout is made of rounds. Each round is an ordered list of timed exercises, with configurable rests between exercises and between rounds. Spoken cues from the device's built-in text-to-speech announce each exercise and count down its last three seconds.

## Status

In development. See:

- [Specification](docs/SPECIFICATION.md): what the app does.
- [Implementation plan](docs/IMPLEMENTATION_PLAN.md): how it is built, in phases, and the status of each phase.

## Building

You need Android Studio 2026.1.3 or later. Gradle uses its bundled JDK, so point `JAVA_HOME` at it.

| Task | Command |
|---|---|
| Build, run unit tests and all checks | `./gradlew build` |
| Install on a connected device | `./gradlew installDebug` |
| Run instrumented tests on the device | `./gradlew connectedDebugAndroidTest` |
