# Workout Timer: guide for Claude Code

An Android interval timer for round-based workouts, with spoken cues. It runs on one HUAWEI tablet (Android 8.0, API 26, landscape, 1920 × 1200). Built with Kotlin and Jetpack Compose.

## Documents

- `docs/SPECIFICATION.md`: what the app does. Requirement IDs look like `EX-4` or `NF-1`.
- `docs/IMPLEMENTATION_PLAN.md`: how it is built. Decisions `D-n`, phases P1–P8, and their status in §5.1.
- `docs/phases/PN-<name>/SPEC.md` and `HANDOVER.md`: one specification and one progress tracker per phase.

When working on a phase, read only its HANDOVER.md and SPEC.md (skill `phase-start`). They quote everything needed. Each HANDOVER.md starts with a prompt to paste into a new session to start or continue that phase. Keep it current.

**Precedence:** specification, then plan, then phase SPEC. Fix the higher document first, then let the change flow down.

## Skills

- `phase-docs`: write a phase's SPEC.md and HANDOVER.md.
- `phase-start`: begin or resume a phase (branch, dependencies, status).
- `handover`: end a session. Runs the checks, updates HANDOVER.md, commits and pushes, and opens the pull request when the phase is done.
- `phase-close`: after the user merges a phase's pull request, mark the phase `Done` on `main`.
- `tablet`: build, install, launch, take screenshots and read logs on the tablet.

## Commands

| Task | Command |
|---|---|
| Compile, unit tests, Android Lint, ktlint, no-INTERNET check | `./gradlew build` |
| Fix Kotlin formatting | `./gradlew ktlintFormat` |
| Unit tests only | `./gradlew test` |
| Install on the tablet | `./gradlew installDebug` |
| Instrumented tests on the tablet | `./gradlew connectedDebugAndroidTest` |

- Gradle runs on Android Studio's JDK; `JAVA_HOME` already points to it.
- `adb` is not on PATH. The `tablet` skill has the full path.
- Run `ktlintFormat` as its own command, before `build`. In a single call (`./gradlew ktlintFormat build`), the ktlint check can still report violations that the format step has just fixed, and the build fails.
- On JDK 25, ktlint prints harmless `sun.misc.Unsafe` warnings. To hide them and keep only real output (Git Bash):

  ```bash
  ./gradlew build --console=plain -q 2>&1 | grep -v -i 'unsafe\|^WARNING'
  ```

- `-q` hides the test counts. To total the unit test results per module and test task, after a build:

  ```bash
  for d in */build/test-results/*/; do grep -h -o 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' "$d"*.xml 2>/dev/null | awk -F'"' -v d="$d" '{t+=$2;s+=$4;f+=$6;e+=$8} END {if (NR) print d": tests="t" skipped="s" failures="f" errors="e}'; done
  ```

## Code structure

- `:core` is pure Kotlin: model, timeline, engine, cue planner. Never add Android dependencies to it (plan D-2).
- `:app` holds the Android code: `data/`, `audio/`, `ui/<feature>/`, `navigation/`, and `di/AppContainer.kt`.
- The base package is `io.github.davamix.workouttimer`.

## Conventions

- **Text:** all user-facing text lives in resources, one `res/values/strings_<feature>.xml` per feature (D-13). English only for now.
- **Permissions:** none. `INTERNET` is forbidden and fails the build (NF-4).
- **Versions:** pinned in `gradle/libs.versions.toml`. AGP must stay at 9.3 or lower while Android Studio 2026.1.3 is the installed IDE.
- **Kotlin style:** ktlint, official style (`.editorconfig`). Run `ktlintFormat` before committing.
- **Ownership:** each phase owns its packages. Shared files (the catalog, build files, `AppContainer`, `AppNavHost`) only get that phase's own entries.

## Git

- `main` always builds. Each phase is built on its own branch, `phase/pN-<name>`, and merged through a pull request with green CI. The user merges.
- Draft documents (specification, plan, a phase's SPEC.md) are committed and pushed to `main` at every version.
- Phase status changes in plan §5.1 also go straight to `main`.
- HANDOVER.md updates go on the phase branch. The one exception: after the merge, the closing updates go on `main` (skill `phase-close`).
- Create phase branches with `--no-track` and push them with `-u`, so they never track `main` (skill `phase-start`).
