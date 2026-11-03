# P3 — Storage: Handover

| | |
|---|---|
| **Status** | Done. Merged in pull request [#3](https://github.com/davamix/workout-timer/pull/3) on 2026-11-03. |
| **Spec** | [SPEC.md](SPEC.md), version v0.2 |
| **Branch** | `phase/p3-storage` |
| **Last session** | 2026-11-03 |

This document tracks the phase as it's built. Update it at the end of every session, so the next one can continue from here.

## Prompt to start or continue

Phase done: no prompt needed.

## Current state

All five tasks are done and merged into `main` through pull request [#3](https://github.com/davamix/workout-timer/pull/3). CI passed on the pull request (1m51s on `6a5561c`, the last code change, and again on the handover commit `e977362`).

- `./gradlew build` passes locally: 155 unit tests (97 in `:core`, 58 in `:app`), 0 failures. Lint, ktlint and the no-INTERNET check are clean.
- `connectedDebugAndroidTest` on the tablet: 11 tests. 8 passed and 3 were skipped on purpose (the `PersistenceCheck` steps).
- The manual persistence check (SPEC.md §6) passed on the tablet: a workout and the voice setting survive a force-stop and a reboot.
- The merged manifest gains no permission or component. `:core`'s runtime classpath is still only `kotlin-stdlib` and `kotlinx-coroutines-core`.

What's new (base package `io.github.davamix.workouttimer`):

| Where | Files | Main API |
|---|---|---|
| `:core` `core/settings` | `Settings.kt`, `InMemorySettingsRepository.kt` | `Settings(voiceCuesEnabled = true)`, `SettingsRepository`, `InMemorySettingsRepository` |
| `:app` `data/` | `WorkoutFileFormat.kt`, `AtomicFiles.kt`, `FileWorkoutRepository.kt`, `DataStoreSettingsRepository.kt` | `FileWorkoutRepository`, `DataStoreSettingsRepository`, `createSettingsDataStore`; internal: `WorkoutFileFormat`, `writeAtomically`, `isValidWorkoutId` |
| `:app` `di/AppContainer.kt` | — | `workoutRepository`, `settingsRepository` (lazy, one per process) |

On the tablet, the files are `files/workouts/<id>.json` and `files/datastore/settings.preferences_pb` under the app's private data. Read them with `adb shell run-as io.github.davamix.workouttimer.debug cat files/workouts/<id>.json`.

## Next steps

None for P3. P4 (workout engine) and P6 (workout management UI) can start now; their dependency P2 is done.

## Tasks

The task IDs come from SPEC.md §7.

| Task | Status | Notes |
|---|---|---|
| T1 | Done | `WorkoutFileFormatTest` (32, including 20 damaged cases). Each damaged case also checks the reason given. |
| T2 | Done | `AtomicFilesTest` (6), `FileWorkoutRepositoryTest` (15) |
| T3 | Done | `InMemorySettingsRepositoryTest` (3, `:core`), `DataStoreSettingsRepositoryTest` (5) |
| T4 | Done | `FileWorkoutRepositoryDeviceTest` (2) and `PersistenceCheck` (3 steps, skipped unless named). Merged manifest checked. |
| T5 | Done | Manual checks passed. KDoc on the public API. Pull request #3 merged. |

## Decisions and deviations

None of these change SPEC.md. They settle points it doesn't specify.

| Date | Decision | Reason |
|---|---|---|
| 2026-11-03 | `onWarning` messages give the file name and the reason. The `Throwable` passed with a parse failure is kotlinx.serialization's, and its message may quote a short fragment of the file. | The parser's error is what makes a damaged file easy to diagnose. It only reaches debug logs (plan §3), and workout names aren't sensitive. |
| 2026-11-03 | `FileWorkoutRepository` updates its in-memory list inside the same I/O block as the file move or delete, with no suspension point in between. | `withContext` can throw `CancellationException` after its block has finished. Updating afterwards could leave the list out of step with the files if the caller was cancelled. |
| 2026-11-03 | `save` encodes the workout before taking the lock, and checks the ID before loading. | An invalid ID fails at once, without touching the disk. The lock is held only for file work. |
| 2026-11-03 | Leftover `*.json.tmp` files are deleted on load whatever their base name. Every other file that isn't `<valid id>.json`, including directories, is ignored and kept. | The SPEC says leftovers from an interrupted save are deleted and other files left alone. A `.json.tmp` file can only come from `writeAtomically`. |
| 2026-11-03 | `DataStoreSettingsRepository` falls back to the defaults on a read `IOException` without reporting it. Only the corruption handler reports through `onWarning`. | The SPEC's constructor takes only the DataStore. A corrupted file is the case worth logging. |
| 2026-11-03 | `:app` unit tests use JUnit Jupiter through `testOptions.unitTests.all { it.useJUnitPlatform() }`. With AGP 9, `./gradlew build` runs only the debug unit tests (`testDebugUnitTest`). | AGP 9 only runs unit tests for the tested build type by default. Both variants share the same sources. |
| 2026-11-03 | Instrumented test methods use camelCase names, not backtick names with spaces. | DEX files before version 040 (API 30) don't allow spaces in method names, and `minSdk` is 26. |

## Known issues

- **USB reset after unlocking:** after `adb reboot` and unlocking, the tablet dropped off `adb devices` for about 10 s while the USB connection reset. Wait for it to come back as `device` before running commands. This could be added to the `tablet` skill.
- **Slow first launch:** the first launch after installing took 72 s this time. The `tablet` skill says about 30 s. Android 8 compiles the debug APK on that first launch, and it was the first launch after `installDebug installDebugAndroidTest`. Later launches are fast.
- **`sun.misc.Unsafe` warnings:** ktlint prints them on JDK 25. They are harmless (noted in P1).

## Pending wiring

None. P5, P6 and P7 take `workoutRepository` and `settingsRepository` from `AppContainer`. If P6 started with `InMemoryWorkoutRepository` or `InMemorySettingsRepository`, it switches to the container's repositories.

## How to verify

```
./gradlew build                                               # 155 unit tests (97 :core, 58 :app), lint, ktlint
./gradlew connectedDebugAndroidTest                           # tablet: 8 pass, 3 PersistenceCheck steps skipped (uninstalls the app afterwards)
./gradlew :core:dependencies --configuration runtimeClasspath # still only kotlin-stdlib and kotlinx-coroutines-core
```

The manual persistence check is in SPEC.md §6. In short (Git Bash, `MSYS_NO_PATHCONV=1`):

```
./gradlew installDebug installDebugAndroidTest
adb shell am instrument -w -e persistenceCheck <step> -e class io.github.davamix.workouttimer.data.PersistenceCheck#<step> io.github.davamix.workouttimer.debug.test/androidx.test.runner.AndroidJUnitRunner
```

Run the steps in this order: `seed`, force-stop, `verify`, `adb reboot` and unlock, `verify`, `clean`.

## Session log

Newest first.

### 2026-11-03 (close)

- **Done:** the user merged pull request #3. P3 is closed: `Done` in plan §5.1, done criteria ticked in SPEC.md.

### 2026-11-03

- **Done:**
  - Wrote SPEC.md v0.1. The user agreed to all four draft assumptions, so v0.2 is `Ready`.
  - Started the phase: P3 marked `In progress` on `main`, branch created.
  - Did T1–T5, one commit per task.
  - Ran the instrumented tests and the full manual persistence check on the tablet, including a reboot that the user unlocked.
  - Opened pull request #3.
- **Phase docs:** SPEC.md and HANDOVER.md were enough for the implementation. The specification and plan were only read while writing SPEC.md.
- **Notes:**
  - DataStore works in plain JVM unit tests. A second DataStore on the same file needs the first one's scope to be cancelled and joined.
  - `kotlinx.serialization` accepts `1e12` but rejects it as an `Int`, and rejects `2.5` and quoted `schemaVersion` values, as the SPEC requires.
  - `adb shell am instrument` with `-e persistenceCheck <step>` works without `connectedDebugAndroidTest`, so the app's data isn't wiped.
