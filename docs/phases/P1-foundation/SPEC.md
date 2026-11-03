# P1 — Project foundation: Specification

| | |
|---|---|
| **Status** | Ready, v0.1 (implementation started at the user's request) |
| **Phase** | P1, see [Implementation plan §5](../../IMPLEMENTATION_PLAN.md#5-phases) |
| **Depends on** | — |
| **Branch** | `phase/p1-foundation` |
| **Handover** | [HANDOVER.md](HANDOVER.md) |

This document holds everything needed to implement the phase. Working on it shouldn't require reading the full specification or plan.

## 1. Goal

An empty but complete app skeleton that builds, tests and installs on the tablet, with CI. Every later phase only adds code to the structure created here:

- the two modules (`:core` and `:app`);
- the theme;
- placeholder screens wired into navigation;
- the checks.

## 2. Requirements covered

Quoted from [SPECIFICATION.md](../../SPECIFICATION.md) v0.5.

| ID | Requirement (quoted) | What this phase does for it |
|---|---|---|
| NF-4 | "**Offline and private:** no network permission, no accounts, no analytics." | No `INTERNET` permission. A build check fails if the merged manifest contains it. Auto Backup is disabled (plan `D-11`). |
| NF-5 | "**Compatibility:** runs on Android 8.0 (API 26) on the target device." | `minSdk` 26, and the app is installed and run on the tablet |
| NF-6 | "**Readability:** the time and exercise name are readable from about 3 m away. Touch targets are large." | Foundation only: a dark, high-contrast theme and the phase colours. P6 and P7 do the real work. |
| NF-7 | "**Distribution:** installed as an APK from the development machine (`adb` / Android Studio)." | `gradlew installDebug` installs on the tablet. The project opens in the installed Android Studio. |
| NF-8 | "**Landscape only:** the app follows the sensor between the two landscape orientations and never shows portrait." | The activity uses `sensorLandscape` |
| NF-9 | "**Language:** the UI is in English. All user-facing text lives in resource files, so a Spanish translation can be added later without code changes." | All text is in resources, with one `strings_<feature>.xml` file per feature (plan `D-13`) |

## 3. Scope

### In scope

- The Gradle project: wrapper, version catalog, the `:core` and `:app` modules.
- The app skeleton: application class, `AppContainer`, single activity, theme, launcher icon.
- Navigation with a placeholder for each of the five screens.
- Checks: Android Lint, ktlint, and the no-INTERNET check.
- An instrumented smoke test.
- GitHub Actions CI.
- `CLAUDE.md`, and the `tablet` skill (`SK-4`).

### Out of scope

| What | Phase |
|---|---|
| Domain model, timeline, `WorkoutRepository` interface | P2 |
| Storage and settings | P3 |
| Engine | P4 |
| Audio | P5 |
| Real screens, replacing the placeholders | P6, P7 |
| Release signing, `v1.0.0` | P8 |

## 4. Design

### 4.1 Packages and files owned

The base package is `io.github.davamix.workouttimer` (see §4.5).

```
.editorconfig, .gitattributes
settings.gradle.kts, build.gradle.kts, gradle.properties
gradle/libs.versions.toml, gradle/wrapper/*, gradlew, gradlew.bat
.github/workflows/ci.yml
CLAUDE.md
.claude/skills/tablet/SKILL.md
core/build.gradle.kts                       :core, no sources yet
app/build.gradle.kts                        includes the no-INTERNET check task
app/src/main/AndroidManifest.xml
app/src/main/kotlin/io/github/davamix/workouttimer/
    WorkoutTimerApp.kt                      Application; creates the AppContainer
    MainActivity.kt
    di/AppContainer.kt
    navigation/Routes.kt, AppNavHost.kt
    ui/theme/Color.kt, Theme.kt, Type.kt
    ui/common/PlaceholderScreen.kt          removed once the last placeholder goes
    ui/list/WorkoutListScreen.kt            placeholder, replaced by P6
    ui/editor/WorkoutEditorScreen.kt        placeholder, replaced by P6
    ui/settings/SettingsScreen.kt           placeholder, replaced by P6
    ui/runner/RunnerScreen.kt               placeholder, replaced by P7
    ui/completed/CompletedScreen.kt         placeholder, replaced by P7
app/src/main/res/values/strings.xml         app name and shared text
app/src/main/res/values/strings_<feature>.xml   list, editor, settings, runner, completed
app/src/main/res/values/colors.xml, themes.xml
app/src/main/res/xml/data_extraction_rules.xml
app/src/main/res/drawable/ic_launcher_foreground.xml
app/src/main/res/mipmap-anydpi/ic_launcher.xml, ic_launcher_round.xml
app/src/debug/res/values/strings.xml        debug app name
app/src/androidTest/kotlin/io/github/davamix/workouttimer/NavigationSmokeTest.kt
```

**Shared files** that later phases change: the version catalog, the module build files, `AppContainer`, `AppNavHost`/`Routes`, and `CLAUDE.md`. Each later phase only adds its own entries to them.

### 4.2 Interfaces and data

- **`WorkoutTimerApp : Application`** exposes `container: AppContainer`, created in `onCreate`.
- **`AppContainer(context: Context)`** holds the app's long-lived objects. It is empty in P1; P3 and P5 add the repositories and the audio.
- **Routes** are type-safe Navigation Compose routes, made `@Serializable`:
  - `WorkoutListRoute`: the start destination.
  - `WorkoutEditorRoute(workoutId: String? = null)`: `null` means a new workout.
  - `SettingsRoute`
  - `RunnerRoute(workoutId: String)`
  - `CompletedRoute`
- **Theme:** `WorkoutTimerTheme { }` uses a Material 3 dark colour scheme. `PhaseColors` holds four background colours: get ready, exercise, rest and paused (for `UI-7` in P7).

### 4.3 Behaviour

The placeholder screens show their name, plus buttons for the navigation they will have later. This keeps the whole navigation graph testable now:

| Screen | Placeholder buttons |
|---|---|
| Workout list | "New workout" → editor; "Settings" → settings; "Run sample" → runner with id `sample` |
| Editor | Back |
| Settings | Back |
| Runner | "Finish" → completed |
| Completed | "Back to list" → list, clearing the back stack |

The system Back button works as normal everywhere.

### 4.4 Dependencies

| Item | Version | Notes |
|---|---|---|
| Gradle | 9.8.2 | `-bin` distribution, pinned with SHA-256 |
| Android Gradle Plugin | 9.3.3 | The newest AGP the installed Android Studio 2026.1.3 supports (plan `D-12`). Needs Gradle ≥ 9.5. |
| Kotlin | 2.4.21 | AGP's built-in Kotlin, so no `kotlin-android` plugin. Adds the Compose compiler and serialization plugins. |
| Compose BOM | 2026.09.00 | ui, material3, tooling, ui-test |
| activity-compose | 1.13.0 | |
| navigation-compose | 2.10.2 | type-safe routes |
| kotlinx-serialization | 1.12.0 | for the routes; P3 uses the JSON part |
| JUnit | 6.1.3 (BOM) | `:core` unit tests, from P2 on |
| AndroidX Test | ext-junit 1.3.0, runner 1.7.0 | instrumented tests |
| ktlint Gradle plugin | 14.2.0 | supports AGP 9 built-in Kotlin |

The catalog also declares the libraries that P3–P5 will need: lifecycle 2.11.0, coroutines 1.11.0, DataStore 1.3.0. Later phases then only apply them to modules. This keeps parallel phases from editing the same catalog lines.

SDK levels: `minSdk` 26; `compileSdk` and `targetSdk` 37. Android 16 and later ignore orientation locks on large screens for apps that target API 36 or higher. That doesn't affect the Android 8.0 tablet.

Bytecode targets Java 17. The build runs on Android Studio's bundled JDK (JBR 25) locally, and on Temurin 25 in CI.

### 4.5 Decisions made in this phase

| ID | Decision | Reason |
|---|---|---|
| P1-D1 | The application ID and namespace are `io.github.davamix.workouttimer`. | The usual convention for a project hosted on GitHub. It can't easily change once installed. |
| P1-D2 | Debug builds use the application ID suffix `.debug` and the name "Workout Timer (debug)". | Debug and release builds can then be installed side by side on the tablet without losing data (P8). |
| P1-D3 | The theme is dark only. | High contrast, viewed from a distance. Android 8.0 has no dynamic colour anyway. |
| P1-D4 | Lint stops the build on errors; warnings are reported. The orientation-lock warnings are suppressed on purpose (`NF-8`). | Strict, but no noise from "newer version available" warnings |
| P1-D5 | `.gitattributes` keeps LF line endings in the repo, except for `*.bat`. | Keeps Windows and CI consistent. Shell scripts break with CRLF. |

## 5. Security and privacy

- **Manifest:**
  - no `INTERNET` permission;
  - `android:allowBackup="false"`, plus data extraction rules that exclude everything from cloud backup and device transfer;
  - only the launcher activity is exported.
- **No-INTERNET check:** a Gradle task reads the merged manifest of every variant, and fails the `check` task if it finds `android.permission.INTERNET`.
- **CI:**
  - `permissions: contents: read`, and no secrets;
  - every action is pinned to a commit SHA;
  - `setup-gradle` validates the Gradle wrapper.
- **Gradle wrapper:** pinned with `distributionSha256Sum`.

## 6. Testing

| Test | Type | Covers |
|---|---|---|
| `NavigationSmokeTest`: the app starts on the workout list; each placeholder screen opens and Back returns | UI (instrumented, on the tablet) | Navigation skeleton, `NF-5` |
| `NavigationSmokeTest`: the activity requests `SCREEN_ORIENTATION_SENSOR_LANDSCAPE` | UI (instrumented) | `NF-8` |
| No-INTERNET check: passes as is; fails when the permission is added temporarily | Build check, tried once by hand | `NF-4` |
| CI run on the pull request | CI | Build, lint, ktlint |

### Manual checks on the tablet

1. `gradlew installDebug` → "Workout Timer (debug)" appears in the launcher, with its icon.
2. Open it → it shows in landscape. Turn the tablet upside down → the screen follows. Hold it upright → it stays landscape.
3. Move through all the placeholder screens using their buttons and the system Back button.

## 7. Tasks

| ID | Task |
|---|---|
| T1 | Gradle project: wrapper, `settings.gradle.kts`, root build, `gradle.properties`, version catalog, `.editorconfig`, `.gitattributes` |
| T2 | `:core` module: Kotlin JVM, Java 17 target, JUnit 6 setup |
| T3 | `:app` module: build file, manifest, application class, `AppContainer`, theme, launcher icon, strings |
| T4 | Navigation: routes, `AppNavHost`, placeholder screens |
| T5 | Checks: lint options, ktlint, no-INTERNET task wired into `check` |
| T6 | `NavigationSmokeTest`, run on the tablet |
| T7 | CI workflow |
| T8 | `CLAUDE.md` and the `tablet` skill |
| T9 | Manual checks on the tablet; pull request with CI green |

## 8. Done criteria

- [x] All tasks done
- [x] `gradlew build` passes locally and in CI
- [x] `gradlew connectedDebugAndroidTest` passes on the tablet
- [x] Manual checks on the tablet pass
- [x] HANDOVER.md is up to date, and the plan's status table is updated

## 9. Open questions

None.

## 10. Change log

| Version | Date | Changes |
|---|---|---|
| 0.1 | 2026-11-02 | First version |
