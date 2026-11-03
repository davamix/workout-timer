# P3 — Storage: Specification

| | |
|---|---|
| **Status** | Ready, v0.2 |
| **Phase** | P3, see [Implementation plan §5](../../IMPLEMENTATION_PLAN.md#5-phases) |
| **Depends on** | P2 (done) |
| **Branch** | `phase/p3-storage` |
| **Handover** | [HANDOVER.md](HANDOVER.md) |

This document holds everything needed to implement the phase. Working on it shouldn't require reading the full specification or plan.

## 1. Goal

Workouts and settings are stored on the tablet and survive app restarts and reboots. At the end of this phase:

- `FileWorkoutRepository` implements P2's `WorkoutRepository`, with one JSON file per workout, written atomically;
- `DataStoreSettingsRepository` stores the voice-cue setting (`TTS-9`) in DataStore;
- `AppContainer` creates both, ready for the later phases.

There is no UI in this phase. Later phases build on it:

| Phase | Uses |
|---|---|
| P5 | `SettingsRepository.settings`, to apply the voice setting |
| P6 | `WorkoutRepository` (list and editor), `SettingsRepository` (settings screen) |
| P7 | `WorkoutRepository.get`, to load the workout to run |
| P8 | checks `WM-8` end to end through the UI |

## 2. Requirements covered

Quoted from [SPECIFICATION.md](../../SPECIFICATION.md) v0.6.

| ID | Requirement (quoted) | What this phase does for it |
|---|---|---|
| WM-3 | "Several workouts can be saved on the device and are listed on the home screen." | Stores any number of workouts and lists them all through `workouts`. The list screen is P6. |
| WM-4 | "Workouts can be created, edited, duplicated and deleted in the app. The editor is the only way to create workouts." | The storage operations: save a new workout, replace an edited one, save a duplicate (made with P2's `duplicate()`), delete one. The screens are P6. |
| WM-8 | "Workouts are stored locally and survive app restarts and device reboots. There is no cloud backup: uninstalling the app deletes them." | Files in the app's private storage, read back on the next start. Backup stays disabled (P1, `D-11`). Android deletes private storage on uninstall. |
| TTS-9 | "A setting turns voice cues on or off. The choice is remembered. With voice cues off, the timer, the screen and the round-end sound work the same." | Stores the setting and remembers it across restarts. The settings screen is P6; applying the setting to the cues is P5. |

### Specification sections this phase implements (quoted)

**§4.2, defaults and unused values:**

> - **Defaults:** an optional value that isn't set falls back to the workout default. Changing a default updates every exercise or round that doesn't set its own value. Those that set their own keep it.
> - **Unused values:** two values are never used. The last exercise of a round has no exercise rest, because the round rest runs instead. The last round has no round rest, because the workout ends. The editor marks these fields as not applicable but keeps their values, so they apply again if the exercise or round is moved.

For storage this means:

- an own value that isn't set stays unset after saving and loading, even if the default has the same value, so it stays linked to the default;
- unused values are stored like any other.

### Plan decisions this phase implements (quoted)

| ID | Decision |
|---|---|
| D-4 | "Each workout is stored as one JSON file (kotlinx.serialization), written atomically." |
| D-5 | "Settings are stored in Jetpack DataStore (Preferences)." |
| D-11 | "Android Auto Backup is disabled (`allowBackup="false"`)." Already done in P1; this phase keeps it. |

Plan §2.4: "Each workout is stored as `workouts/<id>.json` in the app's private storage. Writes go to a temporary file first, which is then moved into place, so a crash never leaves a half-written workout. The file format has a `schemaVersion` field, so it can change later without losing data."

## 3. Scope

### In scope

- **JSON format, version 1:** one document per workout, with `schemaVersion`.
- **Atomic writes:** a helper that either replaces a file completely or leaves it unchanged.
- **`FileWorkoutRepository`:** implements `WorkoutRepository`. Damaged files are skipped and logged, never fatal.
- **Settings:**
  - `Settings`, `SettingsRepository` and `InMemorySettingsRepository` in `:core`;
  - `DataStoreSettingsRepository` in `:app`.
- **Wiring:** `AppContainer` creates both repositories. Storage warnings are logged in debug builds only.
- **Test setup:** JUnit Jupiter unit tests in `:app`.
- **Tablet:** instrumented tests, and a persistence check across closing the app and a reboot.

### Out of scope

| What | Phase |
|---|---|
| The workout list, editor and settings screens; how the list is sorted; error messages shown to the user | P6 |
| Applying the voice setting to the cues | P5 |
| Loading a workout into the runner | P7 |
| Checking persistence through the UI, as part of acceptance | P8 |
| Migrating from an older format: version 1 is the first | Later, when a version 2 exists |
| Backup, export or import of workouts | Not in v1 (specification §2, §10) |

## 4. Design

### 4.1 Packages and files owned

The base package is `io.github.davamix.workouttimer`.

```
core/src/main/kotlin/io/github/davamix/workouttimer/core/settings/      (new package, owned by P3)
    Settings.kt                         Settings, SettingsRepository
    InMemorySettingsRepository.kt
core/src/test/kotlin/io/github/davamix/workouttimer/core/settings/
    InMemorySettingsRepositoryTest.kt

app/src/main/kotlin/io/github/davamix/workouttimer/data/               (owned by P3)
    WorkoutFileFormat.kt                JSON format v1: file classes, mapping, encode/decode
    AtomicFiles.kt                      writeAtomically()
    FileWorkoutRepository.kt
    DataStoreSettingsRepository.kt      DataStoreSettingsRepository, createSettingsDataStore()
app/src/test/kotlin/io/github/davamix/workouttimer/data/
    SampleWorkouts.kt                   fixtures for :app tests, including the §4.4 example
    WorkoutFileFormatTest.kt
    AtomicFilesTest.kt
    FileWorkoutRepositoryTest.kt
    DataStoreSettingsRepositoryTest.kt
app/src/androidTest/kotlin/io/github/davamix/workouttimer/data/
    FileWorkoutRepositoryDeviceTest.kt
    PersistenceCheck.kt                 seed / verify / clean steps for the manual check (§6)
```

**Shared files changed:**

- `app/build.gradle.kts`:
  - the dependencies in §4.4;
  - JUnit Platform for `:app` unit tests: `testOptions { unitTests.all { it.useJUnitPlatform() } }`.

  P6 may add the same unit-test setup in parallel. Whichever phase merges second keeps a single copy.
- `app/src/main/kotlin/io/github/davamix/workouttimer/di/AppContainer.kt`: adds `workoutRepository` and `settingsRepository`.
- `gradle/libs.versions.toml`: no change. Every library P3 needs is already declared.

**Why the settings interface is in `:core`:** plan §2.1 lists the `SettingsRepository` under `app/data/`. This phase splits it the same way P2 split `WorkoutRepository`: the interface and an in-memory fake in `:core`, and the DataStore implementation in `:app`. P5's planner tests and P6's ViewModel tests can then use the fake, and the interface doesn't depend on DataStore. `:core` stays free of Android.

### 4.2 Interfaces and data

```kotlin
// :core, package core.settings

/** App-wide settings. */
data class Settings(
    val voiceCuesEnabled: Boolean = true,      // TTS-9; on by default (Q1, answered)
)

interface SettingsRepository {
    /** The current settings on collection, then again after each change. Emits only when a value changes. */
    val settings: Flow<Settings>

    suspend fun setVoiceCuesEnabled(enabled: Boolean)
}

/** For tests and previews. Nothing survives the process. */
class InMemorySettingsRepository(initial: Settings = Settings()) : SettingsRepository
```

```kotlin
// :app, package data

/** WorkoutRepository backed by one JSON file per workout in [directory] (§4.3). One instance per directory. */
class FileWorkoutRepository(
    directory: File,                                            // <filesDir>/workouts in the app
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    onWarning: (message: String, error: Throwable) -> Unit = { _, _ -> },
) : WorkoutRepository

/** SettingsRepository backed by a Preferences DataStore. */
class DataStoreSettingsRepository(
    dataStore: DataStore<Preferences>,
) : SettingsRepository

/**
 * The settings DataStore, with corruption handling (§4.3). Only one may be active per file in a process:
 * AppContainer creates it once.
 */
fun createSettingsDataStore(
    file: File,                                                 // name must end in ".preferences_pb"
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    onWarning: (message: String, error: Throwable) -> Unit = { _, _ -> },
): DataStore<Preferences>

// Internal to :app, used by the repository and its tests
internal object WorkoutFileFormat {
    const val SCHEMA_VERSION = 1
    fun encode(workout: Workout): String
    fun decode(text: String): Workout                          // throws WorkoutFormatException with the reason
}
internal class WorkoutFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)

internal fun isValidWorkoutId(id: String): Boolean             // matches [A-Za-z0-9_-]{1,64}

/** Calls [write] on a temporary file, then moves it over [target]. [target] ends up fully replaced or unchanged. */
internal fun writeAtomically(target: File, write: (OutputStream) -> Unit)
```

```kotlin
// :app, AppContainer additions
val workoutRepository: WorkoutRepository           // FileWorkoutRepository(File(context.filesDir, "workouts"), …)
val settingsRepository: SettingsRepository         // DataStoreSettingsRepository(createSettingsDataStore(
                                                   //     context.preferencesDataStoreFile("settings"), …))
```

Both are created lazily, once per process.

**JSON format, version 1.** The §4.4 example as stored (IDs shortened):

```json
{
    "schemaVersion": 1,
    "id": "6f1c…",
    "name": "Example",
    "defaultExerciseDurationSeconds": 40,
    "defaultExerciseRestSeconds": 15,
    "defaultRoundRestSeconds": 60,
    "rounds": [
        {
            "id": "a2d4…",
            "name": "Warm-up",
            "restSeconds": 30,
            "exercises": [
                { "id": "c9e0…", "name": "Jumping jacks", "durationSeconds": 30 },
                { "id": "17b3…", "name": "Arm circles", "durationSeconds": 10, "restSeconds": 5 },
                { "id": "e85f…", "name": "High knees" }
            ]
        },
        {
            "id": "0b7a…",
            "name": "Core",
            "exercises": [
                { "id": "5d21…", "name": "Plank", "durationSeconds": 60, "restSeconds": 30 },
                { "id": "f3c8…", "name": "Crunches" }
            ]
        }
    ]
}
```

Format rules:

- One JSON object per file, in UTF-8, pretty-printed so it can be read with `adb`.
- `schemaVersion` is required and is `1` in this version. Any change to the format (a field added, renamed or removed) needs a new version, and reading branches on the version number. So `decode` reads `schemaVersion` first, then decodes the rest with the classes for that version.
- Field names match the P2 model. The format still has its own `@Serializable` classes (internal to `WorkoutFileFormat.kt`), so renaming a model property can't silently change the file format.
- Required fields: `schemaVersion`, every `id` and `name`, the three workout defaults, `rounds` and `exercises`.
- Optional fields: `restSeconds` on rounds, and `durationSeconds` and `restSeconds` on exercises. When they aren't set, they are left out of the file. Reading also accepts an explicit `null`.
- Reading is strict: an unknown field makes the file damaged (§4.3).

### 4.3 Behaviour

**Files**, all in the app's private storage:

```
<filesDir>/
├── workouts/
│   ├── <workout id>.json          one per workout
│   └── <workout id>.json.tmp      exists only during a save, or after a save was interrupted
└── datastore/
    └── settings.preferences_pb    the settings
```

**Workout IDs and file names.** A workout's file name is its ID plus `.json`. Only IDs that match `[A-Za-z0-9_-]{1,64}` are stored, so an ID can never point outside the directory. The app's IDs are UUIDs from P2's `newId()`, which always match.

- `save` with any other ID throws `IllegalArgumentException`, a programming error.
- `get` with such an ID returns `null`, and `delete` does nothing.

**Loading.** It happens once per instance, on first use: the first collection of `workouts`, or the first `get`, `save` or `delete`. It runs on `ioDispatcher`:

1. Create the directory if it's missing.
2. Delete leftover `*.json.tmp` files. They come from a save that was interrupted; the `.json` file it was replacing, if any, is intact.
3. Read every file named `<valid id>.json`. Any other file is ignored and left alone.
4. Skip any damaged file and call `onWarning` with its name and the reason. Never delete, move or change it, so it can be inspected with `adb`. The other workouts load normally.

A file is **damaged** when:

| Case | Example |
|---|---|
| It can't be read | an I/O error |
| It isn't a JSON object | empty file, truncated text, a JSON array |
| `schemaVersion` is missing, isn't an integer, or isn't `1` | a file written by a newer version |
| A required field is missing, a field has the wrong type or doesn't fit in an `Int`, or an unknown field is present | `"name": 3`, `"durationSeconds": 1e12` |
| The `id` inside differs from the file name | a file copied or renamed by hand |

The repository doesn't check `WM-7` validation. A well-formed file with, for example, an empty round list loads as it is (Q2, answered).

**Saving** a workout, atomically:

1. Check the ID (see above).
2. Write the JSON to `<id>.json.tmp`, flush it, and sync it to disk (`FileOutputStream.fd.sync()`), so a power cut can't leave an empty file after the move.
3. Move the temporary file over `<id>.json` with `Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE)`. This is a single rename, available from API 26.
4. Update the in-memory list, which makes `workouts` emit.

If any step fails, the temporary file is deleted and `save` throws `IOException`. The `.json` file stays as it was (the old version, or no file), and so does the in-memory list.

The same `save` call creates (`WM-4` create), replaces (edit), and stores a duplicate made with P2's `Workout.duplicate()`, which has a new ID.

**Deleting** removes `<id>.json` if it exists, then updates the in-memory list. If the file can't be deleted, `delete` throws `IOException` and the list is unchanged. An unknown ID does nothing.

**Concurrency.** One `Mutex` guards loading, saving and deleting, so operations apply in call order and the in-memory list always matches the files. All file work runs on `ioDispatcher`. Only one instance may use a directory at a time; `AppContainer` holds the only one in the app.

**The `workouts` flow** has the same state-flow behaviour as P2's `InMemoryWorkoutRepository`:

- it emits the current list on collection, and again after each change;
- saving an identical workout, or deleting an unknown ID, doesn't emit;
- a slow collector may skip intermediate lists, but always gets the latest one;
- the order isn't specified (P6 sorts the list).

**Settings:**

- One Preferences DataStore file, `settings.preferences_pb`. The voice setting is the Boolean key `voice_cues_enabled`.
- A missing key means the default: voice cues on (Q1, answered).
- A corrupted file is replaced with empty preferences (`ReplaceFileCorruptionHandler`), which means defaults. `onWarning` is called.
- An `IOException` while reading emits the defaults instead of failing the flow.
- `settings` emits only when a value changes (`distinctUntilChanged`).
- `setVoiceCuesEnabled` writes with `dataStore.edit`. An I/O error propagates to the caller.
- DataStore throws if two instances are active on the same file in one process, so `AppContainer` creates exactly one.

**Logging.** `AppContainer` passes an `onWarning` that calls `Log.w("WorkoutStorage", message, error)`, but only when the app is debuggable (`ApplicationInfo.FLAG_DEBUGGABLE`). Release builds don't log (plan §3). Messages name the file and the reason, not the workout's contents. The repositories never call `android.util.Log` themselves, so they run in JVM unit tests.

### 4.4 Dependencies

| Library | Version | Where | Why |
|---|---|---|---|
| `kotlinx-serialization-json` | 1.12.0 | `:app` main | the JSON format (`D-4`). The serialization plugin is already applied in `:app`. |
| `androidx.datastore:datastore-preferences` | 1.3.0 | `:app` main | settings (`D-5`) |
| JUnit Jupiter + Platform launcher | 6.1.3 (BOM) | `:app` test | unit tests, as in `:core` |
| `kotlinx-coroutines-test` | 1.11.0 | `:app` test | `runTest` |

All are already in the version catalog. Instrumented tests use the existing AndroidX test libraries.

From P2: `Workout`, `Round`, `Exercise`, `newId()`, `duplicate()` and `WorkoutRepository`. P2 is merged, so no fakes are needed.

P2's test fixtures (`ExampleWorkouts.kt`) are in `:core`'s test sources, which `:app` tests can't see. Sharing them would need the `java-test-fixtures` plugin, a shared build change (P2 known issue). Instead, `:app` tests get their own small `SampleWorkouts.kt`, including the §4.4 example.

## 5. Security and privacy

Plan §3 applies. For this phase:

- **Private storage only.** Workouts go to `filesDir/workouts`, and settings to `filesDir/datastore`. Both are private to the app: no external storage, and no permissions needed.
- **No backup.** Both locations are in the `file` domain, which `backup_rules.xml` and `data_extraction_rules.xml` already exclude. Leave those files and `allowBackup="false"` unchanged.
- **No network.** The new libraries must not add `INTERNET` or any other permission, nor any component. `./gradlew build` already fails on `INTERNET`. Also check the merged manifest for any other new permission or component (`app/build/intermediates/merged_manifest/debug/…/AndroidManifest.xml`).
- **File names from IDs.** Only `[A-Za-z0-9_-]` IDs become file names, so no ID can reach outside `workouts/` (no `..`, no `/`).
- **Files are parsed strictly.** They are decoded into fixed, non-polymorphic classes with no class discriminator. A damaged or unexpected file is skipped; it can never crash the app.
- **No data loss on failure.** Atomic writes keep the old file if a save fails. Damaged files are kept, not deleted.
- **Logs.** Debug builds only, with file names and reasons, never workout contents.

## 6. Testing

Unit tests run in `./gradlew build` and in CI: locally on Windows, in CI on Linux. They must pass on both, so use `@TempDir` and close every stream before moving or deleting a file.

| Test | Type | Covers |
|---|---|---|
| `InMemorySettingsRepositoryTest`: voice cues on by default; a change is emitted; setting the same value doesn't emit | unit (`:core`) | TTS-9 (interface) |
| `WorkoutFileFormatTest`:<br>• round trip of the §4.4 example and of a one-exercise workout<br>• unset values stay unset, even when equal to the default; own values equal to the default stay set<br>• unused values are kept<br>• names with quotes, backslashes, accents and emoji<br>• a fixed version-1 document decodes to the expected workout, which pins the format<br>• `encode` writes `schemaVersion: 1` and leaves out unset values<br>• explicit `null` is accepted for optional values<br>• each damaged case in §4.3 throws `WorkoutFormatException` | unit | WM-1, WM-2 (stored as set), §4.2, D-4 |
| `AtomicFilesTest`:<br>• creates a new file<br>• fully replaces an existing one, including with shorter content<br>• a failure while writing leaves the old file unchanged and no temporary file<br>• a failure while writing a new file leaves no file at all | unit | D-4, plan §2.4 |
| `FileWorkoutRepositoryTest`:<br>• save and get<br>• several workouts are all listed<br>• save replaces a workout with the same ID<br>• a duplicate is stored as a separate workout<br>• delete; delete of an unknown ID<br>• a new instance on the same directory loads everything (restart)<br>• damaged files are skipped, reported to `onWarning` and left untouched, while the others load<br>• leftover `.tmp` files are deleted and other files are ignored<br>• invalid IDs: `save` throws, `get` returns `null`, `delete` does nothing<br>• a failed save (a directory placed at the `.tmp` path) throws `IOException` and leaves the files and the list unchanged<br>• `workouts` emits after each change and not after a no-op | unit | WM-3, WM-4, WM-8 |
| `DataStoreSettingsRepositoryTest`:<br>• defaults with no file<br>• set off, then on<br>• a new DataStore on the same file sees the stored value (restart), after the first one's scope is cancelled<br>• a corrupted file gives the defaults and calls `onWarning`<br>• emits only on change | unit | TTS-9 (remembered) |
| `FileWorkoutRepositoryDeviceTest`: save, reload with a new instance, replace and delete, in a scratch directory under the app's `filesDir` on the tablet; the directory is removed afterwards | instrumented | WM-8; atomic moves on the tablet's file system |
| `PersistenceCheck`: steps for the manual check below. Each step runs only when the `persistenceCheck` instrumentation argument names it, so `connectedDebugAndroidTest` skips them all. | instrumented, run by hand | WM-8, TTS-9 |

### Manual checks on the tablet

There is no UI to create workouts yet, so the check drives the real `AppContainer` repositories through instrumentation. `connectedDebugAndroidTest` uninstalls the app when it ends, which would wipe the data, so these steps use `adb shell am instrument` directly.

`PersistenceCheck` has three steps, each a test method:

- **`seed`** saves the workout "P3 persistence check" (fixed ID `p3-persistence-check`, two rounds, own and linked values) through `container.workoutRepository`, and turns voice cues off through `container.settingsRepository`;
- **`verify`** checks that the same workout loads, equal to the seeded one, and that voice cues are off;
- **`clean`** deletes the workout and turns voice cues back on.

To run one step (`<step>` is `seed`, `verify` or `clean`):

```
adb shell am instrument -w -e persistenceCheck <step> -e class io.github.davamix.workouttimer.data.PersistenceCheck#<step> io.github.davamix.workouttimer.debug.test/androidx.test.runner.AndroidJUnitRunner
```

The instrumentation package name is an assumption. Confirm it with `adb shell pm list instrumentation`.

| # | Step | Expected |
|---|---|---|
| 1 | `./gradlew installDebug installDebugAndroidTest` | Both APKs installed |
| 2 | Run `seed` | `OK (1 test)` |
| 3 | Run `adb shell am force-stop io.github.davamix.workouttimer.debug`, then `verify` | `OK (1 test)`: the data survives closing the app |
| 4 | Launch the app normally, then close it | It opens on the workout list as before |
| 5 | Run `adb reboot`, wait for the tablet, and ask the user to unlock it (private storage is only available after the first unlock) | The tablet is back as `device` in `adb devices` |
| 6 | Run `verify` | `OK (1 test)`: the data survives a reboot |
| 7 | Run `clean`, then check `adb shell run-as io.github.davamix.workouttimer.debug ls files/workouts` | `OK (1 test)`; no `p3-persistence-check.json` left |
| 8 | Read the app's logcat during steps 2–7 | No crash. No `WorkoutStorage` warnings. |

## 7. Tasks

| ID | Task |
|---|---|
| T1 | `:app` unit-test setup (JUnit Platform, test dependencies) and `kotlinx-serialization-json`. `WorkoutFileFormat.kt` (format version 1), `SampleWorkouts.kt`. Test: `WorkoutFileFormatTest`. |
| T2 | `AtomicFiles.kt` and `FileWorkoutRepository`. Tests: `AtomicFilesTest`, `FileWorkoutRepositoryTest`. |
| T3 | Settings: `core/settings` (`Settings`, `SettingsRepository`, `InMemorySettingsRepository`), and `DataStoreSettingsRepository` with `createSettingsDataStore` and the DataStore dependency. Tests: `InMemorySettingsRepositoryTest`, `DataStoreSettingsRepositoryTest`. |
| T4 | Wiring: `AppContainer` gets both repositories and the debug-only warning log. Instrumented `FileWorkoutRepositoryDeviceTest` and `PersistenceCheck`. Run `connectedDebugAndroidTest` on the tablet, and check the merged manifest. |
| T5 | Manual checks on the tablet (§6). KDoc on the public API for P5–P7; `./gradlew build`; handover; pull request. |

## 8. Done criteria

The checks that must all pass before the phase is merged.

- [x] All tasks done
- [x] Unit tests and lint pass locally and in CI
- [x] Instrumented tests pass on the tablet (`connectedDebugAndroidTest`)
- [x] Manual checks on the tablet pass: a workout and the voice setting survive closing the app and a reboot
- [x] The merged manifest gains no permission and no component
- [x] `:core` still has no Android dependency
- [x] HANDOVER.md is up to date, and the plan's status table is updated

## 9. Open questions

None at the moment.

### Answered

| # | Question | Decision |
|---|---|---|
| Q1 | What is the voice-cue setting on a fresh install? | **On.** Voice cues are the main way to follow a workout hands-free. |
| Q2 | Should the repository refuse a well-formed file whose workout fails `WM-7` validation? | **No, it loads as it is.** Only the editor writes files, and it validates first (P6). P7 must validate before building a timeline, as `Timeline.of` throws on invalid workouts. |
| Q3 | What should the user see if saving or deleting fails? | **P3 throws `IOException` and keeps the old data. P6 decides the message.** Suggested for P6: a short error, and the editor stays open with the changes kept. No specification change. |
| Q4 | Is a check driven by instrumentation acceptable for "workouts survive closing the app and rebooting", given there is no UI yet? | **Yes.** It uses the real `AppContainer` repositories on the tablet. P8 repeats the check through the UI as part of acceptance. |

## 10. Change log

| Version | Date | Changes |
|---|---|---|
| 0.1 | 2026-11-03 | First draft |
| 0.2 | 2026-11-03 | Q1–Q4 answered, confirming the draft assumptions. Status `Ready`. |
