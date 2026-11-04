# Workout Timer: guide for Claude Code

An Android interval timer for round-based workouts, with spoken cues. It runs on one HUAWEI tablet (Android 8.0, API 26, landscape, 1920 × 1200). Built with Kotlin and Jetpack Compose.

## Documents

- `docs/SPECIFICATION.md`: what the app does. Requirement IDs look like `EX-4` or `NF-1`.
- `docs/IMPLEMENTATION_PLAN.md`: how it is built. Decisions `D-n`, phases P1–P8, and their status in §5.1.
- `docs/phases/PN-<name>/SPEC.md` and `HANDOVER.md`: one specification and one progress tracker per phase.

When working on a phase, read only its HANDOVER.md and SPEC.md (skill `phase-start`). They quote everything needed. Each HANDOVER.md starts with a prompt to paste into a new session to start or continue that phase. Keep it current.

**Precedence:** specification, then plan, then phase SPEC. Fix the higher document first, then let the change flow down.

## Skills

- `phase-docs`: write a phase's SPEC.md and HANDOVER.md, and change a SPEC.md during implementation.
- `phase-start`: begin or resume a phase (branch, dependencies, status).
- `handover`: end a session. Runs the checks, updates HANDOVER.md, commits and pushes, and opens the pull request when the phase is done.
- `phase-close`: after the user merges a phase's pull request, mark the phase `Done` on `main`.
- `tablet`: build, install, launch, take screenshots and read logs on the tablet. It also covers reading the text on screen without a screenshot, running one instrumented test without wiping the app's data, summarising instrumented test results, measuring device behaviour with a throwaway probe, reading the app's files, and rebooting.

## Commands

| Task | Command |
|---|---|
| Compile, unit tests, Android Lint, ktlint, no-INTERNET check | `./gradlew build` |
| Fix Kotlin formatting | `./gradlew ktlintFormat` |
| Unit tests only | `./gradlew test` |
| Install on the tablet | `./gradlew installDebug` |
| Instrumented tests on the tablet | `./gradlew connectedDebugAndroidTest` (uninstalls the app afterwards) |

- Gradle runs on Android Studio's JDK; `JAVA_HOME` already points to it.
- `adb` is not on PATH. The `tablet` skill has the full path.
- Run `ktlintFormat` as its own command, before `build`. In a single call (`./gradlew ktlintFormat build`), the ktlint check can still report violations that the format step has just fixed, and the build fails.
- `ktlintFormat` can't fix every line that's too long. It can leave one half-wrapped, and `build` then fails on ktlint. Wrap such lines by hand, for example by moving part of an expression into a local `val`. The reports in `app/build/reports/ktlint/` (or `core/…`) name the file and line.
- On JDK 25, ktlint prints harmless `sun.misc.Unsafe` warnings. To hide them and keep only real output (Git Bash):

  ```bash
  ./gradlew build --console=plain -q 2>&1 | grep -v -i 'unsafe\|^WARNING'
  ```

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

## Conventions

- **Text:** all user-facing text lives in resources, one `res/values/strings_<feature>.xml` per feature (D-13). English only for now.
- **Permissions:** none. `INTERNET` is forbidden and fails the build (NF-4).
- **Versions:** pinned in `gradle/libs.versions.toml`. AGP must stay at 9.3 or lower while Android Studio 2026.1.3 is the installed IDE.
- **Kotlin style:** ktlint, official style (`.editorconfig`). Run `ktlintFormat` before committing.
- **Tests:**
  - **Unit tests:** in both modules, JUnit Jupiter, with sentence names in backticks. With AGP 9, `./gradlew build` runs only `:app`'s debug unit tests (`testDebugUnitTest`).
  - **Characters in test names:** names in backticks can't contain `.` `;` `:` `/` `\` `[` `]` `<` `>` on the JVM; the compiler rejects them. Use commas, or "and", instead.
  - **Coroutine tests:** in `runTest`, `advanceUntilIdle()` ignores work that only `backgroundScope` has scheduled. Code launched there, such as a `WorkoutSession`'s timer, doesn't run. Wait in the foreground instead: `session.state.first { … }` or `session.events.toList()`. `advanceTimeBy()` does run background work. For the clock, use `Clock { testScheduler.currentTime }`.
  - **Instrumented tests** (`app/src/androidTest`): JUnit 4 with camelCase names. Names with spaces don't compile to DEX below API 30, and `minSdk` is 26.
  - **Compose UI tests:** use the rules from `androidx.compose.ui.test.junit4.v2` (`createComposeRule`, `createAndroidComposeRule`). The v1 rules are deprecated in Compose BOM 2026.09.00. After an action that starts a coroutine, wait for its result (`waitUntil { … }` or `waitForIdle()`) before asserting.
  - **JUnit 4 asserts:** `assertNotNull` returns nothing, so its result can't be used. To check a value and then use it, write `val voice = checkNotNull(x) { "No voice selected" }`.
  - **Fixtures:** `core/src/test/.../core/ExampleWorkouts.kt` for `:core` and `app/src/test/.../data/SampleWorkouts.kt` for `:app`. Both hold the specification §4.4 example. `:app` IDs only use characters that are valid in file names.
- **Ownership:** each phase owns its packages. Shared files (the catalog, build files, `AppContainer`, `AppNavHost`) only get that phase's own entries.

## Git

- `main` always builds. Each phase is built on its own branch, `phase/pN-<name>`, and merged through a pull request with green CI. The user merges.
- Draft documents (specification, plan, a phase's SPEC.md) are committed and pushed to `main` at every version. That includes a SPEC version made during implementation. Afterwards, merge `main` into the phase branch (skill `handover`).
- Phase status changes in plan §5.1 also go straight to `main`.
- HANDOVER.md updates go on the phase branch. The one exception: after the merge, the closing updates go on `main` (skill `phase-close`).
- Create phase branches with `--no-track` and push them with `-u`, so they never track `main` (skill `phase-start`).
