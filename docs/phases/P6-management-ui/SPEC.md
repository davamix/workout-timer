# P6 — Workout management UI: Specification

| | |
|---|---|
| **Status** | Ready, v0.4 |
| **Phase** | P6, see [Implementation plan §5](../../IMPLEMENTATION_PLAN.md#5-phases) |
| **Depends on** | P2 (done). It also uses P3 and P5, both done. |
| **Branch** | `phase/p6-management-ui` |
| **Handover** | [HANDOVER.md](HANDOVER.md) |

This document holds everything needed to implement the phase. Working on it shouldn't require reading the full specification or plan.

## 1. Goal

The three screens for managing workouts replace their P1 placeholders:

- the **workout list** (home): every saved workout with its total duration, and the actions start, edit, duplicate, delete (with confirmation), new workout and settings;
- the **workout editor**: the workout's name and defaults, its rounds and exercises, own values that show the default when they aren't set, unused rests marked, validation messages, a live total, and a confirmation before discarding changes;
- the **settings** screen: voice cues on or off.

They use P3's real storage through `AppContainer`. At the end of this phase, workouts are created, edited and kept on the tablet through the UI. Running them is P7.

Later phases build on it:

| Phase | Uses |
|---|---|
| P7 | the list's Start, which opens `RunnerRoute(workoutId)`; `formatTotalDuration()` for the completed screen's total |
| P8 | the end-to-end test: create a workout in the editor, run it, complete it |

## 2. Requirements covered

Quoted from [SPECIFICATION.md](../../SPECIFICATION.md) v0.8.

| ID | Requirement (quoted) | What this phase does for it |
|---|---|---|
| WM-1 | "Each exercise has a **name** and a **duration** in seconds. A workout-level default duration applies to every exercise that doesn't set its own." | Editor: a name and an own-duration field per exercise, and the default-duration field. An empty own duration shows the default (§4.3.2). |
| WM-2 | "Rests are set in seconds; 0 means no rest. The workout sets a default exercise rest and a default round rest. Each exercise can set its **own rest**, used after it. Each round can set its **own round rest**, used after it." | Editor: the two default rests, an own rest per exercise and per round. 0 is accepted. |
| WM-3 | "Several workouts can be saved on the device and are listed on the home screen." | The list shows every saved workout, sorted by name (§4.3.1). |
| WM-4 | "Workouts can be created, edited, duplicated and deleted in the app. The editor is the only way to create workouts." | List: new workout, edit, duplicate, delete. Editor: saves new and edited workouts. |
| WM-5 | "In the editor, rounds and exercises can be added, renamed, reordered, duplicated and removed. To repeat a round, duplicate it." | Editor: add, rename, move up and down, duplicate and remove, for rounds and for exercises (§4.3.3). |
| WM-6 | "The editor and the workout list show the workout's total duration, from Start to completion: get ready, exercises and rests." | Each list row and the editor's top bar show the total. In the editor it updates with every edit. |
| WM-7 | "Validation: the workout, each round and each exercise need a name; a workout has at least one round; a round has at least one exercise; durations are at least 1 s; rests are 0 s or more. No duration or rest can be longer than 99:59 (99 minutes 59 seconds)." | The editor shows P2's `validate()` errors next to their fields, and won't save a workout that has any (§4.3.4). |
| WM-8 | "Workouts are stored locally and survive app restarts and device reboots. There is no cloud backup: uninstalling the app deletes them." | Storage is P3's. This phase saves through it, and a manual check confirms that a workout created in the UI survives closing the app. |
| WM-9 | "Deleting a workout asks for confirmation." | A dialog on the list (§4.3.1). |
| WM-10 | "Leaving the editor with unsaved changes asks for confirmation before discarding them." | A dialog on Cancel, the back arrow and the system Back button (§4.3.5). |
| TTS-9 | "A setting turns voice cues on or off. The choice is remembered. With voice cues off, the timer, the screen and the round-end sound work the same." | The settings screen's switch. P3 stores the setting and P5 applies it. |
| NF-6 | "**Readability:** the time and exercise name are readable from about 3 m away. Touch targets are large." | Touch targets of at least 48 dp, and 56 dp for the main actions, with the app's enlarged type scale (§4.3.8). Reading the time and name from 3 m is the runner's part, P7. |
| NF-9 | "**Language:** the UI is in English. All user-facing text lives in resource files, so a Spanish translation can be added later without code changes." | Every text and content description of the three screens is in `strings_list.xml`, `strings_editor.xml` and `strings_settings.xml`. |

### Specification text this phase implements (quoted)

**§4.1, glossary:** "A rest of **0 seconds means no rest**: the next exercise or round starts immediately."

**§4.2, structure:**

```
Workout
├── name
├── default exercise duration (s)
├── default exercise rest (s, ≥ 0)
├── default round rest (s, ≥ 0)
└── rounds (ordered, at least 1)
    ├── name
    ├── round rest (s), optional: falls back to the default round rest
    └── exercises (ordered, at least 1)
        ├── name
        ├── duration (s), optional: falls back to the default exercise duration
        └── rest (s), optional: falls back to the default exercise rest
```

> - **Defaults:** an optional value that isn't set falls back to the workout default. Changing a default updates every exercise or round that doesn't set its own value. Those that set their own keep it.
> - **Unused values:** two values are never used. The last exercise of a round has no exercise rest, because the round rest runs instead. The last round has no round rest, because the workout ends. The editor marks these fields as not applicable but keeps their values, so they apply again if the exercise or round is moved.

**§4.4, the example** used by the tests and the manual checks. Workout "Example" has these defaults: exercise duration 40 s, exercise rest 15 s, round rest 60 s.

> - Round 1 "Warm-up", own round rest 30 s:
>   - Jumping jacks: 30 s
>   - Arm circles: 10 s, own rest 5 s
>   - High knees: default duration
> - Round 2 "Core":
>   - Plank: 60 s, own rest 30 s
>   - Crunches: default duration

Its total is **4:23**: "get ready 3 s + exercises 180 s + exercise rests 50 s + round rest 30 s."

**§6, screens:**

| Screen | Purpose | Main actions |
|---|---|---|
| **Workout list** (home) | Shows saved workouts with their total duration | Start, edit, duplicate, delete (with confirmation), new workout, open settings |
| **Workout editor** | Edits a workout's name, defaults, rounds and exercises, including their own durations and rests | Save, cancel (asks for confirmation if there are unsaved changes) |
| **Settings** | App-wide settings: voice cues on/off (`TTS-9`) | Back |

"All screens are landscape." The tablet's screen is 960 × 600 dp in landscape.

### Plan text this phase implements (quoted)

Plan §5.2, P6 scope:

> - **Workout list:** name, total duration, start, edit, duplicate, delete with confirmation, new workout, and the way to settings.
> - **Workout editor:**
>   - the workout's name and defaults;
>   - adding, renaming, reordering, duplicating and removing rounds and exercises;
>   - own durations and rests that show the default when not set;
>   - unused rest fields marked as not applicable;
>   - validation messages;
>   - a confirmation before discarding unsaved changes.
> - **Settings screen:** voice cues on/off.
>
> **Done when:** Compose UI tests cover creating, editing and validating a workout; a manual check on the tablet passes.

| ID | Decision |
|---|---|
| D-3 | "Manual dependency injection through an `AppContainer`." The ViewModels get their repositories from it. |
| D-13 | "Each UI feature keeps its text in its own resource file (`strings_<feature>.xml`)." |

## 3. Scope

### In scope

- **Workout list:** `WorkoutListViewModel` and the list screen.
- **Workout editor:**
  - the form model, which maps to and from P2's `Workout`, with its edit operations;
  - the mapping from validation errors to fields;
  - `WorkoutEditorViewModel` and the editor screen.
- **Settings:** `SettingsViewModel` and the settings screen, which also starts preparing the audio when voice cues are turned on (P5's suggestion).
- **Shared formatting:** `formatTotalDuration()`, for the list, the editor and later P7.
- **Navigation:** this phase's entries in `AppNavHost`.
- **Dependencies:** the Material icons library (core set).
- **Keyboard:** measure how the tablet's keyboard behaves in landscape (T1), and set `windowSoftInputMode` from the result.
- **Tests:** JVM unit tests for the logic and the ViewModels; Compose UI tests on the tablet; the P1 smoke test updated for the real list.

### Out of scope

| What | Phase |
|---|---|
| The runner and completed screens, and what Start shows | P7 |
| Validating a workout before running it (P3 Q2), and what to show for an invalid or missing one | P7 |
| The end-to-end test (create, run, complete) and the acceptance checklist | P8 |
| The storage format, atomic writes, damaged files | P3 (done) |
| Applying the voice setting to the cues | P5 (done) and P7 |
| Import, export or backup of workouts | Not in v1 (specification §2, §10) |
| Undo, and drag-and-drop reordering | Not in v1: the editor has move up and move down buttons |
| Keeping unsaved editor changes after Android closes the app in the background | Not in v1, as for the runner (`EX-11`) |
| The Spanish translation | Later. `NF-9` keeps it possible. |

## 4. Design

### 4.1 Packages and files owned

The base package is `io.github.davamix.workouttimer`.

```
app/src/main/kotlin/io/github/davamix/workouttimer/
├── ui/list/                          (owned by P6)
│   ├── WorkoutListScreen.kt              replaces the placeholder: WorkoutListScreen and the stateless WorkoutListContent
│   └── WorkoutListViewModel.kt           WorkoutListViewModel, WorkoutListState, WorkoutListItem, ListMessage
├── ui/editor/                        (owned by P6)
│   ├── WorkoutForm.kt                    the form model, mapping, edit operations; no Android
│   ├── EditorErrors.kt                   validation errors arranged by field; no Android
│   ├── WorkoutEditorViewModel.kt         WorkoutEditorViewModel, EditorState, EditorMessage
│   ├── WorkoutEditorScreen.kt            replaces the placeholder: screen, top bar, dialogs
│   └── EditorFields.kt                   the defaults section, round card, exercise row and time field
├── ui/settings/                      (owned by P6)
│   ├── SettingsScreen.kt                 replaces the placeholder
│   └── SettingsViewModel.kt
└── ui/common/
    └── TotalDuration.kt                  new, owned by P6: formatTotalDuration()

app/src/main/res/values/
    strings_list.xml, strings_editor.xml, strings_settings.xml      (owned by P6)

app/src/test/kotlin/io/github/davamix/workouttimer/ui/
├── MainDispatcherExtension.kt            JUnit 5 extension: Dispatchers.setMain for ViewModel tests
├── TestRepositories.kt                   a WorkoutRepository and a SettingsRepository whose writes can be made to fail
├── common/TotalDurationTest.kt
├── list/WorkoutListViewModelTest.kt
├── editor/WorkoutFormTest.kt
├── editor/EditorErrorsTest.kt
├── editor/WorkoutEditorViewModelTest.kt
└── settings/SettingsViewModelTest.kt

app/src/androidTest/kotlin/io/github/davamix/workouttimer/ui/
├── UiTestWorkouts.kt                     fixtures: the §4.4 example (androidTest can't see app/src/test)
├── TouchTargets.kt                       helper: asserts every clickable node fully on screen has a touch area of at least 48 × 48 dp
├── list/WorkoutListScreenTest.kt
├── editor/WorkoutEditorScreenTest.kt
└── settings/SettingsScreenTest.kt
```

**Shared files changed,** with this phase's own entries only:

- `gradle/libs.versions.toml`: a new library entry, versioned by the Compose BOM:
  `androidx-compose-material-icons-core = { module = "androidx.compose.material:material-icons-core" }`.
- `app/build.gradle.kts`:
  - `implementation(libs.androidx.compose.material.icons.core)`;
  - `implementation(libs.androidx.lifecycle.viewmodel.compose)`. It's already in the catalog, and already on the classpath through Navigation; declaring it makes `viewModel()` an explicit dependency. P7 may add the same line. Whichever phase merges second keeps a single copy.
- `navigation/AppNavHost.kt`: the list, editor and settings destinations (§4.3.9).
- `app/src/main/AndroidManifest.xml`: `android:windowSoftInputMode="adjustResize"` on `MainActivity` (§4.3.6, measured in T1).
- `app/src/androidTest/.../NavigationSmokeTest.kt` (P1's): updated for the real list. The "Run sample" button is gone, so the runner tests start a seeded workout (§6).
- `strings_list.xml`: `list_run_sample` is removed with the button.

**Not changed:**

- `AppContainer`: the ViewModel factories take the repositories and `prepareAudio()` from it.
- `MainActivity`, the theme, and `Routes.kt`: the routes already carry what P6 needs.
- `PlaceholderScreen` and `strings.xml`: P7's runner and completed placeholders still use them.
- `:core`: P6 adds nothing to it.

### 4.2 Interfaces and data

The names below are a guide. The implementation may adjust them, but not the behaviour in §4.3.

#### Screens, as `AppNavHost` calls them

```kotlin
// ui/list
@Composable
fun WorkoutListScreen(
    onNewWorkout: () -> Unit,
    onEditWorkout: (workoutId: String) -> Unit,
    onStartWorkout: (workoutId: String) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: WorkoutListViewModel = viewModel(factory = WorkoutListViewModel.Factory),
)

// ui/editor
@Composable
fun WorkoutEditorScreen(
    workoutId: String?,                 // null for a new workout
    onClose: () -> Unit,                // called once: after saving, or after leaving without saving
    viewModel: WorkoutEditorViewModel = viewModel(factory = WorkoutEditorViewModel.factory(workoutId)),
)

// ui/settings
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
)
```

Each screen has this entry point and a stateless `…Content(state, callbacks)` composable that draws it. The factories read the `AppContainer` from the application: `(this[APPLICATION_KEY] as WorkoutTimerApp).container`. Tests build the ViewModels directly, with `InMemoryWorkoutRepository` and `InMemorySettingsRepository`, and pass them in.

#### Workout list

```kotlin
class WorkoutListViewModel(
    private val repository: WorkoutRepository,
) : ViewModel() {
    val state: StateFlow<WorkoutListState>

    /** Saves a copy of the workout called [copyName] (§4.3.1). The screen formats the name from a resource. */
    fun duplicate(workoutId: String, copyName: String)
    fun requestDelete(workoutId: String)      // opens the confirmation
    fun confirmDelete()
    fun cancelDelete()
    fun messageShown()

    companion object { val Factory: ViewModelProvider.Factory }
}

data class WorkoutListState(
    val loading: Boolean = true,                         // until the repository's first list arrives
    val workouts: List<WorkoutListItem> = emptyList(),   // sorted (§4.3.1)
    val pendingDelete: WorkoutListItem? = null,          // the confirmation is open for this one
    val message: ListMessage? = null,                    // shown once in a snackbar
)

data class WorkoutListItem(val id: String, val name: String, val totalSeconds: Int)

enum class ListMessage { DUPLICATE_FAILED, DELETE_FAILED }
```

#### Editor form: `WorkoutForm.kt`, pure Kotlin

The editor works on a copy of the workout in which every time field holds the text typed.

```kotlin
/** The editor's working copy of a workout. Time fields hold digits; "" means not set. */
data class WorkoutForm(
    val id: String,
    val name: String,
    val defaultExerciseDuration: String,
    val defaultExerciseRest: String,
    val defaultRoundRest: String,
    val rounds: List<RoundForm>,
)

data class RoundForm(val id: String, val name: String, val rest: String, val exercises: List<ExerciseForm>)

data class ExerciseForm(val id: String, val name: String, val duration: String, val rest: String)

fun Workout.toForm(): WorkoutForm
fun WorkoutForm.toWorkout(): Workout

/** A new workout's form (§4.3.2, Q2). */
fun newWorkoutForm(firstRoundName: String, newId: () -> String = ::newId): WorkoutForm

/** What a time field keeps of what was typed: digits only, at most 4. */
fun timeText(input: String): String
```

Mapping rules:

| | `toForm()` | `toWorkout()` |
|---|---|---|
| Names | as stored | trimmed |
| Own value (duration or rest) | `null` → `""`; a number → its digits | `""` → `null`; digits → the number |
| Default value | its digits | digits → the number; `""` → `EMPTY_DEFAULT` (−1) |
| IDs, order | kept | kept |

`EMPTY_DEFAULT` is outside every allowed range, so `validate()` reports an empty default as `TimeOutOfRange`, and `estimatedTotalSeconds()` counts it as 0. A field can't hold anything else: `timeText()` keeps only digits, and `toForm()` writes only numbers.

**Edit operations,** each a function from form to form. An unknown ID returns the form unchanged.

| Operation | Rule |
|---|---|
| Set the workout name, a round or exercise name | stores the text as typed |
| Set a default, a round rest, an exercise duration or rest | stores `timeText(input)` |
| Add a round | at the end, named by the caller ("Round *n*", where *n* is the new number of rounds), with one exercise to name, own values unset |
| Add an exercise | at the end of its round, with an empty name and own values unset |
| Move a round, or an exercise within its round | one place up or down; nothing at either end |
| Duplicate a round or an exercise | the copy goes right after the original, with the same name and values and new IDs (P2's `Round.duplicate()` and `Exercise.duplicate()`) |
| Remove a round or an exercise | removes it. Removing the last one is allowed; validation then reports it. |

#### Validation errors by field: `EditorErrors.kt`, pure Kotlin

```kotlin
/** validate()'s errors, arranged for the fields that show them. */
data class EditorErrors(
    val workoutNameMissing: Boolean = false,
    val noRounds: Boolean = false,
    val roundNameMissing: Set<String> = emptySet(),      // round IDs
    val roundHasNoExercises: Set<String> = emptySet(),   // round IDs
    val exerciseNameMissing: Set<String> = emptySet(),   // exercise IDs
    val timeOutOfRange: Map<TimeField, IntRange> = emptyMap(),
) {
    val isEmpty: Boolean
}

fun List<ValidationError>.toEditorErrors(): EditorErrors
```

#### Editor ViewModel

```kotlin
class WorkoutEditorViewModel(
    private val repository: WorkoutRepository,
    private val workoutId: String?,                  // null: a new workout
    private val roundName: (number: Int) -> String,  // "Round %1$d" from strings_editor.xml, via the factory
    private val newId: () -> String = ::newId,
) : ViewModel() {
    /** Compose state, read during composition, so text fields update synchronously (§4.3.5). */
    val state: EditorState

    fun edit(change: (WorkoutForm) -> WorkoutForm)   // every form change goes through here
    fun addRound()
    fun save()
    fun requestClose()       // Cancel, the back arrow and system Back
    fun confirmDiscard()
    fun cancelDiscard()
    fun messageShown()

    companion object { fun factory(workoutId: String?): ViewModelProvider.Factory }
}

sealed interface EditorState {
    data object Loading : EditorState
    data object NotFound : EditorState               // no saved workout has this ID
    data class Editing(
        val form: WorkoutForm,
        val isNew: Boolean,
        val totalSeconds: Int,                       // form.toWorkout().estimatedTotalSeconds()
        val errors: EditorErrors,                    // empty until the first Save attempt (§4.3.4)
        val hasChanges: Boolean,                     // §4.3.5
        val confirmingDiscard: Boolean,
        val saving: Boolean,
        val message: EditorMessage?,
    ) : EditorState
    data object Closed : EditorState                 // the screen calls onClose() once
}

enum class EditorMessage { CHECK_FIELDS, SAVE_FAILED }
```

#### Settings ViewModel

```kotlin
class SettingsViewModel(
    private val repository: SettingsRepository,
    private val prepareAudio: suspend () -> Unit,    // AppContainer::prepareAudio
) : ViewModel() {
    val state: StateFlow<SettingsState>

    fun setVoiceCuesEnabled(enabled: Boolean)
    fun messageShown()

    companion object { val Factory: ViewModelProvider.Factory }
}

data class SettingsState(
    val voiceCuesEnabled: Boolean? = null,           // null until the stored value is read
    val saveFailed: Boolean = false,
)
```

#### Total duration: `ui/common/TotalDuration.kt`

```kotlin
/** A total duration: m:ss under an hour, h:mm:ss from an hour. 263 → "4:23", 3663 → "1:01:03". Below 0 → "0:00". */
fun formatTotalDuration(seconds: Int): String
```

The list and the editor show it as "Total %1$s". It isn't the runner's `mm:ss` (`UI-3`), which P7 formats itself.

#### Main texts

The final wording can change in review. Formats use numbered placeholders.

| File | Texts |
|---|---|
| `strings_list.xml` | "Workouts" (exists) · "New workout" (exists) · "Settings" (exists) · "No workouts yet. Create one to get started." · "Total %1$s" · "Start" · "Edit" · "Duplicate" · "Delete" · "%1$s (copy)" · "Delete \"%1$s\"?" · "This can't be undone." · "Cancel" · "Couldn't duplicate the workout." · "Couldn't delete the workout." |
| `strings_editor.xml` | "New workout", "Edit workout" (exist) · "Save" · "Cancel" · "Total %1$s" · "Workout name" · "Defaults" · "Exercise duration" · "Rest between exercises" · "Rest between rounds" · "Round %1$d" · "Round name" · "Round rest" · "Exercise" · "Duration" · "Rest" · "s" · "Default" · "Use default" · "Not used: last exercise" · "Not used: last round" · "Add round" · "Add exercise" · "Move up" · "Move down" · "More actions" · "Duplicate" · "Remove" · "Enter a name" · "From %1$d to %2$d s" · "Add at least one round" · "Add at least one exercise" · "Check the highlighted fields." · "Couldn't save the workout. Your changes are still here." · "Discard changes?" · "Your changes to this workout haven't been saved." · "Keep editing" · "Discard" · "This workout no longer exists." |
| `strings_settings.xml` | "Settings" (exists) · "Voice cues" · "Speak exercise names and countdowns. The round-end sound plays either way." · "Couldn't save the setting." |

### 4.3 Behaviour

#### 4.3.1 Workout list

```
+------------------------------------------------------------------------------+
| Workouts                                            [ + New workout ]  [ ⚙ ] |
+------------------------------------------------------------------------------+
|  Example                                                                     |
|  Total 4:23                  [ ▶ Start ]  [ Edit ]  [ Duplicate ]  [ Delete ] |
|------------------------------------------------------------------------------|
|  Legs                                                                        |
|  Total 32:10                 [ ▶ Start ]  [ Edit ]  [ Duplicate ]  [ Delete ] |
+------------------------------------------------------------------------------+
```

- **Loading:** a progress indicator until the repository's first list arrives. P3 loads the files on the first collection. The empty state doesn't flash before the list.
- **Empty:** a short message, and the New workout button in the top bar.
- **Order:** by name, ignoring case (`String.CASE_INSENSITIVE_ORDER`), then by ID, so equal names keep a stable order (Q4). P3 leaves the order unspecified.
- **A row:** the name on one line, cut with an ellipsis if too long; "Total *m:ss*", from `estimatedTotalSeconds()`; then the buttons Start, Edit, Duplicate and Delete.
- **Start** calls `onStartWorkout(id)`, which opens the runner (P7). It is always enabled. Only the editor writes workouts, and it validates them first; P7 still validates before running (P3 Q2).
- **Edit** calls `onEditWorkout(id)`.
- **Duplicate** saves `workout.duplicate(copyName)`, where `copyName` is "*name* (copy)". P2's `duplicate` gives new IDs to the workout and to every round and exercise, and keeps every value. The list stays open, and the copy appears in its sorted place (Q6).
- **Delete** opens a dialog with the workout's name: Cancel, or dismissing it, keeps the workout; Delete deletes it (`WM-9`).
- **Failures:** P3's `save` and `delete` throw `IOException` and keep the old data. The list then shows a snackbar ("Couldn't duplicate the workout.", "Couldn't delete the workout.") and stays as it was.
- **Top bar:** the title, New workout, and a settings icon button.

#### 4.3.2 Editor: the form and its values

```
+------------------------------------------------------------------------------+
| ←  Edit workout                         Total 4:23      [ Cancel ]  [ Save ] |
+------------------------------------------------------------------------------+
|  Workout name                                                                |
|  [ Example                       ]                                           |
|  Defaults                                                                    |
|  Exercise duration       Rest between exercises       Rest between rounds    |
|  [ 40 s              ]   [ 15 s                   ]   [ 60 s              ]  |
|                                                                              |
|  +- Round 1 ---------------------------------------------------- ↑  ↓  ⋮ -+  |
|  |  Round name                                                            |  |
|  |  [ Warm-up                ]   Round rest [ 30 s × ]                    |  |
|  |      Exercise                 Duration       Rest                      |  |
|  |  1   [ Jumping jacks     ]    [ 30 s × ]     [ 15 s ]          ↑ ↓ ⋮   |  |
|  |                                              Default                   |  |
|  |  2   [ Arm circles       ]    [ 10 s × ]     [  5 s × ]        ↑ ↓ ⋮   |  |
|  |  3   [ High knees        ]    [ 40 s ]       [ 15 s ]          ↑ ↓ ⋮   |  |
|  |                               Default        Not used: last exercise   |  |
|  |  [ + Add exercise ]                                                    |  |
|  +------------------------------------------------------------------------+  |
|  +- Round 2 ---------------------------------------------------- ↑  ↓  ⋮ -+  |
|  |  [ Core                  ]   Round rest [ 60 s ]  Not used: last round |  |
|  |  ...                                                                   |  |
|  +------------------------------------------------------------------------+  |
|  [ + Add round ]                                                             |
+------------------------------------------------------------------------------+
```

`×` is the "Use default" button of an own value. Linked values show the default as a dimmed placeholder.

The form scrolls: a `LazyColumn` with the workout section, one card per round (keyed by round ID), then Add round. The top bar, with the total, Cancel and Save, stays in place.

- **Labels:** the workout name, the round name and the three defaults have floating labels. Own-value fields have none: with a label, Material 3's value-based `OutlinedTextField` hides the placeholder until the field has focus, so a linked value wouldn't show its default. (The `labelPosition` option that avoids this exists only on the `TextFieldState` overload.) So a round's exercises are a table under one header row (Exercise · Duration · Rest), and the round rest has an inline label. The table is also compact above the keyboard (§4.3.6). Seconds fields are 200 dp wide.

- **Opening:**
  - `workoutId == null`: a new form (Q2). The name is empty; the defaults are 40 s, 15 s and 60 s (the §4.4 example's); there is one round, "Round 1", with one exercise to name.
  - Otherwise the workout is loaded with `repository.get(id)`: `Loading`, then `Editing`. If there is none, `NotFound` shows "This workout no longer exists." and a Back button that calls `onClose()`.
- **Names:** free text, trimmed when saved. A blank name counts as missing (P2).
- **Time fields:** whole seconds (Q3), with a number keyboard and the suffix "s". A field keeps only digits, at most 4 (`timeText`), so it can hold 9999; validation rejects anything over 5999.
- **Defaults** are required. An empty default shows the range error once errors are shown (§4.3.4).
- **Own values** (§4.2):
  - An empty own field is linked to its default. It shows the default's value, the number alone, as a placeholder dimmed to 60 %, with "Default" as its supporting text. The unit "s" is the field's suffix.
  - Typing a number sets an own value, even one equal to the default. A trailing "Use default" button, or clearing the text, links it to the default again.
  - Changing a default changes every linked placeholder at once; own values keep theirs.
- **Unused rests** (§4.2): the rest of the last exercise of each round, and the round rest of the last round.
  - They are dimmed and labelled "Not used: last exercise" or "Not used: last round". Their values are kept.
  - They stay editable. An own value is validated even where it's unused (P2), so a read-only field could hold an error the user can't fix.
  - The marking follows the position live: adding, moving or removing an exercise or a round updates it.
- **Total:** "Total *m:ss*" in the top bar, from `form.toWorkout().estimatedTotalSeconds()`. It updates with every edit, and works on an invalid workout: rounds without exercises are ignored, and negative values count as 0 (P2).

#### 4.3.3 Editor: rounds and exercises (`WM-5`)

- **Rounds**, from the round card's header:
  - **Move up** and **Move down**, disabled at the ends;
  - **More actions** (⋮): Duplicate and Remove.
- **Exercises**, from the end of each row:
  - **Move up** and **Move down** within the round, disabled at the round's ends (Q8);
  - **More actions** (⋮): Duplicate and Remove.
- **Add exercise** at the end of each round card; **Add round** after the last round.
- **Rename:** the name fields.
- The operations follow the rules in §4.2. Removing asks for no confirmation (Q7): nothing is saved until Save, and leaving without saving asks first (`WM-10`).
- After adding or duplicating, the new item scrolls into view. After Add exercise, its name field takes the focus.
- **To repeat a round,** duplicate it (`WM-5`). The copy keeps the name, as P2's `Round.duplicate()` does.

#### 4.3.4 Editor: validation and saving (`WM-7`)

- **Save:** maps the form with `toWorkout()` and calls `validate()`.
  - With no errors, it calls `repository.save(workout)`, and the editor closes, back to the list (Q5).
  - With errors, nothing is saved. The errors appear at their fields, a snackbar says "Check the highlighted fields.", and the form scrolls to the first error.
- **When errors show** (Q1): from the first Save attempt on. After that they update with every edit, so a fixed field clears at once. Before the first attempt, no errors are shown, so a new workout doesn't open covered in red.
- **Where each error shows:**

  | `ValidationError` | Where | Message |
  |---|---|---|
  | `WorkoutNameMissing` | the workout name field | "Enter a name" |
  | `NoRounds` | above Add round | "Add at least one round" |
  | `RoundNameMissing(roundId)` | that round's name field | "Enter a name" |
  | `RoundHasNoExercises(roundId)` | inside that round's card, above Add exercise | "Add at least one exercise" |
  | `ExerciseNameMissing(exerciseId)` | that exercise's name field | "Enter a name" |
  | `TimeOutOfRange(field, min, max)` | the field named by `field` | "From *min* to *max* s", for example "From 1 to 5999 s" |

  P2 reports each field once, in workout order, and identifies rounds and exercises by ID. Defaults are checked where they're defined; linked values aren't reported again.
- **Save fails:** P3's `save` throws `IOException` and keeps the old file. The editor stays open with the changes, and shows "Couldn't save the workout. Your changes are still here." (P3 Q3). While a save runs, Save does nothing.
- `save` also throws `IllegalArgumentException` for an ID that can't be a file name. That can't happen here: every ID comes from `newId()`, a UUID.

#### 4.3.5 Editor: leaving it (`WM-10`), and its state

- **Cancel**, the **back arrow** and the **system Back** button (`BackHandler`) all call `requestClose()`:
  - with no changes, the editor closes;
  - with changes, a dialog asks "Discard changes?": **Keep editing**, or dismissing the dialog, stays; **Discard** closes without saving.
- **"Changes"** means `form.toWorkout()` differs from the workout the editor opened with, mapped the same way. Changing a value and putting it back counts as no change, and "040" is the same as "40". A new workout that hasn't been touched has no changes.
- After a successful save, the editor closes without asking.
- `Closed` makes the screen call `onClose()` once.
- **Configuration changes:** the form lives in the ViewModel, so flipping the tablet keeps unsaved edits. If Android closes the app in the background, they're lost. If it then rebuilds the navigation stack, the editor reopens from the saved workout, or with a new form (as for the runner, `EX-11`).
- **Text field pitfall:** a Compose text field must get its new value back in the same frame. Asynchronous round trips, such as a `StateFlow` collected with `collectAsState`, can drop characters or move the cursor (Android's "State in Compose: TextField" guidance). So the ViewModel holds the editor state in Compose snapshot state (`mutableStateOf`) and changes it synchronously in `edit()`. A JVM unit test can read it directly. The list and settings have no text fields, so they use `StateFlow` and `collectAsStateWithLifecycle`.

#### 4.3.6 Keyboard (measured in T1)

The soft keyboard takes a large part of a 600 dp-high landscape screen, so the editor depends on how it behaves on the tablet.

**Decision, from T1's measurements:** `MainActivity` gets `android:windowSoftInputMode="adjustResize"`. The window then shrinks above the keyboard, Compose scrolls the focused field into view, and the top bar stays on screen.

**Measured in T1** on 2026-11-04. A throwaway probe showed a 64 dp top bar over a `LazyColumn` of `OutlinedTextField`s, and tapped the lowest fully visible field (field 4). Each mode ran twice, with the same numbers each time.

- **Input method:** Microsoft SwiftKey (`com.touchtype.swiftkey`), the tablet's default.
- **Full screen in landscape:** no (`InputMethodManager.isFullscreenMode` is `false`). The app stays visible above the keyboard.
- **Keyboard height:** 744 px, 372 dp, with a text keyboard. The visible window shrinks from 48–1133 px to 48–456 px, which leaves **204 dp** between the status bar and the keyboard. With a 64 dp top bar, that's 140 dp for the form: about one exercise row. The frame changes 350–400 ms after the tap.
- **Each soft-input mode:**

  | Mode | Window | Top bar | Focused field |
  |---|---|---|---|
  | unspecified (today's manifest) | panned up by 504 px; the content keeps its full height (1085 px) | **off screen** (at −456 px) | visible, right above the keyboard |
  | `adjustPan` | the same as unspecified | **off screen** | visible |
  | `adjustResize` | the content shrinks to 408 px | **visible** (48–176 px) | visible, right above the keyboard; the list scrolled by itself to keep it there |

  Unspecified behaves as `adjustPan`, as expected: a Compose view isn't a scroll container, so Android pans.

**What follows for the layout:**

- the top bar stays a single standard row (64 dp at most), so the form keeps the 140 dp it has above the keyboard;
- an exercise row should stay compact: its fields side by side, not stacked, so the field being typed in and its row fit in that space.

Keyboard actions: name and time fields use `ImeAction.Next`; the last field on the screen uses `ImeAction.Done`. Name fields capitalise sentences.

#### 4.3.7 Settings (`TTS-9`)

- One row: the switch "Voice cues", with "Speak exercise names and countdowns. The round-end sound plays either way."
- The switch shows the stored value. Until it has been read (`voiceCuesEnabled == null`), the switch isn't shown.
- Changing the switch calls `setVoiceCuesEnabled`. If that throws `IOException`, a snackbar says "Couldn't save the setting.", and the switch goes back to the stored value.
- **Turning voice cues on** also calls `prepareAudio()` (`AppContainer.prepareAudio()`, P5) in `viewModelScope`, after the write. `MainActivity` only prepares the speaker if voice cues are on when it's created. Without this, the first workout after turning them on would wait the whole 9–10 s for the TTS engine (P5).
  - The screen doesn't wait for it, and leaving the screen may cancel the call. The preparation still carries on: `TtsSpeaker.prepare()` starts connecting the engine before it suspends, and only the wait is cancelled.
- **Turning them off** does nothing else. The runner reads the setting when a workout starts (P5).
- The back arrow and the system Back button call `onBack()`.

#### 4.3.8 Touch targets and text (`NF-6`, `NF-9`)

- **Touch targets:**
  - every touch target is at least 48 × 48 dp, which Material 3 components ensure through `minimumInteractiveComponentSize`;
  - the main actions are at least 56 dp high: Start, Save, New workout, and the confirm button of each dialog.
- **Type and theme:** text uses the app's type scale, Material's enlarged by 25 % (P1). The theme is dark only (P1).
- **Text:** every text and content description is in `strings_list.xml`, `strings_editor.xml` or `strings_settings.xml`. Kotlin code passes no string literal to `Text`, a label, a placeholder or a `contentDescription`. Only totals ("4:23") and numbers are formatted in code.
- **Icons,** from `material-icons-core`: `Add`, `ArrowBack`, `Delete`, `Edit`, `KeyboardArrowUp`, `KeyboardArrowDown`, `MoreVert`, `PlayArrow`, `Settings`, and `Clear` for "Use default". The core set has no copy icon, so Duplicate is a text button on the list and a menu item in the editor.

#### 4.3.9 Navigation

| From | Action | To |
|---|---|---|
| List | New workout | `WorkoutEditorRoute()` |
| List | Edit | `WorkoutEditorRoute(id)` |
| List | Start | `RunnerRoute(id)` (P7's screen) |
| List | Settings | `SettingsRoute` |
| Editor | `onClose()` | back to the list (`popBackStack()`) |
| Settings | `onBack()` | back to the list |

- Navigating callbacks ignore taps unless their destination is resumed, so a double tap doesn't open a screen twice. The callbacks without an argument use `dropUnlessResumed`, as now. The ones that take an ID check `lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)` before navigating.
- The editor's callback is now `onClose` instead of `onBack`, because it's also called after saving.

### 4.4 Dependencies

| Library | Version | Where | Why |
|---|---|---|---|
| `androidx.compose.material:material-icons-core` | 1.7.8, from Compose BOM 2026.09.00 | `:app` main, **new** catalog entry | The icons in §4.3.8. Material 3 1.4 no longer brings them. |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.11.0 | `:app` main (catalog entry exists; now declared) | `viewModel()` with a factory |
| `androidx.lifecycle:lifecycle-runtime-compose` | 2.11.0 | `:app` main (already there) | `collectAsStateWithLifecycle`, `dropUnlessResumed` |
| Material 3 1.4.0, Navigation Compose 2.10.2, Activity Compose 1.13.0 | as in the catalog | `:app` main (already there) | the screens, `BackHandler`, the routes |
| JUnit Jupiter, `kotlinx-coroutines-test` | as in the catalog | `:app` test (already there) | ViewModel tests |
| Compose UI test (JUnit 4), AndroidX Test | as in the catalog | `:app` androidTest (already there) | screen tests |

**Why `material-icons-core`:** it's an AndroidX library from Google Maven, as plan §3 asks, with the same Apache 2.0 licence as the rest of AndroidX. So it raises no third-party asset licence question, which plan `D-10` avoids for the chime. Google no longer adds icons to it, and recommends Material Symbols drawables for new icons. But every icon P6 needs is already in it, and drawables copied into the repo would need their licence notice kept. It must add no permission or component to the merged manifest (§5).

From earlier phases it uses:

- **P2** (`:core`):
  - `Workout`, `Round`, `Exercise`;
  - `validate()`, `ValidationError`, `TimeField`, `Limits`;
  - `duplicate()`, `newId()`;
  - `estimatedTotalSeconds()`;
  - `WorkoutRepository`, and `InMemoryWorkoutRepository` in tests.
- **P3:** `AppContainer.workoutRepository` and `settingsRepository`; `InMemorySettingsRepository` in tests; `SampleWorkouts.kt` (the §4.4 example and builders) in `:app` unit tests.
- **P5:** `AppContainer.prepareAudio()`.

No fakes are needed: P2, P3 and P5 are merged.

### 4.5 From earlier phases

What earlier phases settled about the code this phase uses, from their SPECs and handovers:

- **P2, validation:**
  - `validate()` lists errors in a stable order: "the workout's name, its three defaults, `NoRounds`, then for each round its name, its rest and `RoundHasNoExercises`, followed by each of its exercises (name, duration, rest)". "Defaults are checked once, where they're defined; linked values aren't reported again."
  - "A name that is empty or only whitespace counts as missing (`isBlank()`)."
  - Own values are validated even where they're unused.
- **P2, other functions:**
  - `Round.isRestUsed(exercise)` and `Workout.isRestUsed(round)` match by ID, and throw `IllegalArgumentException` if the item isn't there. The form can also compute "unused" from the position.
  - `estimatedTotalSeconds()` works on invalid workouts: rounds without exercises are ignored, negative values count as 0, a workout without exercises gives 0. For a valid workout it equals the timeline's total.
  - `Workout.duplicate(name)` gives new IDs to everything. `Round.duplicate()` and `Exercise.duplicate()` keep the name.
- **P3, the workout repository:**
  - "the order isn't specified (P6 sorts the list)";
  - it loads the files once, on first use, on the I/O dispatcher;
  - `save` replaces a workout with the same ID;
  - `save` and `delete` throw `IOException` on failure, and the files and the list stay as they were;
  - `save` throws `IllegalArgumentException` for an ID that doesn't match `[A-Za-z0-9_-]{1,64}`. `newId()` UUIDs always match.
  - P3 Q2: "Only the editor writes files, and it validates first (P6)."
  - P3 Q3: "P6 decides the message. Suggested for P6: a short error, and the editor stays open with the changes kept."
- **P3, the settings repository:** `setVoiceCuesEnabled` lets an I/O error reach the caller, and `settings` emits only on change.
- **P3, pending wiring:** "P5, P6 and P7 take `workoutRepository` and `settingsRepository` from `AppContainer`." P6 does so from the start.
- **P5, pending wiring** (a suggestion for P6): "when the voice switch is turned on, call `container.prepareAudio()` (in a scope that outlives the screen, or `lifecycleScope` of the activity). `MainActivity` only prepares the speaker if voice cues are on when it's created." §4.3.7 does it in `viewModelScope`, and explains why that's enough.
- **P1:** the Compose test rule's v1 API is deprecated in Compose BOM 2026.09.00. Use `androidx.compose.ui.test.junit4.v2.createComposeRule` and `createAndroidComposeRule`, as `NavigationSmokeTest` does. After an action that starts a coroutine, wait for its result (`waitUntil { … }`, or `waitForIdle()`) before asserting.
- **P1:** `connectedDebugAndroidTest` uninstalls the app when it ends. Reinstall with `installDebug` before checking anything by hand.
- **P1 and P3:** the first launch after installing takes 30–72 s on the tablet, while Android 8 compiles the debug APK. Later launches are fast.
- **P2 and P3, fixtures:** `:core`'s `ExampleWorkouts.kt` isn't visible to `:app`, and `app/src/test`'s `SampleWorkouts.kt` isn't visible to `app/src/androidTest`. Instrumented tests get their own small `UiTestWorkouts.kt` rather than a shared-source build change.

CLAUDE.md already covers instrumented test names (camelCase), the `v2` Compose test rules, `checkNotNull` in JUnit 4, and running `ktlintFormat` on its own.

## 5. Security and privacy

Plan §3 applies. For this phase:

- **No new permission or component.** `material-icons-core` and `lifecycle-viewmodel-compose` must add none. `./gradlew build` already fails on `INTERNET`. Also compare the merged debug and release manifests with `main`'s (`app/build/intermediates/merged_manifest/…/AndroidManifest.xml`).
- **Private storage only.** Workouts are saved only through P3's repository, in the app's private storage. P6 writes no files of its own.
- **Safe file names.** Every ID comes from `newId()`, so P3's file-name rule always holds.
- **Validated before saving.** The editor never passes the repository a workout that fails `validate()`.
- **Nothing leaves the app.** No sharing, clipboard export or intents to other apps.
- **No logging** of workout contents. P6 adds no logs.
- **Tests clean up.** The smoke test seeds one workout with a fixed ID in the app's real storage, and deletes it afterwards. It never touches other workouts.

## 6. Testing

Unit tests run in `./gradlew build` and in CI. The ViewModel tests use `MainDispatcherExtension`, which sets `Dispatchers.Main` to a test dispatcher. They also use the in-memory repositories, wrapped by `TestRepositories.kt` when a write must fail.

| Test | Type | Covers |
|---|---|---|
| `TotalDurationTest`: 0 → "0:00"; 59 → "0:59"; 60 → "1:00"; 263 → "4:23"; 3599 → "59:59"; 3600 → "1:00:00"; 3663 → "1:01:03"; a negative value → "0:00" | unit | WM-6 |
| `WorkoutFormTest`:<br>• `toForm()` then `toWorkout()` gives back the §4.4 example<br>• own `""` ↔ `null`; an own value equal to the default stays own<br>• unused values are kept<br>• names are trimmed by `toWorkout()`<br>• an empty default makes `validate()` report `TimeOutOfRange`, and the total counts it as 0<br>• `timeText` drops letters, signs, spaces and separators, and keeps at most 4 digits<br>• every edit operation, at both ends of the list, and with an unknown ID<br>• a duplicated round or exercise goes right after the original, with new IDs and the same name and values<br>• removing the last round or exercise gives `NoRounds` or `RoundHasNoExercises`<br>• the new-workout form (Q2) | unit | WM-1, WM-2, WM-5, §4.2 |
| `EditorErrorsTest`: each `ValidationError` kind lands on its field; the §4.4 example has none | unit | WM-7 |
| `WorkoutListViewModelTest`:<br>• loading, then the list sorted by name, ignoring case, then by ID<br>• the §4.4 example's total is 263 s<br>• duplicate saves a copy with the given name and new IDs<br>• delete: request, then cancel keeps it; request, then confirm deletes it<br>• a failing save or delete gives the message, and the list stays as it was | unit | WM-3, WM-4, WM-6, WM-9 |
| `WorkoutEditorViewModelTest`:<br>• a new form (Q2); an existing workout loads; an unknown ID gives `NotFound`<br>• an edit updates the total<br>• `hasChanges` after an edit, and not after putting the value back<br>• Save on a valid form saves it and gives `Closed`<br>• Save on an invalid form saves nothing, shows the errors, and they then update with each edit<br>• a failing save gives `SAVE_FAILED`, and the form is kept<br>• `requestClose` with no changes closes; with changes it asks; Keep editing stays; Discard closes and saves nothing | unit | WM-4, WM-6, WM-7, WM-10 |
| `SettingsViewModelTest`:<br>• reads the stored value<br>• turning off, then on, stores each value<br>• `prepareAudio` is called when turning on, not when turning off<br>• a failing write gives the message, and the stored value stays | unit | TTS-9 |
| `WorkoutListScreenTest`:<br>• rows sorted, each with its total ("Total 4:23" for the example)<br>• the empty state<br>• Start, Edit, New workout and Settings call back with the right ID<br>• Duplicate shows "Example (copy)"<br>• Delete: the dialog names the workout; Cancel keeps it; Delete removes it<br>• touch targets of at least 48 dp | instrumented (Compose) | WM-3, WM-4, WM-6, WM-9, NF-6 |
| `WorkoutEditorScreenTest`:<br>• **create:** type the §4.4 example into a new workout, with own values and linked ones, and save. The repository holds a workout equal to the example apart from its IDs, and the editor showed "Total 4:23" before saving.<br>• **edit:** open the example and change the default duration to 45. High knees and Crunches show 45 as a placeholder, Jumping jacks keeps 30, and the total becomes 4:33.<br>• **own values:** clearing an own value shows the default again<br>• **unused rests:** High knees' rest and the Core round's rest are marked "Not used"; moving High knees up moves the marking<br>• **structure:** add, duplicate, move and remove rounds and exercises<br>• **validation:** Save with an empty exercise name, a duration of 0 and a rest of 6000 shows the three messages and saves nothing; fixing them clears the messages<br>• **leaving:** Back with changes asks; Keep editing stays; Discard leaves the repository unchanged. Back without changes leaves at once.<br>• touch targets of at least 48 dp | instrumented (Compose) | WM-1, WM-2, WM-4–WM-7, WM-10, §4.2, NF-6 |
| `SettingsScreenTest`: the switch shows the stored value; changing it stores it; touch targets | instrumented (Compose) | TTS-9, NF-6 |
| `NavigationSmokeTest`, updated:<br>• starts on the list<br>• New workout opens the editor; Cancel returns to the list<br>• Settings, then system Back, returns to the list<br>• Start on a seeded workout leads to the runner placeholder, then the completed screen, then the list<br>• Back on the completed screen skips the runner<br>• the activity is landscape only | instrumented | navigation (§4.3.9), NF-8 |

**Seeding the smoke test:** it saves one workout, with the fixed ID `smoke-test-workout`, through `container.workoutRepository` in `@Before`, and deletes it in `@After`. The list collects the repository's flow, so the workout appears even though the activity is already open. The test must not assume the list is otherwise empty: run with `am instrument`, the app's data is kept.

**Finding things in tests:** rows and cards carry test tags built from their ID, for example `workout-<id>`, `round-<id>` and `exercise-<id>`. Tests find a button within a row with `hasAnyAncestor(hasTestTag(…))`. Texts are matched through `getString(R.string.…)`, never as literals.

**Comparing "apart from IDs":** a test helper maps a workout to the same structure with every ID blanked.

### Manual checks on the tablet

Run them with `./gradlew installDebug`, after the instrumented tests, which uninstall the app. Start from an empty list: clear the app's data with `adb shell pm clear io.github.davamix.workouttimer.debug` if needed.

| # | Step | Expected |
|---|---|---|
| 1 | Open the app with no workouts | The empty message and New workout. Nothing flashes before it. |
| 2 | Create the §4.4 example by hand, leaving the linked values empty, then Save | The editor shows "Total 4:23" before saving. The list shows "Example", "Total 4:23". |
| 3 | Edit it: set the default exercise duration to 45 | High knees and Crunches show 45; Jumping jacks keeps 30; the total shows 4:33 |
| 4 | Press the system Back button | "Discard changes?" Keep editing stays; Back again, then Discard, returns to the list with 4:23 |
| 5 | Edit it: check the unused rests, then move High knees up one place | High knees' rest and Core's round rest are marked "Not used". After the move, Arm circles' rest is marked instead, and High knees' rest shows 15 again. Cancel, then Discard. |
| 6 | Edit it: clear an exercise name, set a duration to 0 and a rest to 6000, then Save | Three messages at their fields, and "Check the highlighted fields." Nothing is saved. Fix them, and the messages clear. Discard. |
| 7 | Edit it: tap the bottom field on screen, in both landscape orientations | The keyboard doesn't cover the field, and the top bar stays visible (§4.3.6). Turning the tablet over keeps the unsaved text. |
| 8 | Duplicate "Example", then delete the copy: Cancel first, then Delete | "Example (copy)" appears with 4:23. Cancel keeps it; Delete removes it. |
| 9 | `adb shell am force-stop io.github.davamix.workouttimer.debug`, then reopen | "Example" is still listed (`WM-8`) |
| 10 | Settings: turn voice cues off, force-stop the app, reopen Settings | Still off. Turn them back on. |
| 11 | Build a large workout: duplicate a round until there are 10, each with 8 exercises | Scrolling and typing stay smooth; the total keeps up |
| 12 | Use every screen at arm's length | Every button is easy to hit, and the text is easy to read (`NF-6`) |
| 13 | Read the app's logcat during steps 1–12 | No crash. No `WorkoutStorage` warning. |

## 7. Tasks

| ID | Task |
|---|---|
| T1 | **Keyboard probe** (tablet skill, "Measuring device behaviour"). A throwaway `probe/KeyboardProbe.kt` shows a top bar over a scrolling Compose list of about 30 `OutlinedTextField`s, as the editor will. For each soft-input mode, unspecified (today's manifest), `adjustResize` and `adjustPan`, set with `window.setSoftInputMode`, it taps a field in the bottom quarter of the screen. It logs: the default input method (`Settings.Secure.DEFAULT_INPUT_METHOD`); whether it's full screen (`InputMethodManager.isFullscreenMode`); the visible window frame (`getWindowVisibleDisplayFrame`); the field's bounds once the keyboard is up; and whether the top bar is still on screen. Take a screenshot of each case, and run it twice. Record the results in §4.3.6 and choose the manifest's `windowSoftInputMode`. If the results contradict §4.3.6, stop and agree a SPEC change first. Delete the probe. |
| T2 | **Foundations:** the dependencies (§4.4); `TotalDuration.kt`; `WorkoutForm.kt` and `EditorErrors.kt`; `MainDispatcherExtension` and `TestRepositories.kt`. Tests: `TotalDurationTest`, `WorkoutFormTest`, `EditorErrorsTest`. |
| T3 | **Workout list:** `WorkoutListViewModel`, the screen, `strings_list.xml`, and the list's entries in `AppNavHost` (`onEditWorkout`, the double-tap guards). Update `NavigationSmokeTest`. Add `UiTestWorkouts.kt` and `TouchTargets.kt`. Tests: `WorkoutListViewModelTest`, `WorkoutListScreenTest`. |
| T4 | **Editor ViewModel:** opening, edits, the total, changes, validation display, saving and its failure, leaving with and without changes. Test: `WorkoutEditorViewModelTest`. |
| T5 | **Editor screen, part 1:** the top bar, name, defaults, round cards and exercise rows with their time fields; placeholders for linked values, "Use default", unused rests marked; validation messages; Save, with the snackbar and scrolling to the first error. Add `strings_editor.xml`, the editor's entry in `AppNavHost` (`onClose`), and the manifest's soft-input mode from T1. Install it and check it on the tablet with screenshots. |
| T6 | **Editor screen, part 2:** move, duplicate and remove for rounds and exercises, with Add exercise and Add round; scrolling to new items and focusing a new exercise's name; the discard dialog, Cancel, the back arrow and `BackHandler`; the not-found state. Test: `WorkoutEditorScreenTest`. |
| T7 | **Settings:** `SettingsViewModel` (with `prepareAudio` on turning voice cues on), the screen, `strings_settings.xml`, the settings entry in `AppNavHost`. Tests: `SettingsViewModelTest`, `SettingsScreenTest`. |
| T8 | **Finish:** run `connectedDebugAndroidTest`; compare the merged manifests with `main`'s; check that P6's Kotlin code holds no user-facing text; run the manual checks (§6) with the user. Then KDoc on the public API for P7 and P8, `./gradlew build`, handover, pull request. |

## 8. Done criteria

The checks that must all pass before the phase is merged.

- [x] All tasks done
- [x] Unit tests and lint pass locally and in CI
- [x] Instrumented tests pass on the tablet (`connectedDebugAndroidTest`), including the updated `NavigationSmokeTest`
- [x] Compose UI tests cover creating, editing and validating a workout (plan §5.2)
- [x] Manual checks on the tablet pass
- [x] The merged manifests gain no permission and no component
- [x] P6's Kotlin code holds no user-facing text: it's all in `strings_list.xml`, `strings_editor.xml` and `strings_settings.xml`
- [x] `:core` is unchanged
- [x] HANDOVER.md is up to date, and the plan's status table is updated

## 9. Open questions

None at the moment. New questions are numbered from Q10.

### Answered

| # | Question | Decision |
|---|---|---|
| Q1 | When does the editor show validation messages? | **From the first Save attempt on,** then live with every edit. Before that, none, so a new workout doesn't open covered in red. |
| Q2 | What does a new workout start with? | **An empty name; defaults of 40 s, 15 s and 60 s** (the §4.4 example's); **one round, "Round 1", with one exercise to name.** Rounds added later are named "Round *n*" and come with one exercise to name. |
| Q3 | How are durations and rests typed? | **Whole seconds,** with a number keyboard: "90" for a minute and a half. Not mm:ss. |
| Q4 | In what order does the list show workouts? | **By name, A to Z, ignoring case.** The model has no dates, so "last edited first" would need a storage-format change. |
| Q5 | What happens after Save? | **The editor closes,** and the list shows the workout. |
| Q6 | What does Duplicate on the list do? | **It saves a copy called "*name* (copy)" at once,** which appears in the list. The editor doesn't open. |
| Q7 | Does removing a round or an exercise ask for confirmation? | **No.** Nothing is saved until Save, and leaving without saving asks before discarding (`WM-10`). |
| Q8 | Where can an exercise be moved? | **Up and down within its round only.** To move one to another round, add it there and remove it here. |
| Q9 | Found in T5: with a floating label, Material 3's value-based text field hides the placeholder until the field has focus, so a linked value wouldn't show its default. And "Not used: last exercise of the round" wrapped to three lines in a seconds field. | **Own-value fields have no floating label.** A round's exercises are a table under one header row (Exercise · Duration · Rest), and the round rest has an inline label. The placeholder is the default's number alone, dimmed. The "Not used" texts are "Not used: last exercise" and "Not used: last round", and seconds fields are 200 dp wide (§4.3.2). The user checked the result on the tablet. |

## 10. Change log

| Version | Date | Changes |
|---|---|---|
| 0.1 | 2026-11-04 | First draft |
| 0.2 | 2026-11-04 | Q1–Q8 answered, confirming the draft assumptions. No specification change: they settle UI details. Status `Ready`. |
| 0.3 | 2026-11-04 | T1 done: the keyboard measurements are in §4.3.6. They confirm the draft's assumptions, so `MainActivity` gets `adjustResize`. With the keyboard up, 204 dp of the screen stay visible, so the top bar and the exercise rows stay compact. |
| 0.4 | 2026-11-05 | Matches the code after T5 (Q9): own-value fields have no floating label, so the exercises are a table under column headers; the editor sketch in §4.3.2 is redrawn; the "Not used" texts are shorter. The touch-target helper checks the nodes fully on screen (§4.1). |
