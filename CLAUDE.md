# Workout Timer: guide for Claude Code

An Android interval timer for round-based workouts, with spoken cues. It runs on one HUAWEI tablet (Android 8.0, API 26, landscape, 1920 × 1200). Built with Kotlin and Jetpack Compose.

## Documents

- `docs/SPECIFICATION.md`: what the app does. Requirement IDs look like `EX-4` or `NF-1`.
- `docs/IMPLEMENTATION_PLAN.md`: how it is built. Decisions `D-n`, phases P1–P8, and their status in §5.1.
- `docs/phases/PN-<name>/SPEC.md` and `HANDOVER.md`: one specification and one progress tracker per phase.
- `docs/phases/P8-integration/ACCEPTANCE.md`: how each requirement of version 1.0.0 was checked on the tablet.

The specification and the plan are at v1.0, which matches the app's version 1.0.0. New work starts a new version of each.

When working on a phase, read only its HANDOVER.md and SPEC.md (skill `phase-start`). They quote everything needed. Each HANDOVER.md starts with a prompt to paste into a new session to start or continue that phase. Keep it current.

**Precedence:** specification, then plan, then phase SPEC. Fix the higher document first, then let the change flow down.

## Skills

- `phase-docs`: write a phase's SPEC.md and HANDOVER.md, and change a SPEC.md during implementation.
- `phase-start`: begin or resume a phase (branch, dependencies, status).
- `handover`: end a session. Runs the checks, updates HANDOVER.md, commits and pushes, and opens the pull request when the phase is done.
- `phase-close`: after the user merges a phase's pull request, mark the phase `Done` on `main`.
- `tablet`: build, install, launch, take screenshots and read logs on the tablet. It also covers reading the text on screen without a screenshot, running one instrumented test without wiping the app's data, summarising instrumented test results, timing a workout from outside the app, driving the editor through adb, measuring device behaviour with a throwaway probe, reading the app's files, and rebooting.

## Commands

| Task | Command |
|---|---|
| Compile, unit tests, Android Lint, ktlint, no-INTERNET check | `./gradlew build` |
| Fix Kotlin formatting | `./gradlew ktlintFormat` |
| Unit tests only | `./gradlew test` |
| Install on the tablet | `./gradlew installDebug` |
| Instrumented tests on the tablet | `./gradlew connectedDebugAndroidTest` (uninstalls the app afterwards, which deletes its workouts). To keep them, the `tablet` skill's `instrument` helper runs the whole suite on the installed app. |
| Signed release APK | `./gradlew assembleRelease`, which writes `app/build/outputs/apk/release/app-release.apk` |
| Check a release APK's signature | `D:\Android\Sdk\build-tools\37.0.0\apksigner.bat verify --print-certs <apk>`. It must show a v2 signature and `CN=Daniel Valcarce, C=ES`, with the SHA-256 in P8 SPEC §4.3.2. |
| Install a release on the tablet | `adb install -r <apk>`. It installs next to the debug build, and each keeps its own workouts. |

- **Signing:** the keystore and the file that holds its passwords, the user-level `gradle.properties`, stay outside the repo. Never read or print that file, and never put a password on a command line. Without its four properties, as on CI, the release APK is unsigned and `build` still passes.
- Gradle runs on Android Studio's JDK; `JAVA_HOME` already points to it.
- `adb` is not on PATH. The `tablet` skill has the full path.
- Run `ktlintFormat` as its own command, before `build`. In a single call (`./gradlew ktlintFormat build`), the ktlint check can still report violations that the format step has just fixed, and the build fails.
- `ktlintFormat` can't fix every line that's too long. It can leave one half-wrapped, and `build` then fails on ktlint. Wrap such lines by hand, for example by moving part of an expression into a local `val`. The reports in `app/build/reports/ktlint/` (or `core/…`) name the file and line.
- On JDK 25, ktlint prints harmless `sun.misc.Unsafe` warnings. To hide them and keep only real output, write the output to a file and filter that (Git Bash):

  ```bash
  mkdir -p build; ./gradlew build --console=plain -q > build/gradle.log 2>&1; echo "exit=$?"; grep -v -i 'unsafe\|^WARNING' build/gradle.log | tail -30
  ```

- **Keep Gradle's exit code.** Don't pipe Gradle into `grep` or `tail` and chain the next step with `&&`: the pipe returns the last command's code, so the next step runs after a failed build. In P7, `am instrument` then ran an old test APK. `set -o pipefail` doesn't help, because `grep -v` returns 1 when it has nothing left to print. Use the file, as above, or the `tablet` skill's `gradle` helper, which does the same.
- `-q` hides the test counts. To total the unit test results per module and test task, after a build:

  ```bash
  for d in */build/test-results/*/; do grep -h -o 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' "$d"*.xml 2>/dev/null | awk -F'"' -v d="$d" '{t+=$2;s+=$4;f+=$6;e+=$8} END {if (NR) print d": tests="t" skipped="s" failures="f" errors="e}'; done
  ```

- When unit tests fail, this prints each failing test's class, name and message (first 300 characters), from the JUnit XML:

  ```bash
  grep -h -o '<testcase name="[^"]*" classname="[^"]*"\|<failure message="[^"]\{0,300\}' */build/test-results/*/*.xml | grep -B1 '<failure' | grep -v '^--$' | sed -e 's/^<testcase name="\([^"]*\)" classname="\([^"]*\)"/\2 > \1/' -e 's/^<failure message="/    /' -e 's/&quot;/"/g; s/&lt;/</g; s/&gt;/>/g; s/&amp;/\&/g'
  ```

- Python isn't installed on this machine. Use `grep`, `sed` and `awk` in Git Bash, or PowerShell.
- Create and change source files with the Write and Edit tools, not shell heredocs. In P5, a heredoc that wrote a Kotlin file failed to parse in Git Bash, and nothing was written.

## Code structure

- `:core` is pure Kotlin: model, timeline, engine, cue planner. Never add Android dependencies to it (plan D-2).
- `:app` holds the Android code: `data/`, `audio/`, `ui/<feature>/`, `navigation/`, and `di/AppContainer.kt`.
- The base package is `io.github.davamix.workouttimer`.
- **Screens** (since P6): each has an entry composable that takes its ViewModel, defaulting to `viewModel(factory = …)`, and a stateless `…Content` composable that draws the state. The factory, in the ViewModel's companion, gets the container with `(this[APPLICATION_KEY] as WorkoutTimerApp).container`. Tests build the ViewModel directly, with fakes.
- **Navigation:**
  - **A click that navigates** goes through `dropUnlessResumed`, or `NavBackStackEntry.ifResumed` in `AppNavHost` when it takes an argument, so a double tap can't open a screen twice.
  - **Navigation driven by a ViewModel state** works differently, for an outcome such as the runner's `Completed` or `Exited`. Report it once from a `LaunchedEffect` on that state, after waiting with `lifecycle.currentStateFlow.first { it.isAtLeast(RESUMED) }`, and pass a plain lambda.
  - **Why the wait:** a screen Android rebuilds after closing the app is composed before its entry is resumed. `dropUnlessResumed` would silently drop its call (P7 SPEC §4.3.7).

## Conventions

- **Text:** all user-facing text lives in resources, one `res/values/strings_<feature>.xml` per feature (D-13). English only for now.
- **Permissions:** the app requests none. The merged manifest's only one is AndroidX's `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, which the app declares and grants itself (plan §3). `INTERNET` is forbidden and fails the build (NF-4).
- **Versions:** pinned in `gradle/libs.versions.toml`. AGP must stay at 9.3 or lower while Android Studio 2026.1.3 is the installed IDE.
- **Kotlin style:** ktlint, official style (`.editorconfig`). Run `ktlintFormat` before committing.
- **Formatting numbers:** don't call `String.format` without a `Locale`. Lint warns (`DefaultLocale`), and the result would follow the device's language. Pad with `padStart` instead, as `formatTotalDuration()` does.
- **Tests:**
  - **Unit tests:** in both modules, JUnit Jupiter, with sentence names in backticks. With AGP 9, `./gradlew build` runs only `:app`'s debug unit tests (`testDebugUnitTest`).
  - **Characters in test names:** names in backticks can't contain `.` `;` `:` `/` `\` `[` `]` `<` `>` on the JVM; the compiler rejects them. Use commas, or "and", instead.
  - **Coroutine tests:** in `runTest`, `advanceUntilIdle()` ignores work that only `backgroundScope` has scheduled. Code launched there, such as a `WorkoutSession`'s timer, doesn't run. Wait in the foreground instead: `session.state.first { … }` or `session.events.toList()`. `advanceTimeBy()` does run background work. For the clock, use `Clock { testScheduler.currentTime }`.
  - **Instrumented tests** (`app/src/androidTest`): JUnit 4 with camelCase names. Names with spaces don't compile to DEX below API 30, and `minSdk` is 26.
  - **ViewModel tests** (`:app`): register `MainDispatcherExtension`, so `viewModelScope` runs on a test dispatcher. Compose snapshot state (`mutableStateOf`) works in these JVM tests.
  - **Compose UI tests:** use the rules from `androidx.compose.ui.test.junit4.v2` (`createComposeRule`, `createAndroidComposeRule`). The v1 rules are deprecated in Compose BOM 2026.09.00. After an action that starts a coroutine, wait for its result (`waitUntil { … }` or `waitForIdle()`) before asserting. Also:
    - A `LazyColumn` item exists only once it's scrolled into composition. Tag the list, and call `performScrollToNode(matcher)` on it before using an item.
    - With the soft keyboard up, Back only closes the keyboard. Call `closeSoftKeyboard()` before `pressBack()`.
    - A click on a node with empty bounds raises no error, and lands at the screen's corner. Under `MainActivity`, the window resizes when the keyboard changes, even between `performScrollToNode` and `performClick`, so call `closeSoftKeyboard()` before a click that follows a scroll. Focus moving from a name to a number swaps the keyboard a moment later (P8's `EndToEndTest`).
    - `and` and `or` on matchers are members and need no import. `assert(…)` on a node is an extension, imported from `androidx.compose.ui.test.assert`.
    - Check touch targets (`NF-6`) with `assertTouchTargetsAtLeast()`. Material 3 icon buttons lay out at 40 dp but take touches over 48 dp, so it checks the touch bounds of the nodes fully on screen.
    - `captureToImage()` reads the colours and look of a screen in a test, for example a background colour from one pixel. It can't capture a dialog below API 28: it throws.
  - **JUnit 4 asserts:** `assertNotNull` returns nothing, so its result can't be used. To check a value and then use it, write `val voice = checkNotNull(x) { "No voice selected" }`.
  - **Fixtures:** `core/src/test/.../core/ExampleWorkouts.kt` for `:core` and `app/src/test/.../data/SampleWorkouts.kt` for `:app`. Both hold the specification §4.4 example. `:app` IDs only use characters that are valid in file names.
  - **Test helpers** (since P6):
    - `app/src/test/.../ui/`: `MainDispatcherExtension`, and `TestRepositories.kt`, whose in-memory repositories can hold back their data or fail writes with `IOException`.
    - `app/src/androidTest/.../ui/`: `UiTestWorkouts.kt` (the §4.4 example, and `withoutIds()` to compare structures), and `TouchTargets.kt`.
- **Ownership:** each phase owns its packages. Shared files (the catalog, build files, `AppContainer`, `AppNavHost`) only get that phase's own entries.

## Git

- `main` always builds. Each phase is built on its own branch, `phase/pN-<name>`, and merged through a pull request with green CI. The user merges.
- Draft documents (specification, plan, a phase's SPEC.md) are committed and pushed to `main` at every version. That includes a SPEC version made during implementation. Afterwards, merge `main` into the phase branch (skill `handover`).
- Phase status changes in plan §5.1 also go straight to `main`.
- HANDOVER.md updates go on the phase branch. The one exception: after the merge, the closing updates go on `main` (skill `phase-close`).
- Create phase branches with `--no-track` and push them with `-u`, so they never track `main` (skill `phase-start`).
- A release is a `vX.Y.Z` tag on `main` and a GitHub release with the signed APK (P8 SPEC §4.3.6). The repo is public, so tag and release only with the user's go-ahead.
